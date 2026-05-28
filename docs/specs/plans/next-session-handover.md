# Next-session handover — deferred work after `follow-up-fixes`

**Status:** Handover doc for the next session. Written 2026-05-28 after `follow-up-fixes` was merged to `main`. Closed-testing pre-release gates from `closed-testing-pre-handover.md` are either landed or not yet applicable (no testers added, so 4.x verification/monitoring and tester comms haven't been triggered yet). This doc captures the architectural cleanups, Google Books backlog, and the one ready-to-implement feature that were explicitly deferred from this session.
**What landed on this branch:** column-scoped DAO writes for personal metadata (no more upsert-on-update bug), in-memory preview cache for search-tap, screen-gate matrix for personal cards, add-to-library affordance on detail screen, add/remove-from-shelf now stay on detail (mirror), search dialog scroll preservation, and a restored androidTest source set (41/41 green). See commits `52fea692` through `04fb9aad`.

## D — Ready-to-implement feature (highest leverage for the next session)

**D1 — "Show my library only" search-scope toggle**

Plan: [`library-scope-search-toggle.md`](library-scope-search-toggle.md). Self-contained, three-pass-reviewed in spirit (one round of staff review during drafting), implementer's open decisions called out explicitly. Lets users re-shelve owned books without re-querying Google Books.

Why first: highest user-visible value, smallest blast radius (no schema change, one new use case, one new toggle, one preference field), and the plan is already written to be implemented from cold.

## B — Architectural cleanups (deferred from `update-usecase-upsert-leak.md`)

**B1 — `ToggleBookPurchaseUseCase` signature shrink**

Currently:

```kotlin
suspend operator fun invoke(book: Book, purchased: Boolean): Result<Book, DataError.Local>
```

Takes a full `Book`, uses only `book.id` to call `bookRepository.updatePurchased(...)`, returns `book.copy(purchased = purchased)`. The full-Book param and the round-tripped book in the return are vestiges of the pre-v3 read-modify-upsert shape — under column-scoped writes they're noise.

Proposed:

```kotlin
suspend operator fun invoke(bookId: String, purchased: Boolean): Result<Unit, DataError.Local>
```

`BookDetailViewModel.OnPurchaseClick` already has the book in state, so it can update its own state on success without the round-trip:

```kotlin
is BookDetailAction.OnPurchaseClick -> {
    val currentBook = state.value.book ?: return
    viewModelScope.launch {
        when (val r = bookDetailUseCases.toggleBookPurchase(currentBook.id, !currentBook.purchased)) {
            is Result.Success -> _state.update {
                it.copy(book = it.book?.copy(purchased = !currentBook.purchased))
            }
            is Result.Error -> _state.update { it.withError(r.error, "toggle book purchase") }
        }
    }
}
```

Scope: one use case + impl + interface + DI registration + the ViewModel call site + tests. ~30 minutes. No risk to the persistence path — the DAO call doesn't change.

**B2 — Club-review debounce — leave as-is (recommended)**

The personal-notes debounce was removed in the column-update fix because writes are local-only and SQLite handles per-keystroke cheaply. The club-review debounce in `BookDetailViewModel.OnClubReviewTextChange` writes to Firestore, which has different economics: network round-trip per keystroke, billable operations, and user data exposed to other club members in real time which is arguably not what they want.

**My recommendation: drop this item.** The debounce is justified by network cost and serves the user (avoids broadcasting half-typed reviews to the club). Listed here only to close the loop with the `update-usecase-upsert-leak.md` "out of scope" note — not actually worth doing.

**B3 — Wider get-then-upsert audit**

The 2026-05-28 round audited four use cases (`UpdateBookMetadata`, `ToggleBookPurchase`, `UpsertBook`, `AddBookToShelf`) and fixed the two writes; `UpsertBook` and `AddBookToShelf` legitimately keep cache-aware reads since they compose `getBookById ?: peekPreview` for the "preserve metadata if already owned" branch. That audit was scoped to the personal-metadata path.

Likely remaining suspects (grep `bookRepository.getBookById` + nearby `.upsert`):

- `GetOrCreateTutorialBookUseCaseImpl.kt:26` — uses `getBookById`, but the tutorial id never enters the preview cache (per `update-usecase-upsert-leak.md §7`), so this is observationally safe. Worth verifying the contract is documented at the call site.
- Club-sync paths (`BookClubRepositoryHelper.kt`, `BookClubSyncRepositoryImpl.kt`) — these now default `dateAdded` at insert per the v3 fix; double-check they don't have the same get-then-upsert smell elsewhere.

Scope: investigation first (one grep + a few file reads), then targeted fixes only where the pattern actually leaks. Could be a no-op investigation that just confirms safety.

## C — Google Books backlog (from `google-books-followups.md`)

The full source of truth is `google-books-followups.md`. Summary of what remains, in rough priority order:

| # | Item | Cost | Value |
|---|---|---|---|
| C1 | Pagination (`startIndex`/`offset` + UI "load more") | Medium — touches API layer + UI | Real gap — limits result discoverability past first page |
| C4 | `saleInfo` capture — buy/read button accuracy + inline price | Medium — schema bump + DTO expansion + button-state logic | High — current "View on Google" button is generic; this enables "Buy — £7.99" / "Read free" / hide |
| C2 | PrintType filter toggle (exclude magazines) | Trivial | Low-medium — quality-of-life filter |
| C3 | Drop no-author Google records | Trivial | Low — fix-when-noticed |
| C5 | UK affiliate monetisation (Bookshop / Amazon / Hive) | Large — depends on C4 + legal disclosure work | Medium — actual revenue stream once you have any user base |
| ~~C6~~ | Cover-image quality (Dune 1965 white-bar) | N/A | **Explicitly not planned** — accept Google data limitation |

C4 + C5 are paired: C5 needs the `saleability` signal that C4 produces, and the monetisation work only makes sense once there's at least a small user base to monetise. C1 is the most user-visible gap independent of monetisation.

## Recommended ordering for the next session

1. **D1** — highest leverage, plan is implementation-ready, single commit.
2. **B1** — tiny cleanup, can ride alongside D1 or as a separate commit.
3. **C1** (pagination) — most user-visible Google Books gap, independent of monetisation.
4. **C4** — only if monetisation is in scope; otherwise defer until there's pull from real usage.
5. **B3** — investigation only, low priority.
6. Skip **B2**, **C2**, **C3**, **C5**, **C6** unless they surface as user complaints.

## Explicitly accepted, not work

- Search dialog z-order during outbound nav (the AlertDialog scrim showing the new screen through it briefly). Cosmetic, leave unless it becomes annoying after multiple visits.
- Cover-image quality (C6 above).
- B2 (club-review debounce).
