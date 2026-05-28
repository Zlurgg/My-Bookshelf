# Update use case must not upsert — surviving preview-cache leak

**Status:** Landed 2026-05-28 on branch `follow-up-fixes` as a single commit per the v3 plan. All 692 unit tests + detekt green. Manual device re-verification on Pixel_8 (step 8) is still owed by the user. Screen-gate matrix (5 rows, §5) ships in `androidTest/` rather than the Robolectric unit-test suite — Robolectric + Compose's `createComposeRule()` cannot resolve the `ComponentActivity` host without manifest plumbing this commit chose not to introduce; the existing `androidTest` source set was the right home but had pre-existing compile breakage from earlier refactors (`ValidatorIntegrationTest`, `BookRepositoryIntegrationTest`) — the new gate test is independently correct and will run once those neighbors are fixed.

v1 proposed `getPersistedBookById` + retained read-modify-write upsert. v2 introduced column-scoped DAO updates + a screen gate. v3 corrected: DAO param type (String, not enum — Room can't convert), screen gate (`isInLibrary` only, not `isInLibrary || hasShelfContext` — the latter still rendered cards on shelf-search previews), multi-field write atomicity via a `@Transaction` DAO orchestrator, named `dateAdded` insert sites, pure-delegation use cases (no double-wrap), and several smaller framings.
**Origin:** Manual device verification of [preview-cache-library-leak.md](preview-cache-library-leak.md) on Pixel_8 (debug) revealed the preview leak still occurs on Bookshelf and Library search → tap → back. The preview-cache commit (`52fea692`) landed correctly; this plan addresses the second leak path that the cache work unintentionally re-armed, plus a set of latent problems the review surfaced.
**Scope:** Make `UpdateBookMetadataUseCase` and `ToggleBookPurchaseUseCase` write to a single column instead of doing read-modify-upsert of the whole row; remove the cache fallback from `BookRepository.getBookById` and route the one legitimate cache consumer through a new `peekPreview`; gate the personal-metadata cards on the detail screen behind library/shelf membership so they aren't shown for previewed books at all.

## The bug

After landing the preview-cache fix (commit `52fea692`), the reproduction from the original plan still leaks — via a different code path:

1. Open Library or any Bookshelf.
2. Open the search dialog, search for a book you don't already have.
3. Tap the row (not the `+` button) — detail screen renders from the cache.
4. **Press Back.**
5. Open the Library — the previewed book is now there.

The leak is no longer in `OnSearchResultBookClick` (that path is clean). It is in `BookDetailAction.OnBackClick` (`BookDetailViewModel.kt:256-271`):

```kotlin
BookDetailAction.OnBackClick -> {
    viewModelScope.launch {
        saveNotesJob?.cancel()
        bookDetailUseCases.updateBookMetadata(
            bookId = bookId,
            personalNotes = state.value.book?.personalNotes,
        )
        onNavigateBack?.invoke()
    }
}
```

That handler exists to flush any debounced notes save that hasn't fired yet — without it, typing then immediately pressing back would lose the keystrokes.

`UpdateBookMetadataUseCaseImpl.kt:43-62` then does a classic **get-then-upsert** of the entire row:

```kotlin
val existingBook = when (val getResult = bookRepository.getBookById(bookId)) {
    is Result.Success -> getResult.data ?: return Result.Error(DataError.Local.NOT_FOUND)
    is Result.Error -> return getResult
}
…
val updatedBook = existingBook.copy(…)
return bookRepository.upsertBook(updatedBook)
```

Before the preview cache, `getBookById` returned null for previewed books → use case early-exited with `NOT_FOUND` → no upsert. `update` *happened* to behave like "update-existing-only" by accident. The preview cache changed `getBookById` from *"is this book persisted?"* to *"is this book either persisted OR previewable?"*. The use case still treats a non-null result as "the book exists in storage, safe to upsert." The cache hands it a previewed book → use case upserts the whole row → preview is promoted into the library.

## Why this is worse than just a leak — three latent problems the v1 plan missed

The v1 plan treated this as a single leak with a single fix (a DAO-only read variant). Staff review surfaced that the `get → copy → upsertBook` shape is itself the smell, and that the codebase has **already decided this is wrong** for one column. See `BookDao.kt:21-31`:

> Targeted update for the description column only. Intentionally a column-scoped UPDATE rather than a full-row upsert: callers write the description in parallel with debounced personal-metadata writes (notes/rating/status). A full-row upsert here would clobber any in-flight personal-metadata write from the user.

The same docstring rationale applies to reading status, personal rating, personal notes, and purchased. Today's read-modify-upsert path has three latent failure modes, *each of which the existing description-column pattern was designed to prevent*:

1. **Clobbering of concurrent writes.** If two paths edit different fields in flight (e.g. user edits notes in one part of the screen while a description fetch resolves in another), the second writer's full-row upsert overwrites the first. Today this can't happen because the description path uses targeted updates — but the metadata path is the bug the docstring foretold.
2. **TOCTOU window between get and upsert.** Sync-down, cleanup, or any other writer can delete or modify the row between the `getBookById` and the `upsertBook`. The use case then resurrects deleted data or overwrites newer data with stale-read fields.
3. **Per-keystroke full-row I/O.** With the notes debounce removed (see §View Model), per-keystroke writes would read the whole row, copy it in memory, and upsert the whole row — strictly worse than the debounced status quo. With column updates, per-keystroke is one targeted UPDATE on one column.

The leak is the user-visible symptom; the read-modify-upsert shape is the root cause. Fixing only the leak (via a guard like `getPersistedBookById`) leaves the other two and locks in the per-keystroke regression.

## Naming vs. behaviour — the architectural smell

| Use case | Name implies | Actually does | What it should do |
|---|---|---|---|
| `UpsertBookUseCase` | create-or-update | upsert | upsert ✅ |
| `AddBookToShelfUseCase` | add to shelf | upsert + join | upsert + join ✅ |
| `updateDescription` (repository) | update one column | targeted DAO UPDATE | targeted DAO UPDATE ✅ |
| **`UpdateBookMetadataUseCase`** | **update existing** | **get → copy → upsert (full row)** | **targeted DAO UPDATE per column** |
| **`ToggleBookPurchaseUseCase`** | **toggle one boolean** | **get → copy (preserve 5 fields) → upsert (full row)** | **targeted DAO UPDATE on purchased** |

The fix is to extend the `updateDescription` pattern to the other personal-metadata columns. The shape is already justified in the codebase; we're not introducing a new convention, we're applying an existing one consistently.

## The fix — domain + DAO layer, no schema change

### 1. DAO: column-scoped UPDATE queries

Mirror `updateDescription` for each personal-metadata column. Same docstring rationale (parallel-write safety, no clobbering) is the controlling reason — quoted, not paraphrased.

```kotlin
@Query("UPDATE BookEntity SET readingStatus = :status WHERE id = :id")
suspend fun updateReadingStatus(id: String, status: String)

@Query("UPDATE BookEntity SET personalRating = :rating WHERE id = :id")
suspend fun updatePersonalRating(id: String, rating: Float)

@Query("UPDATE BookEntity SET personalNotes = :notes WHERE id = :id")
suspend fun updatePersonalNotes(id: String, notes: String)

@Query("UPDATE BookEntity SET purchased = :purchased WHERE id = :id")
suspend fun updatePurchased(id: String, purchased: Boolean)
```

`readingStatus` is stored as `String` in `BookEntity.kt:24` (default `"NOT_READ"`) and there is no `ReadingStatus` `TypeConverter` registered on the database — the param must be `String` for Room to compile the query. The domain → entity conversion already lives in `BookMappers`; the use case calls `.name` at the boundary (see §3). Personal notes are non-nullable in the entity (`BookEntity.kt:26`, default `""`), so the DAO param matches.

UPDATE on a row that doesn't exist is a silent no-op in SQLite — same shape that already keeps `updateDescription` safe for the preview path. The leak is fixed at the storage layer; no use-case-level guard needed.

A multi-column write should preserve the atomicity guarantee the current single-statement upsert provides. Today all callers pass exactly one field, but the contract should not silently weaken. Add a `@Transaction` DAO orchestrator that does the nullable-aware conditionals in one place:

```kotlin
@Transaction
suspend fun updatePersonalMetadata(
    id: String,
    readingStatus: String? = null,
    personalRating: Float? = null,
    personalNotes: String? = null,
) {
    readingStatus?.let { updateReadingStatus(id, it) }
    personalRating?.let { updatePersonalRating(id, it) }
    personalNotes?.let { updatePersonalNotes(id, it) }
}
```

This keeps the "all fields or none" guarantee at the storage layer. `database.withTransaction { ... }` from the repository would be an alternative but would require adding a `RoomDatabase` reference to `BookRepositoryImpl`'s constructor — strictly more change for no behavioural difference. The `@Transaction` DAO method is the smaller, in-place edit. `updatePurchased` stays standalone (single-field write, used only by `ToggleBookPurchaseUseCase`).

### 2. Repository: drop the cache from `getBookById`; add `peekPreview`

The v1 plan added `getPersistedBookById` and left `getBookById` cache-aware. The review pointed out the resulting footgun — two near-identical methods with subtly different semantics; every future caller of `getBookById` is a potential re-leak.

Better shape: **one read semantic, one cache consumer.**

```kotlin
// BookRepository.kt
suspend fun getBookById(bookId: String): Result<Book?, DataError.Local>   // DAO only
fun peekPreview(bookId: String): Book?                                    // cache only
fun cacheSearchPreviews(books: List<Book>)                                // unchanged

suspend fun updatePersonalMetadata(
    bookId: String,
    readingStatus: String? = null,
    personalRating: Float? = null,
    personalNotes: String? = null,
): Result<Unit, DataError.Local>
suspend fun updatePurchased(bookId: String, purchased: Boolean): Result<Unit, DataError.Local>
```

```kotlin
// BookRepositoryImpl.kt
override suspend fun getBookById(bookId: String): Result<Book?, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        dao.getBookById(bookId)?.toBook()
    }
}

override fun peekPreview(bookId: String): Book? = previewCache[bookId]
```

`peekPreview` intentionally deviates from the repository's all-`Result` convention: it's an in-memory lookup that cannot fail, so a `Result<Book?, DataError.Local>` wrapper would add cost without information. The deviation should be called out in the interface docstring so a reviewer doesn't flag it as inconsistent.

`getBookById` recovers its original meaning: *"is this book persisted?"* The cache has exactly one legitimate consumer (the detail-screen render path), and that consumer composes explicitly.

### 3. Use cases: collapse to one DAO call each

**`GetBookDetailsUseCaseImpl`** — the one cache consumer. After the DAO read returns null, fall back to `peekPreview`:

```kotlin
val book = when (val result = bookRepository.getBookById(bookId)) {
    is Result.Success -> result.data ?: bookRepository.peekPreview(bookId)
    is Result.Error -> return flowOf(emptyDetails)
}
```

(Exact spelling depends on the existing flow shape — the point is: explicit compose, cache fallback visible at the call site.)

**`UpdateBookMetadataUseCaseImpl`** — collapses to validation + one call to the `@Transaction` orchestrator. Validation (rating range, notes length) stays. The whole get-then-upsert disappears. The enum-to-string conversion for `readingStatus` lives at this boundary (mirroring how `BookMappers` already handles the entity ↔ domain edge). The use case does **not** re-wrap in `ErrorMapper.safeSuspendCall` — the repository already does. Pure delegation, same pattern as `UpdateBookDescriptionUseCaseImpl`:

```kotlin
override suspend operator fun invoke(
    bookId: String,
    readingStatus: ReadingStatus? = null,
    personalRating: Float? = null,
    personalNotes: String? = null,
): Result<Unit, DataError> {
    if (personalRating != null && personalRating !in 0f..MAX_RATING) {
        return Result.Error(DataError.Validation.INVALID_FORMAT)
    }
    if (personalNotes != null && personalNotes.length > MAX_NOTES_LENGTH) {
        return Result.Error(DataError.Validation.TOO_LONG)
    }
    return bookRepository.updatePersonalMetadata(
        bookId = bookId,
        readingStatus = readingStatus?.name,
        personalRating = personalRating,
        personalNotes = personalNotes,
    )
}
```

`Result<Unit, DataError.Local>` returned by the repository widens to the use case's `Result<Unit, DataError>` return type because `Result` is covariant in `E` (`Result.kt:6`).

The repository methods `updatePersonalMetadata` and `updatePurchased` are thin pass-throughs to the DAO — `updatePersonalMetadata` delegates to the `@Transaction` orchestrator from §1, `updatePurchased` to the single column query. Both wrap their DAO call in `ErrorMapper.safeSuspendCall(TAG)`, consistent with `updateDescription`'s shape (`BookRepositoryImpl.kt:57-64`).

`purchaseDate` is removed from the use case's parameter list. No call site in `BookDetailViewModel` ever passed it — the `OnPurchaseClick` path goes through `ToggleBookPurchaseUseCase` directly. The param was vestigial.

**`ToggleBookPurchaseUseCaseImpl`** — collapses to one repository call, no preservation gymnastics, no double-wrap (the repository method already returns a `Result`):

```kotlin
override suspend operator fun invoke(book: Book, purchased: Boolean): Result<Book, DataError.Local> {
    return bookRepository.updatePurchased(book.id, purchased)
        .map { book.copy(purchased = purchased) }
}
```

The ~30 lines of "if existingBook != null, preserve readingStatus / personalRating / personalNotes / dateAdded / purchaseDate, else use as-is" branch (`ToggleBookPurchaseUseCaseImpl.kt:14-33`) goes away entirely — it was only there because the upsert overwrote the row, and the upsert is gone.

### 4. View model: collapse `OnBackClick`, drop notes debounce

With column-scoped writes, per-keystroke is one targeted UPDATE. Cheap. The debounce + back-flush pair was only there because each save was a full-row read-modify-write that you wanted to batch.

```kotlin
is BookDetailAction.OnPersonalNotesChange -> {
    _state.update { it.updateBook { book -> book?.copy(personalNotes = action.notes) } }
    viewModelScope.launch {
        when (val r = bookDetailUseCases.updateBookMetadata(bookId, personalNotes = action.notes)) {
            is Result.Success -> Unit
            is Result.Error -> _state.update { it.withError(r.error, "update personal notes") }
        }
    }
}

BookDetailAction.OnBackClick -> onNavigateBack?.invoke()
```

Drop `saveNotesJob: Job?` and `DebounceDelayMs` from the file. As a free side-effect, back-press is no longer waiting on a suspending save — the latency that motivated the back-flush is gone.

The `saveReviewJob` debounce for club reviews **stays** — Firestore traffic per keystroke has different economics and the review path doesn't have a leak. Out of scope.

### 5. Screen: hide personal cards on previewed books

The v1 plan claimed leaving the cards visible would be harmless under the fix. Wrong: the handlers (`OnReadingStatusChange`, `OnPersonalRatingChange`, `OnPurchaseClick`) currently route into `withError` on failure, so tapping the cards on a previewed book would surface error toasts. Under the column-update fix the writes are silent no-ops, which is *also* wrong — the user taps a star rating and nothing happens.

The v2 plan proposed gating on `state.isInLibrary || state.hasShelfContext`. That's still wrong: `hasShelfContext` is true whenever the user reached the detail screen with a `shelfId`, including the **shelf-search preview** case (Bookshelf search → tap row → detail screen, before the user has added the book to anything). Matrix:

| `isInLibrary` | `hasShelfContext` | `onShelf` | Scenario | Cards? | v2 gate | v3 gate |
|---|---|---|---|---|---|---|
| true | false | n/a | Library, owned book | yes | ✓ | ✓ |
| true | true | true | Shelf, owned book on shelf | yes | ✓ | ✓ |
| true | true | false | Shelf search, owned book not yet on this shelf | yes | ✓ | ✓ |
| false | true | false | **Shelf search preview** | no | ✗ (bug) | ✓ |
| false | false | n/a | Library search preview | no | ✓ | ✓ |

The correct gate is just `state.isInLibrary`. For the `else` branch in `BookDetailScreen.kt:196` (non-tutorial, non-club), `isInLibrary` is true iff the book is in `BookEntity` and surfaced by `getAllPersonalBooks()` — exactly the condition under which the personal cards are safe to render and edits land somewhere.

```kotlin
// BookDetailScreen.kt — currently lines 196-225 unconditionally render these
//   ReadingStatusCard, PersonalNotesCard, PurchasedToggleCard
// for any non-tutorial, non-club book. Gate them:

if (state.isInLibrary) {
    item { ReadingStatusCard(…) }
    item { PersonalNotesCard(…) }
    item { PurchasedToggleCard(…) }
}
```

Previewed books then show: hero, description, publication details, languages, and (in the bottom bar) the `LibraryActionsCard` from commit `3c0e5a0c` (which is gated by `!hasShelfContext && !isInLibrary`) or `ShelfActionsCard` (gated by `hasShelfContext`). No editable personal state on a book the user hasn't added.

Note: the "symmetric with `LibraryActionsCard`" framing in v2 was a misread. `LibraryActionsCard` answers "library search preview, offer the add button"; the inverse of *that* is not "personal cards safe." The personal-cards gate is its own condition.

### 6. `dateAdded` — move the backfill to insert time

`UpdateBookMetadataUseCaseImpl.kt:58` currently auto-sets `dateAdded = existingBook.dateAdded ?: timeProvider.currentTimeMillis()` on every metadata update. This is a backfill for books that ended up in the DB without a `dateAdded` — `UpsertBookUseCaseImpl.kt:29-32` upserts a new book "as-is" without defaulting it, and the club-sync paths do the same.

Concrete insert sites that today rely on the update-time backfill:

- `UpsertBookUseCaseImpl.kt:29-32` — new-book branch.
- `BookClubRepositoryHelper.kt:112` — `book.toBookEntity()` → `bookshelfDao.upsert(bookEntity)`.
- `BookClubSyncRepositoryImpl.kt:139` — same shape (`book.toBookEntity()` → `bookshelfDao.upsert(bookEntity)`).

Under columnar updates this auto-backfill is lost (the update path no longer copies and re-inserts the row). The clean fix is to set `dateAdded` once at insert and never touch it again.

Two options for where to default:

1. **At each use-case / sync boundary** — mirror `AddBookToShelfUseCaseImpl.kt:68` (`dateAdded = book.dateAdded ?: timeProvider.currentTimeMillis()`) in the three sites above. Smallest change; each site already calls a use case or repository with a `TimeProvider` in reach.
2. **At the mapper boundary** — default inside `BookMappers.toBookEntity()`. Single point of truth, no caller can forget. Costs an injected `TimeProvider` parameter on the mapper (currently a pure top-level function), which is a wider footprint.

Recommendation: **option 1**. The mapper-level default is structurally tidier but turns a pure mapping function into one requiring DI, which propagates friction to every caller. With only three sites and a `TimeProvider` already in scope at each, the per-site default is honest and minimal. Pre-release latitude means we can revisit if a fourth insert site shows up.

A previously-considered fallback — a one-shot `setDateAddedIfNull(id, ts)` DAO query called from each `updateBookMetadata` field path — is rejected: it would be a get-then-write race against sync (a club-sync insert and a user edit could interleave such that two writers both think the field is null). Right call to leave it out.

### 7. Tutorial path

`GetOrCreateTutorialBookUseCaseImpl.kt:26` uses `getBookById`. The use case expects `null` on first run — that's how it knows to create the tutorial book. Under the new contract (`getBookById` is DAO-only), this behaviour is preserved: nothing seeds the tutorial book id into the preview cache, so de-merging the cache from `getBookById` is observationally a no-op for this caller. The call site doesn't change.

## What this fixes beyond the leak

| Problem | Before | After |
|---|---|---|
| Previewed book promoted to library on back-press | Leak | UPDATE on missing row = no-op |
| Concurrent-write clobbering across columns | Possible (the bug the description docstring foretold) | Impossible — each writer touches only its column |
| TOCTOU between read and write | Possible (sync-down, cleanup races) | Eliminated — no read |
| Per-keystroke I/O cost | Forced debounce or full-row upsert per char | One targeted UPDATE per char, cheap |
| Back-press latency | Waits on suspending full-row upsert | Pure nav, instant |
| Two read methods with subtle semantics | Risk (v1's `getPersistedBookById`) | One method, one meaning |
| Editable cards on previewed books | Render + error toasts (or silent no-ops) | Cards not rendered |
| `dateAdded` backfill on every edit | Fragile | Set once at insert |

## Tests

The review pointed out that the headline regression test belongs at the layer where a future bypass would be caught — the DAO/repo layer, not the ViewModel. A use-case test wouldn't catch a future caller that goes around the use case.

| Layer | Test | What it locks |
|---|---|---|
| DAO | `updateReadingStatus on absent row is a no-op` (Room integration test, real DB) | The leak-fix invariant at the storage layer. Same shape repeated for `updatePersonalRating`, `updatePersonalNotes`, `updatePurchased`. |
| DAO | Existing `updateDescription` test as reference | Confirms we're following the established pattern. |
| Repository | `getBookById returns null for cached-only book` | Locks the new (de-merged) contract — cache is no longer in `getBookById`. |
| Repository | `peekPreview returns cache hit; null when absent` | Locks the cache API. |
| Use case | `updateBookMetadata with valid input calls correct column update` | Locks the column-targeted routing per field. |
| Use case | `updateBookMetadata rejects rating > 5.0` | Validation preserved. |
| ViewModel | `OnBackClick does not call updateBookMetadata` | Locks the simplified back handler. |
| ViewModel | `OnPersonalNotesChange calls updateBookMetadata immediately, no debounce` | Locks the per-change save. |
| Screen | `personal cards emitted iff isInLibrary` — one Compose UI test per row of the §5 matrix (five rows, exhaustive over `(isInLibrary, hasShelfContext, onShelf)` for the non-club non-tutorial branch) | Locks the gate. Per-row assertions surface the shelf-search-preview case (`!isInLibrary && hasShelfContext`) as its own failure, so a future loosening regresses to the exact v2 hole. |

The previously-proposed "integration test using MockBookRepository" is dropped — the review correctly noted that "real `MockBookRepository`" is contradictory. Integration tests use real Room per CLAUDE.md; mocks are for unit tests. The DAO-level no-op test replaces it.

Existing tests that exercise the old read-modify-write path will need updating to assert the new column-write behaviour. Specifically:

- `UpdateBookMetadataUseCaseTest` (if it exists; create if not), `ToggleBookPurchaseUseCaseTest`.
- `BookRepositoryImplTest` — existing cases that assert `getBookById` returns a cached book invert under the de-merge; they should move to assertions against `peekPreview`, with new cases locking that `getBookById` returns null for cache-only ids.
- `SearchBooksUseCaseTest` — `cacheSearchPreviews` is still populated on a successful search; existing cases that subsequently read back via `getBookById` should switch to `peekPreview` to match the new API.

## Out of scope

- **Club-review debounce.** Same complexity-pile as notes, but Firestore-bound — different cost model. Leave it.
- **Wider audit for other "get-then-upsert" use cases.** Four were audited (`UpdateBookMetadata`, `ToggleBookPurchase`, `UpsertBook`, `AddBookToShelf`). The first two are addressed here; the latter two are intentional upserts that compose `getBookById ?: peekPreview` explicitly at the call site and keep the full-row write (that *is* what upsert means).
- **Renaming use cases to reflect their new behaviour.** `UpdateBookMetadataUseCase` is now genuinely an update. `ToggleBookPurchaseUseCase` is now genuinely a toggle. Names match behaviour — no rename needed.
- **Shrinking `ToggleBookPurchaseUseCase`'s signature to `(bookId: String, purchased: Boolean)`.** Currently takes a full `Book` and only uses `book.id` to write plus `book.copy(purchased = purchased)` to return. Could shrink to a `Result<Unit, _>` and let the ViewModel update its own state. Pure cleanup; defer.

## Decisions (resolved from v1 review)

| Decision | v1 question | Resolved |
|---|---|---|
| Drop notes debounce? | "Recommended yes" | **Yes** — drop. Unconditional under columnar updates. |
| Club-review debounce in same PR? | "Recommended follow-up" | **Follow-up** — Firestore exposure differs. Out of scope here. |
| Commit shape | "Recommended single commit" | **Single commit.** All parts only make sense together. |

## Execution order once approved

1. DAO: add four column-update queries with docstrings citing the same rationale as `updateDescription`.
2. Repository: drop cache merge from `getBookById`; add `peekPreview`; surface the four new DAO methods.
3. Use cases: rewrite `UpdateBookMetadataUseCaseImpl` and `ToggleBookPurchaseUseCaseImpl`; update `GetBookDetailsUseCaseImpl` to compose cache fallback.
4. `UpsertBookUseCaseImpl` (and any club-sync insert paths surfaced during step 3): set `dateAdded` defaulting at insert.
5. View model: simplify `OnPersonalNotesChange`, collapse `OnBackClick`, drop `saveNotesJob` and `DebounceDelayMs`.
6. Screen: gate `ReadingStatusCard` / `PersonalNotesCard` / `PurchasedToggleCard` on `state.isInLibrary` (alone — see §5 matrix; the v2 `|| hasShelfContext` rendered cards on shelf-search previews).
7. Tests: DAO no-op tests, repo split-API tests, use-case tests, ViewModel back/notes tests, screen gate test.
8. Manual device re-verification on Pixel_8 (same matrix as commit `52fea692`; the back-press path is the headline check).
9. Doc + memory updates (append to `preview-cache-library-leak.md`; update memory entry).
10. Single commit: `fix(library): column-scoped updates for personal book metadata`.
