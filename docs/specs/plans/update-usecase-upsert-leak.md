# Update use case must not upsert — surviving preview-cache leak

**Status:** Plan drafted 2026-05-28 on branch `follow-up-fixes`. Awaiting approval before implementation.
**Origin:** Manual device verification of [preview-cache-library-leak.md](preview-cache-library-leak.md) on Pixel_8 (debug) revealed the preview leak still occurs on Bookshelf and Library search → tap → back. The preview-cache fix landed correctly; this plan addresses the **second** leak path that the cache work unintentionally re-armed.
**Scope:** Make `UpdateBookMetadataUseCase` (and any peer write-side use case relying on `getBookById` for an exists-check) honest about being update-only — never an upsert. Restore the invariant *the local DB contains only books the user explicitly owns*.

## The bug

After landing the preview-cache fix, the reproduction from the original plan still leaks — but via a different code path:

1. Open Library or any Bookshelf.
2. Open the search dialog, search for a book you don't already have.
3. Tap the row (not the `+` button) — detail screen renders from the cache.
4. **Press Back.**
5. Open the Library — the previewed book is now there.

The leak is no longer in `OnSearchResultBookClick` (that path is clean). It is in `BookDetailAction.OnBackClick`.

## Root cause

`BookDetailViewModel.kt:256-271` runs an unconditional best-effort save on every back-press:

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

That handler exists to flush any debounced notes save that hasn't fired yet (the auto-save delays 2 s after the last keystroke; without the back-flush, typing then immediately pressing back loses the keystrokes).

`UpdateBookMetadataUseCaseImpl.kt:43-62` then does the classic **get-then-upsert** dance:

```kotlin
val existingBook = when (val getResult = bookRepository.getBookById(bookId)) {
    is Result.Success -> getResult.data ?: return Result.Error(DataError.Local.NOT_FOUND)
    is Result.Error -> return getResult
}
…
val updatedBook = existingBook.copy(…)
return bookRepository.upsertBook(updatedBook)
```

Before the preview cache, `getBookById` returned null for a previewed book → use case early-exited with `NOT_FOUND` → no upsert. `update` happened to behave like "update-existing-only" **by accident**. The preview cache changed the meaning of `getBookById` from *"is this book persisted?"* to *"is this book either persisted OR available for preview?"*. The use case still treats a non-null result as "the book exists in storage, safe to upsert." The cache hands it a previewed book → use case upserts → preview is promoted into the library.

## Naming vs. behaviour — the architectural smell

| Use case | Name implies | Actually does | Safe under cache? |
|---|---|---|---|
| `UpsertBookUseCase` | create-or-update | upsert | ✅ Intentional |
| `AddBookToShelfUseCase` | add to shelf | upsert + join | ✅ Intentional |
| **`UpdateBookMetadataUseCase`** | **update existing** | **read → mutate → upsert** | ❌ **Leak** |
| **`ToggleBookPurchaseUseCase`** | **toggle purchase flag** | **read → mutate → upsert** | ❌ Same shape; the purchase toggle card is currently shown for previewed books too |

`updateDescription` is safe — it routes through `dao.updateDescription(bookId, description)` which is a Room `UPDATE` statement; a missing row is a silent no-op. That's the shape every "update" use case should have.

## Why this is the right thing to fix, not the back-press handler

You could narrow the back-press handler to only fire when notes are dirty, or skip the save when the book isn't owned. Both are **symptom-suppressing patches** that leave the trap in place — any future caller of `updateBookMetadata` for a non-persisted book leaks again. The honest fix is to make `update` actually mean update: if the row doesn't exist, return `NOT_FOUND` and write nothing. Every existing caller already handles `NOT_FOUND` because the use case already declared it as a possible return.

The plan below also resolves a second, latent leak in `ToggleBookPurchaseUseCase`. The purchase toggle card is currently rendered for any non-tutorial, non-club book on the detail screen including previewed books — so tapping it on a preview would do the same thing the back-press does today. We fix the use case rather than hiding the card, because the use case being write-correct lets the UI safely show whatever it wants.

## The fix — domain-layer, no schema change

### 1. Repository: split the read API by intent

Add one method to `BookRepository` that only consults the DAO:

```kotlin
/**
 * DAO-only lookup. Returns null for books that exist solely in the preview cache.
 * Use this from write-side use cases that should refuse to operate on a book
 * the user hasn't yet added to their device.
 *
 * Prefer [getBookById] from read-side callers (e.g. the detail screen) — that
 * one falls back to the in-memory search preview cache.
 */
suspend fun getPersistedBookById(bookId: String): Result<Book?, DataError.Local>
```

Implementation in `BookRepositoryImpl`:

```kotlin
override suspend fun getPersistedBookById(bookId: String): Result<Book?, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        dao.getBookById(bookId)?.toBook()
    }
}
```

Leave `getBookById` and `cacheSearchPreviews` exactly as they are. The detail-screen render path (`GetBookDetailsUseCaseImpl`) continues to use `getBookById` and continues to fall back to the cache — that's still the right behaviour for "show me a preview."

### 2. Use cases: write-side switches to the DAO-only read

**`UpdateBookMetadataUseCaseImpl`**

```kotlin
val existingBook = when (val getResult = bookRepository.getPersistedBookById(bookId)) {
    is Result.Success -> getResult.data ?: return Result.Error(DataError.Local.NOT_FOUND)
    is Result.Error -> return getResult
}
…
return bookRepository.upsertBook(updatedBook)
```

The final call stays `upsertBook` — once we've confirmed the row exists in the DAO, the upsert is a true update (Room's `OnConflictStrategy.REPLACE`). The semantic is correct now: "we read from storage, we mutated, we wrote back." A previewed book can never reach the `upsertBook` line.

**`ToggleBookPurchaseUseCaseImpl`** — same change. The current `if (existingBook != null) … else …` branch becomes redundant: if there's no DAO row, return `NOT_FOUND`. (The "new book" branch was a vestige that only matters when the use case is called from a non-detail path; it's dead code in practice and removing it tightens the contract.)

**`UpsertBookUseCaseImpl`** and **`AddBookToShelfUseCaseImpl`** stay on the cache-aware `getBookById`. Their intent *is* "create or update," and the cache fallback is desirable — when the user explicitly adds a book they just previewed, we want to upsert with whatever data the cache holds. That's the whole point of the cache.

### 3. View model: drop the back-press best-effort save AND the debounce

Once the use case is honest, the back-press save still works correctly for owned books (it updates; for previewed books it no-ops with `NOT_FOUND`). But the back-press save **only exists** because notes are debounced — a flush-before-navigate. The debounce + back-flush pair is a complexity-pile we don't need:

- **Why the debounce existed:** to batch typing into one DB write per pause. SQLite handles thousands of writes/sec; per-keystroke writes are not a hardware concern.
- **Sync exposure:** personal notes are local-only (see `UpdateBookMetadataUseCase` docstring — "This data is NOT exported/shared"). No Firestore traffic per keystroke. Sync to clubs is explicit and only fires from `AddBookToShelfUseCase` for club shelves.
- **Risk of removing debounce:** none for correctness. State is already updated optimistically in the handler, so Compose recomposition doesn't bounce.

**Proposed shape** for `OnPersonalNotesChange`:

```kotlin
is BookDetailAction.OnPersonalNotesChange -> {
    _state.update { it.updateBook { book -> book?.copy(personalNotes = action.notes) } }
    viewModelScope.launch {
        when (val r = bookDetailUseCases.updateBookMetadata(bookId, personalNotes = action.notes)) {
            is Result.Success, is Result.Error.Local.NOT_FOUND -> Unit  // previewed = no-op
            is Result.Error -> _state.update { it.withError(r.error, "update personal notes") }
        }
    }
}
```

And **`OnBackClick`** collapses to a pure navigation handler:

```kotlin
BookDetailAction.OnBackClick -> onNavigateBack?.invoke()
```

Drop `saveNotesJob: Job?` and the `DebounceDelayMs` constant. Same treatment for `OnClubReviewTextChange` (the club-review path has its own debounce pair — `saveReviewJob` — that exists for the same reason and can come along for the ride; club reviews go to Firestore, but they're under explicit user control via the rating widget anyway, and the per-keystroke write only goes to Firestore if the user is actively typing in the review text field, which is a rare and short-duration action). Whether to remove the club-review debounce in the same change or defer it is a call-out below.

### 4. Tests

| Test | File | What it asserts |
|---|---|---|
| `getPersistedBookById returns null for cached-only book` | `BookRepositoryImplTest.kt` | Cache populated, DB empty → null. Locks the API contract. |
| `getPersistedBookById returns DAO row when present` | `BookRepositoryImplTest.kt` | Locks DAO precedence. |
| `updateBookMetadata returns NOT_FOUND for cached-only book` | `UpdateBookMetadataUseCaseTest.kt` (new file) | Locks the leak fix at the use-case boundary. |
| `updateBookMetadata succeeds and upserts for persisted book` | same | Confirms owned-book path still works. |
| `toggleBookPurchase returns NOT_FOUND for cached-only book` | `ToggleBookPurchaseUseCaseTest.kt` (new file or existing) | Same shape for the second use case. |
| `OnBackClick does not call updateBookMetadata` | `BookDetailViewModelTest.kt` | Locks the simplified back handler. |
| `OnPersonalNotesChange calls updateBookMetadata per change` | `BookDetailViewModelTest.kt` | Locks the per-change save. |
| `previewed book: OnBackClick leaves Library empty` (integration) | new test in `BookDetailViewModelTest.kt` using real `MockBookRepository` | The headline regression test — the one that would have caught this before manual device verification. |

### 5. Plan + memory updates

- Append a "Follow-up: surviving leak via update use case" section to `preview-cache-library-leak.md` linking to this plan, so the audit trail is one click.
- Update `prerelease-no-migrations.md` is **not** needed — this is a domain-layer fix.
- Update the auto-memory `preview-cache-library-leak.md` to note that the cache fix alone wasn't sufficient and reference the second plan, so future sessions don't repeat the verification miss.

## Out of scope

- **Hiding the purchase toggle / reading-status / notes cards for previewed books on the detail screen.** Once the write paths are correct, showing those cards on a preview is harmless (writes are no-ops). Whether to also hide them for UX clarity is a separate UX call.
- **Removing the club-review debounce.** Symmetric problem but Firestore-bound, so the cost/benefit is different. Leave it untouched unless we explicitly choose to address it; this plan flags the parallel but does not act on it.
- **A wider audit for other "get-then-upsert" use cases.** Three were audited (`UpdateBookMetadata`, `ToggleBookPurchase`, `UpsertBook`, `AddBookToShelf`). Of these, the first two are write-side and need the fix; the latter two are intentional upserts and should keep cache-aware reads.

## Open decisions (the only things needing a call from you)

1. **Drop the notes debounce entirely as part of this fix?** (Recommended yes — debounce is the proximate complexity that produced the bug. SQLite writes are cheap. Personal notes are local-only.)
2. **Same treatment for the club-review debounce in the same PR, or follow-up?** (Recommended follow-up — Firestore exposure changes the calculus.)
3. **Commit shape:** one cohesive commit `fix(library): scope updateBookMetadata to persisted books only` covering all of (1)–(4), or split repo / use-case / view-model? (Recommended single commit — the parts only make sense together; splitting would leave intermediate commits where the leak still exists or the back handler is half-rewritten.)

## Execution order once approved

1. Repository: add `getPersistedBookById` + impl + tests.
2. Use cases: switch `UpdateBookMetadataUseCaseImpl` and `ToggleBookPurchaseUseCaseImpl` to it + use-case tests.
3. View model: drop `saveNotesJob`, simplify `OnPersonalNotesChange`, collapse `OnBackClick` + view-model tests.
4. Integration test: previewed-book back-press leaves DB empty.
5. Manual device re-verification on Pixel_8 (the same matrix from the previous round).
6. Doc + memory updates.
7. Commit.
