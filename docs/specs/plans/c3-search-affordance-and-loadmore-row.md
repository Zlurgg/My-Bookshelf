# C3 — Visible search affordance + list-row "Load more"

Status: draft
Branch target: `last-few-tweaks-I-promise` (or a follow-up branch — single feat commit at the end)
Depends on: C2 (`docs/specs/plans/c2-tap-to-search-keyboard-dismiss.md`) — landed in 4033a482

## Why

On-device testing after C2 surfaced two UX gaps:

1. **Submit is invisible.** With debounce removed, results only appear after the user fires IME Search. There's no on-screen affordance saying "tap here to search," so users who don't notice the keyboard's magnifying-glass key sit on an empty list assuming the app is broken.
2. **Load More looks like a floating control.** The "Load more results" Material `Button` floats centered below the last result, breaking the list rhythm. Reads as a separate UI element rather than a continuation of the result stream.

## What changes

### 1. Trailing search button in `BookSearchBar`

Google-mobile style: the search icon moves from the leading slot to the trailing slot, sitting next to the existing X clear button with a thin vertical separator between them. The leading icon is removed (the placeholder hint `Search…` carries the affordance on its own when the field is empty; the trailing controls take over once typing starts).

- **Leading icon**: removed.
- **Trailing slot**: a `Row` containing
  - `IconButton` (magnifying glass) → `onSubmitSearch()` — visible whenever `searchQuery.isNotBlank()`.
  - thin vertical `Divider` (1.dp wide, `outlineVariant`, ~24.dp tall) — visible whenever the search icon is.
  - `IconButton` (X close) → `onClear()` — unchanged behaviour, same visibility predicate as today.
- Both icons share the existing `AnimatedVisibility` gate keyed on `searchQuery.isNotBlank()` so they appear/disappear together. Empty field → trailing slot is empty, only the `Search…` hint shows.
- The trailing search button MUST also call the same `dismissKeyboard()` lambda the IME Search path already calls. This is done at the dialog level (see file edits below), not inside `BookSearchBar`, to keep the bar free of `LocalSoftwareKeyboardController` coupling.

#### Why not collapse into a single toggling icon?

Considered — one icon that shows "search" when `query != lastSubmittedQuery` and "clear" when they match. Rejected: less discoverable (the icon's meaning changes silently), worse for accessibility (content description churns), and the two-icon pattern matches what users already know from Chrome/Google search.

### 2. `BookSearchDialog` — Load More as a list row

The footer item is already inside the `LazyColumn` (`item(key = LOAD_MORE_ITEM_KEY)`). The visual treatment changes from "centered `Button`" to "tappable list row":

- Top `HorizontalDivider` (`outlineVariant`) above the row so it reads as a continuation, not a separate control.
- Row is full-width clickable (the whole row is the tap target — no inset `Button`).
- Content: leading `Icon` (`Icons.Default.ExpandMore`, primary tint) + centered `Text("Load more results", primary, medium weight)`.
- Row height matches the result-row padding (`vertical = 12.dp`) so the rhythm is preserved.
- **Loading state**: same row geometry — `CircularProgressIndicator(size = 20.dp)` centered, row is non-clickable. No layout shift on the spinner ↔ button transition (this is why we keep the `LOAD_MORE_ITEM_KEY` stable across states; that part of C1 stays).
- Striping: the row is **not** striped. It sits on `MaterialTheme.colorScheme.surface` regardless of result count parity — actions don't follow data striping.

## Files affected

### `app/src/main/java/uk/co/zlurgg/mybookshelf/book/presentation/searchcomponents/BookSearchBar.kt`

- Add param `onSubmitSearch: () -> Unit` after `onImeSearch`.
  - Initially considered folding `onSubmitSearch` and `onImeSearch` into one. Keep them split: the IME path goes through the keyboard action; the icon-tap path bypasses it. Both reach the same VM action via the dialog's wiring, so duplication is in the leaf component only.
- Remove `leadingIcon = { … }`.
- Replace `trailingIcon` block with a `Row` containing the two `IconButton`s and a vertical `Divider`. Both `IconButton`s gated by `AnimatedVisibility(visible = searchQuery.isNotBlank())`.
- Update preview composables in the same file (if any) to pass `onSubmitSearch = {}`.

### `app/src/main/java/uk/co/zlurgg/mybookshelf/book/presentation/searchcomponents/BookSearchDialog.kt`

- Pass the new `onSubmitSearch` to `BookSearchBar`, wrapping it so it calls `dismissKeyboard()` first (same lambda the IME path uses today on line 113–116):
  ```kotlin
  BookSearchBar(
      …,
      onImeSearch = { dismissKeyboard(); onSubmitSearch() },
      onSubmitSearch = { dismissKeyboard(); onSubmitSearch() },
      onClear = onClearSearch,
  )
  ```
- In the `item(key = LOAD_MORE_ITEM_KEY)` block (lines 315–340):
  - Add `HorizontalDivider` before the row.
  - Replace the `Button` with a full-width `Row` modifier chain: `.fillMaxWidth().clickable(enabled = !state.isLoadingMore) { dismissKeyboard(); onLoadMore() }.padding(horizontal = 16.dp, vertical = 12.dp)`.
  - Inside: `Icon(Icons.Default.ExpandMore, …)` + `Spacer(8.dp)` + `Text("Load more results", color = primary, fontWeight = Medium)`.
  - For `isLoadingMore`: same outer Row geometry, single centered `CircularProgressIndicator(size = 20.dp)`, no leading icon, no text.

### Preview / dialog wrappers

Both consumers of `BookSearchDialog` pass `onSubmitSearch` already (added in C2). No VM, no action, no callback-bag changes — the affordance edits are display-layer only.

- `bookshelf/presentation/searchcomponents/ShelfBookSearchDialog.kt`: no change. `onSubmitSearch` is already wired.
- `library/presentation/searchcomponents/LibraryBookSearchDialog.kt`: no change. Same.

Any preview-only Composables that instantiate `BookSearchBar` directly (grep `BookSearchBar(`) — pass `onSubmitSearch = {}` in their preview args.

## Strings

Reuse existing strings:
- `R.string.search_load_more` — "Load more results" — already present.
- `R.string.search_hint` — placeholder, unchanged.
- `R.string.cd_clear_search` — X content description, unchanged.

Add **one** new string for the trailing search icon's content description:
- `R.string.cd_submit_search` — "Search". Place alphabetically in `strings.xml`. No `<plurals>`, no formatting args.

## Tests

This is a display-only change — no VM logic moves. The C2 unit tests (`OnSubmitSearch` handler, `lastSubmittedQuery` invariant, `OnClearSearch`, load-more race-guard) all stay green untouched.

Add **one** Compose UI test (if `app/src/test` has Robolectric + ComposeTestRule infra — check before adding; otherwise skip and mark verified manually):

- `BookSearchDialogTest` — "trailing search icon click submits query":
  - Render dialog with `state.query = "abc"`, `state.lastSubmittedQuery = ""`.
  - Assert search-icon node `onNodeWithContentDescription("Search")` is displayed.
  - Click it → verify the `onSubmitSearch` lambda was invoked exactly once.

Grep for existing `BookSearchDialog` Compose tests first; if none exist, do **not** stand up Robolectric + Compose infra just for this — the manual trace below covers it.

## Manual verification (device)

On the OnePlus 6T release build:

1. Open Library tab → tap shelf → tap search FAB.
2. Empty field: confirm leading icon is gone, only the `Search…` placeholder is visible. Trailing slot is empty.
3. Type `harry` — confirm the trailing slot now shows: [search icon] | [X] with a thin vertical divider between them. Both tap targets are clearly distinct.
4. Tap the magnifying glass → confirm: keyboard dismisses, search fires, results appear, `lastSubmittedQuery` is updated (visible because Load More becomes available if there are enough results).
5. Tap the X → confirm: query clears, results clear, keyboard stays where it was (no re-show; the lambda is the same `onClearSearch` from C2).
6. Scroll to the bottom of the result list → confirm Load More appears as a list-shaped row with a divider above and the `ExpandMore` icon, *not* as a floating filled button.
7. Tap Load More → confirm the row collapses to a centered spinner with no jump in layout, then expands back to results when the page arrives.

## Out of scope

- Any change to VM state, actions, callback bags, repository, or use cases. This is pure display.
- The Phase 2 `RemoteSearchController` extraction — still gated by the C2 plan trigger, untouched here.
- The library list filter `OutlinedTextField` at `LibraryScreen.kt:247` — that's a different component, not `BookSearchBar`. Leave it.

## Acceptance

- `./gradlew compileDebugKotlin testDebugUnitTest detekt` all green.
- Manual trace 1–7 above passes on device.
- Single commit: `feat(search): visible submit affordance + list-row load more`.

## Risk / rollback

Low risk — display only, no behaviour change. Rollback is a `git revert` of the single feat commit; no schema, no preference, no migration entanglement.
