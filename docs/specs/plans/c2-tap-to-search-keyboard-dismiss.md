# C2 — Tap-to-search + keyboard dismiss

**Status:** Plan v7 — seven review passes folded in (B1–B3, D1–D4, S1–S16, N1–N2). Cleared for execution; Phase 2 sketched as the follow-up commit.
**Scope (Phase 1):** Replace debounced-as-you-type remote search with explicit IME Search trigger. Dismiss the soft keyboard on submit, on Load More, and on dialog dismiss. Fix the four review-surfaced regressions (X-button clear, filter-toggle pre-submit, stale Load More params, stale empty-state text) and tighten state hygiene. Library-scope (local) search keeps its current type-to-filter behaviour.
**Scope (Phase 2):** Extract the now-mirrored remote search machinery into a shared `RemoteSearchController`. Separate commit, separate review.

## Why this exists

After C1 (`feat(search): paginate remote results with load-more button`, commit `e99ec4ff`) shipped, two related UX problems were observed on device:

1. **Wasted API quota from typing pauses.** Current debounce is 300ms. Typing "harry potter" with any mid-word pause fires 2-4 searches. Each search is now 1 of 1000 daily Google Books requests; with the silently-capped 20-results-per-call (see C1 §"Why MAX_RESULTS=20"), each wasted call hurts more than under the old 40-cap assumption.
2. **Keyboard never closes.** The IME's Search button is wired (`BookSearchBar.kt:68-76`) but the dialog's `onImeSearch` callback is a literal no-op with the comment "handled by onQueryChange as user types" (`BookSearchDialog.kt:85`). So tapping the keyboard's Search action does nothing, and the keyboard stays up — blocking the lower half of the results when the user tries to scroll.

The two problems share a root cause: search is driven entirely by the typing debounce path, and the IME action is decorative.

## Decisions made (do not re-litigate)

- **Option A** chosen over B (longer debounce) and C (hybrid). The 20-cap math means each saved call matters more than the cost of teaching users to tap. Pre-release, this is the right time to set the pattern.
- **Library-scope mode is unchanged.** Local filtering is free and instant — type-to-filter is the right pattern there (think iOS Contacts, Spotify local library). Forcing tap-to-search for local results would be a regression.
- **Keyboard dismisses on**: IME Search tap, Load More tap, dialog dismiss. Not on every recomposition (that would fight with focus while typing).
- **`lastSubmittedQuery` is "the query that produced the currently-displayed results."** Single invariant across both modes:
  - Remote mode: written by `OnSubmitSearch`. Filter-toggle retrigger fires this, never an unsubmitted typed one — so typing "harr" + toggling Safe Search does NOT send "harr" to Google Books.
  - Library mode: written by `OnSearchQueryChange` on every keystroke (type-to-filter means the displayed results track the typed query in lockstep). Keeps the empty-state hint check and Load More gate working uniformly without per-mode branches in the dialog.
  - **Brief invariant lapse during in-flight searches.** `lastSubmittedQuery` is written *before* `performSearch` runs (both modes). For the duration of the in-flight call it describes "what is being searched" rather than "what is displayed." Empty-state predicates require `!isLoading`, so the user never sees the lapse — but reviewers shouldn't read the invariant as strictly point-in-time.
- **Scope-toggle handling clears `lastSubmittedQuery` when leaving library mode.** See §Fix E. Without this, the invariant write in library mode leaks into remote-mode retriggers and fires unrequested Google Books calls. Single-site fix at the toggle handler; preserves the field as one canonical value.
- **Accept divergent typed/submitted display in remote mode.** Submit "harry" → type "harry potter" without re-submitting → toggle Safe Search → re-fire is for "harry" while the search bar shows "harry potter." Defensible (the user did not ask Google for "harry potter"); the Load More gate (§Fix A) already hides the pagination affordance during divergence. Empty-state text uses `lastSubmittedQuery` (§Fix B) so the message at least matches the data. Flagged here so C1 reviewers don't treat it as a bug.
- **Drop debounce from the remote flow; keep it for library scope.** Reviewer correctly noted 300ms is user-perceived latency under tap-to-search (tap → wait → results). Library-scope's `observeDebouncedLocalQuery` keeps its 300ms because type-to-filter does benefit from debouncing the local re-filter pass. Double-tap cost: `collectLatest` cancels the in-flight `performSearch` on the second emit, but the underlying HTTP request may have already gone out the socket before cancellation reaches Ktor. Worst-case double-tap = one cancelled mid-flight request plus one new full request. Acceptable cost for removing the per-tap latency.
- **Drop `MIN_SEARCH_QUERY_LENGTH` entirely.** It was a debounce-era throttle for unintended calls. Under explicit submit, the user *asked*. If they want to search "h", let them — that's autonomy, not a bug. Only an `isBlank()` guard remains (the API requires a non-empty param). Consequence: no inline "type at least 2 chars" hint either.
- **Drop the `isTyping` field entirely.** Under tap-to-search there is no debounce window to wait through — the field becomes permanently false. Dead state and dead UI branches go.
- **Bundle A/B/C/D into Phase 1; extract E to Phase 2.** A/B are correctness bugs the typed/submitted split exposes — must ship together. C/D are tiny hygiene wins relevant because we're already touching this state. E (RemoteSearchController) is a multi-file refactor that doubles the diff and obscures the behavioural change; ship it next.

## Existing code state (verified during handoff, 2026-05-30)

- `BookSearchBar.kt:68-76` — IME action already configured as `ImeAction.Search`, `KeyboardActions(onSearch = { onImeSearch() })`. Callback is passed through correctly; only the dialog's wiring is dead.
- `BookSearchBar.kt:82-84` — X-button `IconButton.onClick` calls `onSearchQueryChange("")`. Must move to a dedicated `onClear` lambda (see §Phase 1 Fix C).
- `BookSearchDialog.kt:82-86` — `BookSearchBar` called with `onImeSearch = { /* handled by onQueryChange as user types */ }`. This is the dead callback.
- `BookSearchDialog.kt:110, :126, :166` — `isTyping` branches. Remove with the field.
- `BookSearchDialog.kt:127-148` — result-count and "N results found" text read `state.results.size` (fine) but the no-results text at `:198` reads `state.query`. Must switch to `lastSubmittedQuery` (see §Phase 1 Fix B).
- `BookSearchDialog.kt:283-303` — Load More footer renders on `state.canLoadMore || state.isLoadingMore`. Must additionally check `state.query.trim() == state.lastSubmittedQuery.trim()` (see §Phase 1 Fix A).
- `BookshelfViewModel.kt:143-153` — `OnSearchQueryChange` updates query in state AND calls `queryFlow.tryEmit(action.query)` — this is the debounce-driven auto-search path.
- `BookshelfViewModel.kt:46-47` — `SEARCH_DEBOUNCE_MS = 300L`, `MIN_SEARCH_QUERY_LENGTH = 2`. The latter goes away.
- `BookshelfViewModel.kt:60` — `private val queryFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)`.
- `BookshelfViewModel.kt:111-115` — `OnDismissSearchDialog` calls `closeSearchDialog()` then `queryFlow.tryEmit("")`. Will consolidate (see §Phase 1 Fix C).
- `BookshelfViewModel.kt:334-366` — `observeDebouncedQuery()` with the merged `queryFlow + loadMoreFlow` `collectLatest` (load-bearing per C1).
- `BookshelfViewModel.kt:381-390` — `retriggerSearchIfNeeded()` emits to `queryFlow` when filters toggle. Source must change to `lastSubmittedQuery`.
- `BookshelfViewModel.kt:553-567` — `closeSearchDialog()` manually rebuilds `BookSearchState(...)` listing every field to preserve. Brittle; will be replaced (see §Phase 1 Fix D).
- `LibraryViewModel.kt:43-44, :141-142, :152, :294, :388-407` — same shape; remote uses `remoteQueryFlow.debounce`, library scope uses a separate flow with its own debounce (line 376) and is independent.
- `BookshelfAction` / `LibraryAction` have no `OnSubmitSearch` or `OnClearSearch` action yet — both need adding.
- `BookSearchCallbacks` needs `onSubmitSearch` and `onClearSearch`.

## Critical correctness — what NOT to break

C1 invariants that must survive Phase 1:

1. **The merged `queryFlow + loadMoreFlow` `collectLatest` in both VMs is the load-bearing cancellation primitive.** Any new trigger cancels in-flight `performSearch`. Don't refactor to per-call `Job` tracking — the toggle-during-load-more race comes back. Note: only the *debounce* on the remote leg goes away; the merge + collectLatest stays.
2. **`retriggerSearchIfNeeded()`** must continue to fire when filter checkboxes toggle. Source becomes `lastSubmittedQuery` uniformly across both modes (the field is now invariant: "the query that produced the displayed results").
3. **Library-scope mode keeps type-to-filter.** The keyboard does NOT need dismissing for library scope (no scroll-blocking issue — list updates inline, dialog stays compact). `OnSearchQueryChange` in library mode also writes `lastSubmittedQuery = query` to maintain the invariant — see §Decisions.
4. **`tryEmit("")` on dismiss/clear must stay** (now in the shared `resetSearchState` helper — see §Phase 1 Fix C). It's the only thing that cancels an in-flight `performSearch` when the user dismisses mid-fetch.

## Phase 1 — behavioural change + four review fixes

### Fix A. Load More gates on `query == lastSubmittedQuery` — UI **and** VM (correctness)

**Problem.** With typed `query` separated from submitted `lastSubmittedQuery`, a user can submit "harry", get page 1, type "harry potter" without submitting, then tap Load More. `performSearch(append=true)` reads `searchState.toSearchParams()` from current state — using the new typed query — but `searchState.nextStartIndex` is from the "harry" page. Result: mismatched query + cursor sent to Google.

**Fix.** Belt-and-braces — gate in both places:

UI gate, in `BookSearchDialog`:

```kotlin
val canShowLoadMore = (state.canLoadMore || state.isLoadingMore) &&
    state.query.trim() == state.lastSubmittedQuery.trim()
if (canShowLoadMore) {
    item(key = LOAD_MORE_ITEM_KEY) { /* button or spinner */ }
}
```

VM-side, in the `OnLoadMore` handler:

```kotlin
val canFire = s.lastSubmittedQuery.isNotBlank() &&
    s.query.trim() == s.lastSubmittedQuery.trim() &&  // race guard
    s.canLoadMore &&
    !s.isLoadingMore &&
    !s.libraryScopeEnabled
if (canFire) loadMoreFlow.tryEmit(Unit)
```

Why both: the UI gate makes the divergence visible (Load More vanishes when the user starts refining). The VM gate closes the race where a tap dispatched before the recomposition lands; without it, a malformed call slips through. Cheap to add; eliminates the race entirely.

### Fix B. Result-count and no-results text read `lastSubmittedQuery` — and the predicate matches (UX)

**Problem.** `BookSearchDialog.kt:198` interpolates `state.query` into the empty-state message ("No results for 'foo'"). Under tap-to-search, the rendered results belong to `lastSubmittedQuery`. User submits "harry" → no results → starts typing "harry potter" → message reads "No results for 'harry potter'" — a query they never searched for.

**Fix.** Replace `state.query` with `state.lastSubmittedQuery` in *both* the predicate at `:170` and the string interpolations. Reviewer S2 was right to flag the asymmetry: if the predicate still reads `state.query.isNotBlank()` and the text reads `state.lastSubmittedQuery`, the user can backspace the field to empty (without tapping X) and trigger empty-string interpolation ("No results for ''") on a non-empty submitted query. Unifying both on `lastSubmittedQuery` is the correct fix, not an inline comment.

Five changes in `BookSearchDialog`:

- `:170` predicate: `state.query.isNotBlank()` → `state.lastSubmittedQuery.isNotBlank()`.
- `:188` empty-library hint check: `state.query.isBlank()` → `state.lastSubmittedQuery.isBlank()`.
- `:193` "No library results for X" string: `state.query` → `state.lastSubmittedQuery`.
- `:198` "No results for X" string: `state.query` → `state.lastSubmittedQuery`.
- Cross-mode safety: works uniformly *because* `lastSubmittedQuery` is now written on every library-mode keystroke (see §Decisions invariant) and is cleared on library→remote transition (see §Fix E).

Result-count text (`:127-137`) uses `state.results.size` and `state.filteredCount`, which are already submission-aligned — no change needed there.

**Reviewer-flagged regression averted.** If `lastSubmittedQuery` were only written on `OnSubmitSearch`, the library-scope "Search your library" hint would render on top of real filtered results (the user types "h" → filter runs → `lastSubmittedQuery` stays blank → hint shows over the filtered list). The uniform-write decision (§Decisions) plus §Fix E (toggle handling) is what makes this fix safe.

### Fix C. Consolidate `OnDismissSearchDialog` and `OnClearSearch` via a shared helper (DRY)

**Problem.** Both reset search-result state and emit `""` to cancel in-flight. Two paths to maintain → divergence risk as state grows.

**Fix.** Single private helper, parameterised on whether to flip the dialog visibility flag. No action-recursion (reviewer correctly flagged self-dispatch as brittle: double recompositions, action-counting test breakage, future routing-table snags).

```kotlin
private fun resetSearchState(closeDialog: Boolean) {
    _state.update {
        it.copy(
            isSearchDialogVisible = if (closeDialog) false else it.isSearchDialogVisible,
            bookSearchState = it.bookSearchState.resetForDialogClose(),
        )
    }
    queryFlow.tryEmit("") // cancels in-flight via collectLatest
}

is BookshelfAction.OnClearSearch -> resetSearchState(closeDialog = false)
is BookshelfAction.OnDismissSearchDialog -> resetSearchState(closeDialog = true)
```

One `_state.update` per handler, one `tryEmit`. No self-dispatch. The `resetForDialogClose()` helper is §Fix D.

### Fix E. `OnToggleLibraryScope` must not fire an unrequested remote search (correctness)

**Problem (v3 review B3).** The uniform `lastSubmittedQuery` invariant writes the field on every library-mode keystroke. `OnToggleLibraryScope` already calls `retriggerSearchIfNeeded()` (`BookshelfViewModel.kt:222`). After v3, retrigger only checks `lastSubmittedQuery.isNotBlank()` to decide whether to fire. Reproduction:

1. Open dialog (remote, default). Do not submit.
2. Toggle library scope ON.
3. Type "h" in library scope → `OnSearchQueryChange` writes `lastSubmittedQuery = "h"`. Local filter runs.
4. Toggle library scope OFF → `retriggerSearchIfNeeded()` runs. `lastSubmittedQuery = "h"`, `libraryScopeEnabled = false` → emits "h" to `queryFlow` → remote Google Books call the user never tapped Search for.

Exactly the wasted-quota class of bug this whole plan was created to prevent.

**Fix.** Treat the toggle as authoritative. On transition INTO library mode, write `lastSubmittedQuery = query` so the invariant holds immediately and the local filter retriggers correctly. On transition OUT (library → remote), clear `lastSubmittedQuery` AND the result side of state, so the retrigger no-ops and the user sees a clean remote-mode slate. Field stays a single canonical value; no splitting.

```kotlin
is BookshelfAction.OnToggleLibraryScope -> {
    val current = _state.value.bookSearchState
    val newLibScope = !current.libraryScopeEnabled
    _state.update {
        it.copy(
            bookSearchState = if (newLibScope) {
                // Entering library scope: seed the invariant from current typed
                // query, let retrigger fire the local filter.
                it.bookSearchState.copy(
                    libraryScopeEnabled = true,
                    lastSubmittedQuery = it.bookSearchState.query,
                )
            } else {
                // Leaving library scope: clean break. Drop library-derived state
                // and require an explicit IME Search to hit Google.
                it.bookSearchState.copy(
                    libraryScopeEnabled = false,
                    lastSubmittedQuery = "",
                    results = emptyList(),
                    hasSearched = false,
                    canLoadMore = false,
                    nextStartIndex = 0,
                    filteredCount = 0,
                    isLoadingMore = false,
                )
            }
        )
    }
    persistSearchPreferences()
    retriggerSearchIfNeeded() // no-op on library→remote (lastSubmittedQuery blank)
}
```

Asymmetry is deliberate and matches the quota story: remote→library auto-runs (local filter is free); library→remote is a clean slate (no auto-quota burn). User keeps their typed text either way.

This was option 2 from the v3 reviewer's three options. Option 1 (split the field) reintroduces the duplication the uniform invariant was designed to remove. Option 3 (no retrigger on toggle) loses the remote→library convenience.

**Canonical within a VM, not across VMs.** `lastSubmittedQuery` stays a single field within `BookshelfViewModel`. But `libraryScopeEnabled` reaches `LibraryViewModel` via `SearchPreferences` (cross-VM back-channel) and could desync the invariants — see §Fix F.

### Fix F. LibraryViewModel must treat `libraryScopeEnabled` as inert (correctness)

**Problem (v4 review N1).** `LibraryViewModel.observeSearchPreferences()` writes `libraryScopeEnabled = prefs.libraryScopeEnabled` into its own state for round-trip persistence — today's code calls it out as "silently inert here." Under v4 the field becomes actively consulted by the mirrored handlers, opening two failure modes:

- **N1a.** Bookshelf toggles scope ON → DataStore persists → LibraryViewModel observes → its `bookSearchState.libraryScopeEnabled = true`. User opens Library tab's remote-search dialog (which has no scope toggle, so they can't fix it), types "harry", taps Search → `OnSubmitSearch` reads `libraryScopeEnabled=true` → no-op. Keyboard dismisses, no search runs, user stuck.
- **N1b.** Same setup → user types "h" without submitting → mirrored `OnRemoteSearchQueryChange` would write `lastSubmittedQuery = "h"` (library-scope branch) → user toggles a filter checkbox → `retriggerRemoteSearchIfNeeded()` fires "h" to Google. Exactly the B3 leak, through the cross-VM channel.

**Architectural framing.** The Library tab's "library scope" equivalent is the *outer* local-filter path (`localQueryFlow` → `applyFilters()`) — the type-to-filter on the Library list itself. Inside the remote-search dialog, "scope" is meaningless: the dialog is unambiguously remote, and the Library tab can't reach the user's other shelves anyway. The persisted flag is preserved purely so toggling it in Bookshelf round-trips correctly across app restarts.

**Fix.** Option (a) from reviewer N1: LibraryViewModel does NOT mirror the `libraryScopeEnabled` guards. Explicit per-VM divergence, documented. Four total carve-outs — three VM-side (listed immediately below) plus one display-side (described in the "§Fix F display-side carve-out" subsection further down):

1. **`OnRemoteSearchQueryChange` does NOT write `lastSubmittedQuery` on type.** Library-side has no scope toggle and no §Fix E entering-branch seeding — so `OnSubmitSearch` is the only writer. (Inside `BookshelfViewModel`, the library-scope branch of `OnSearchQueryChange` *does* write; this is the asymmetry between VMs.)
2. **`OnSubmitSearch` does NOT guard on `libraryScopeEnabled`.** Only the `isBlank()` guard runs; the dialog is unambiguously remote here.
3. **`retriggerRemoteSearchIfNeeded()` does NOT consult `libraryScopeEnabled`.** Pure check: `if (lastSubmittedQuery.isBlank()) return; queryFlow.tryEmit(lastSubmittedQuery)`.

No `OnToggleLibraryScope` action is added to `LibraryAction`. The dialog never offers it.

`observeSearchPreferences()` continues to write `libraryScopeEnabled = prefs.libraryScopeEnabled` so `persistSearchPreferences()` reads back the same value on its own state writes — round-trip preservation unchanged. Update the existing inert-flag comment in LibraryViewModel.kt:85-89 to point at this Fix F note.

Reviewer offered option (b) — hardcode `libraryScopeEnabled = false` at the observe site — which also works but would require careful handling of `persistSearchPreferences` to avoid overwriting Bookshelf's true setting with false. Option (a) is the minimal-touch, no-write-side-effect fix. Option (c) — adding a real `OnToggleLibraryScope` to LibraryAction — is overkill for a flag with no UI surface.

**Behaviour-contract note (reviewer S11).** §Fix F also extends tap-to-search to the Library tab's remote-search dialog for the first time — `OnRemoteSearchQueryChange` is now typed-only, the same way the Bookshelf dialog works under the headline change. Same quota justification, already in scope per the plan's headline. The Library tab's *outer* local-filter path (the `localQueryFlow`-driven Library list filter, not the dialog) is untouched and keeps its 300ms debounce.

**§Fix F display-side carve-out (reviewer N2).** The three VM-side carve-outs above stop the leaked `libraryScopeEnabled` flag from breaking submit/retrigger/typing, but `BookSearchDialog` itself reads the flag at three display sites that the VM-side fix doesn't touch:

- `:69-70` — `showGoogleAttribution = !state.libraryScopeEnabled && results.any { GOOGLE_BOOKS }`. Library tab + leaked `true` → Google attribution suppressed even though results are from Google. TOS-relevant attribution silently disappears.
- `:168` — `showLibraryEmptyState = isEmptyResult && state.libraryScopeEnabled`. Library tab + leaked `true` + empty Google results → renders "No library results for harry" on what was a remote search.
- `:170` — `showRemoteEmptyState = ... && !state.libraryScopeEnabled && ...`. Mirror of the above — remote empty-state suppressed when it should show.

Fix: thread the existing `showLibraryScopeToggle: Boolean` parameter through display. The dialog already takes this and the Library tab call site already leaves it false (`BookSearchDialog.kt:61-63` comment: "Off by default so the Library dialog … keep today's UX"). Re-use the same semantic: if the toggle isn't shown, the user can't be in "library scope" from this dialog → treat the flag as false for display purposes.

```kotlin
// At the top of BookSearchDialog body:
val effectiveLibraryScope = state.libraryScopeEnabled && showLibraryScopeToggle

// Three references switched from state.libraryScopeEnabled to effectiveLibraryScope:
// 1. showGoogleAttribution memo key + condition (:69-70)
// 2. showLibraryEmptyState predicate (:168)
// 3. showRemoteEmptyState predicate (:170)
```

No new params, no caller knowledge of state-copy tricks, single derived local. Library tab passes `showLibraryScopeToggle=false` (already does) → `effectiveLibraryScope` is always false there → Google attribution shows, remote empty-state renders, library empty-state never triggers. Bookshelf dialog passes `showLibraryScopeToggle=true` and uses `libraryScopeEnabled` authoritatively (matches today's behaviour for that tab).

Reviewer offered options (ii) wrapper-passes-state-copy and (iii) explicit `treatAsRemoteOnly` param. (i) wins on minimality and on reusing an existing semantic the dialog already documents.

### Fix D. `resetForDialogClose()` helper on `BookSearchState` (hygiene)

*Forward-pointer (reviewer S13): Fix D is hygiene only. The load-bearing correctness fixes are above (A–C) and immediately preceding (E–F); this section is a cheap cleanup enabled because we're already touching `BookSearchState`.*

**Problem.** `BookshelfViewModel.kt:553-567` manually rebuilds `BookSearchState(existingBookIds = ..., searchByTitle = ..., /* etc */)`. Adding `lastSubmittedQuery` requires remembering to explicitly clear it here. Adding any future field requires remembering to either preserve or clear it. Easy to miss.

**Fix.** Add a method to `BookSearchState` that explicitly says what resets and lets the rest preserve by default:

```kotlin
// BookSearchState.kt
fun resetForDialogClose(): BookSearchState = copy(
    query = "",
    lastSubmittedQuery = "",
    results = emptyList(),
    hasSearched = false,
    isLoading = false,
    isLoadingMore = false,
    canLoadMore = false,
    nextStartIndex = 0,
    filteredCount = 0,
    errorMessage = null,
    // searchByTitle/Author/Subject, safeSearchEnabled, libraryScopeEnabled,
    // existingBookIds: PRESERVED by `copy()` not naming them.
)
```

Future additions are now opt-in to reset (name them in the `copy(...)`) vs opt-out to preserve (omit). Symmetric with how `withFreshSearch()` works for the search start, and removes the brittle field-list duplication in `closeSearchDialog()`.

LibraryViewModel's equivalent dismissal block (`LibraryViewModel.kt:128-141`) gets the same treatment.

### Other phase 1 changes

- `MIN_SEARCH_QUERY_LENGTH` deleted from both VMs and from `BookSearchState`. `OnSubmitSearch` only guards on `query.isBlank()`.
- Below-MIN inline hint deleted from the dialog. No string resource needed.
- `isTyping` field deleted; `withLoading()`, `withFreshSearch()`, `withBelowMinLength()` lose their `isTyping = false` lines; dialog drops the `isTyping` branches at `:110, :126, :166`.
- `withBelowMinLength()` is replaced by `resetForDialogClose()` for the dismiss/clear path. It's no longer called from `observeDebouncedQuery` either — the only debounce-path consumer was the min-length branch.
- **`observeDebouncedQuery()` drops its remote-flow `.debounce(SEARCH_DEBOUNCE_MS)`.** `merge(queryFlow.map { Fresh(it) }, loadMoreFlow.map { More })` only — no debounce on the remote leg. Library-scope's separate `observeDebouncedLocalQuery()` keeps its 300ms (type-to-filter benefits). Rename the method to `observeSearchTriggers()` — reviewer S1 correctly noted "Remote" is inaccurate because library-mode searches also flow through this observer (via `retriggerSearchIfNeeded()` → `tryEmit` → merge → `performSearch` → `performLibrarySearch` when `libraryScopeEnabled`). The merge/collectLatest is the single chokepoint for both modes; the rename should describe that. `SEARCH_DEBOUNCE_MS` companion stays in `LibraryViewModel` for the local-filter path; deleted from `BookshelfViewModel`.
- **`OnSearchQueryChange` writes `lastSubmittedQuery = action.query` when in library scope.** Maintains the "currently-displayed query" invariant (see §Decisions). The retrigger then fires correctly without per-mode logic. Remote mode stays untouched — only `OnSubmitSearch` writes `lastSubmittedQuery` there.
- **`OnSubmitSearch` guards for library scope.** No-op when `libraryScopeEnabled` is true (the type-to-filter path already keeps results live; submitting is redundant and the dialog continues to dismiss the keyboard via its IME handler regardless). One VM-level guard beats a mode-conditional callback in the dialog.

## Phase 1 — architectural shape

### State

```kotlin
// BookSearchState.kt
data class BookSearchState(
    val query: String = "",
    val lastSubmittedQuery: String = "", // NEW
    val results: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    // REMOVED: val isTyping: Boolean = false
    val hasSearched: Boolean = false,
    val existingBookIds: Set<String> = emptySet(),
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = false,
    val searchBySubject: Boolean = false,
    val safeSearchEnabled: Boolean = true,
    val libraryScopeEnabled: Boolean = false,
    val filteredCount: Int = 0,
    val errorMessage: String? = null,
    val nextStartIndex: Int = 0,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
) {
    // ... existing canToggle* getters and toSearchParams() unchanged ...

    fun withLoading(): BookSearchState = copy(isLoading = true, errorMessage = null)

    fun withFreshSearch(): BookSearchState = copy(
        isLoading = true,
        isLoadingMore = false,
        errorMessage = null,
        results = emptyList(),
        nextStartIndex = 0,
        canLoadMore = false,
        filteredCount = 0,
    )

    fun resetForDialogClose(): BookSearchState = copy(
        query = "",
        lastSubmittedQuery = "",
        results = emptyList(),
        hasSearched = false,
        isLoading = false,
        isLoadingMore = false,
        canLoadMore = false,
        nextStartIndex = 0,
        filteredCount = 0,
        errorMessage = null,
    )

    fun withResults(results: List<Book>): BookSearchState = copy(
        isLoading = false,
        hasSearched = true,
        errorMessage = null,
        results = results,
    )
}
```

### Actions

```kotlin
// BookshelfAction.kt + LibraryAction.kt
data object OnSubmitSearch : ...
data object OnClearSearch : ...
```

### Callbacks

```kotlin
// BookSearchCallbacks.kt
val onSubmitSearch: () -> Unit
val onClearSearch: () -> Unit
```

`BookSearchBar` gains `onClear: () -> Unit`; the X icon calls `onClear()` (not `onSearchQueryChange("")`).

### ViewModel handlers (BookshelfViewModel; LibraryViewModel mirrors except where §Fix F carves out)

```kotlin
is BookshelfAction.OnSearchQueryChange -> {
    _state.update {
        val newSearchState = it.bookSearchState.copy(
            query = action.query,
            // Library scope: keep lastSubmittedQuery == query so the
            // "currently-displayed query" invariant holds (Fix B relies on this).
            lastSubmittedQuery = if (it.bookSearchState.libraryScopeEnabled) {
                action.query
            } else {
                it.bookSearchState.lastSubmittedQuery
            },
        )
        it.copy(bookSearchState = newSearchState)
    }
    if (_state.value.bookSearchState.libraryScopeEnabled) {
        retriggerSearchIfNeeded() // library scope: type-to-filter
    }
}

is BookshelfAction.OnSubmitSearch -> {
    val s = _state.value.bookSearchState
    if (s.libraryScopeEnabled) return@onAction // type-to-filter already live
    val q = s.query
    if (q.isBlank()) return@onAction // API requires non-empty param
    _state.update {
        it.copy(bookSearchState = it.bookSearchState.copy(lastSubmittedQuery = q))
    }
    queryFlow.tryEmit(q)
}

is BookshelfAction.OnClearSearch -> resetSearchState(closeDialog = false)
is BookshelfAction.OnDismissSearchDialog -> resetSearchState(closeDialog = true)

is BookshelfAction.OnLoadMore -> {
    val s = _state.value.bookSearchState
    val canFire = s.lastSubmittedQuery.isNotBlank() &&
        s.query.trim() == s.lastSubmittedQuery.trim() && // Fix A race guard
        s.canLoadMore &&
        !s.isLoadingMore &&
        !s.libraryScopeEnabled
    if (canFire) loadMoreFlow.tryEmit(Unit)
}
```

`retriggerSearchIfNeeded()` unifies across modes because `lastSubmittedQuery` is the live invariant in both:

```kotlin
private fun retriggerSearchIfNeeded() {
    val s = _state.value.bookSearchState
    if (s.lastSubmittedQuery.isBlank() && !s.libraryScopeEnabled) return
    // Library mode: lastSubmittedQuery == query (kept in sync by OnSearchQueryChange).
    // Remote mode: lastSubmittedQuery is the last submitted query.
    queryFlow.tryEmit(s.lastSubmittedQuery)
}
```

`observeSearchTriggers()` (renamed from `observeDebouncedQuery`; "Remote" would mislead — library-mode also flows through here) drops both the `MIN_SEARCH_QUERY_LENGTH` branch and the remote-leg debounce:

```kotlin
viewModelScope.launch {
    merge(
        queryFlow.map<String, SearchTrigger> { SearchTrigger.Fresh(it) }, // no debounce
        loadMoreFlow.map<Unit, SearchTrigger> { SearchTrigger.More },
    ).collectLatest { trigger ->
        when (trigger) {
            is SearchTrigger.Fresh -> {
                val query = trigger.rawQuery.trim()
                val libraryScope = _state.value.bookSearchState.libraryScopeEnabled
                if (query.isEmpty() && !libraryScope) {
                    // Cancellation emit from clear/dismiss — collectLatest above
                    // already cancelled any in-flight performSearch. Nothing to do.
                    return@collectLatest
                }
                performSearch(append = false)
            }
            SearchTrigger.More -> performSearch(append = true)
        }
    }
}
```

`performSearch` does NOT write `lastSubmittedQuery` on success — `OnSubmitSearch` already wrote it before the emit, and the library-scope path keeps it written via `OnSearchQueryChange`. Don't double-write.

### LibraryViewModel divergences (§Fix F)

The remote-search machinery mirrors Bookshelf with three exceptions, none of which consult `libraryScopeEnabled`:

```kotlin
// LibraryViewModel.kt

// 1. OnRemoteSearchQueryChange — typed-only, no lastSubmittedQuery write.
is LibraryAction.OnRemoteSearchQueryChange -> {
    _state.update { it.copy(bookSearchState = it.bookSearchState.copy(query = action.query)) }
    // No retrigger, no lastSubmittedQuery write — Library tab has no scope toggle,
    // and OnSubmitSearch is the sole writer of lastSubmittedQuery on this VM.
}

// 2. OnSubmitSearch — isBlank() guard only; libraryScopeEnabled NOT consulted.
is LibraryAction.OnSubmitSearch -> {
    val q = _state.value.bookSearchState.query
    if (q.isBlank()) return@onAction
    _state.update {
        it.copy(bookSearchState = it.bookSearchState.copy(lastSubmittedQuery = q))
    }
    remoteQueryFlow.tryEmit(q)
}

// 3. retriggerRemoteSearchIfNeeded — pure lastSubmittedQuery check.
private fun retriggerRemoteSearchIfNeeded() {
    val q = _state.value.bookSearchState.lastSubmittedQuery
    if (q.isBlank()) return
    remoteQueryFlow.tryEmit(q)
}
```

No `OnToggleLibraryScope` action. No §Fix E logic. `observeSearchPreferences()` keeps the round-trip write of `libraryScopeEnabled` — update the existing inert-flag comment (`LibraryViewModel.kt:85-89`) to point at §Fix F.

`OnRemoteSearchQueryChange` here is the dialog's typing handler — distinct from `LibraryAction.OnSearchQueryChange`, which drives the *outer* Library-tab local filter via `localQueryFlow`. The outer local-filter path is untouched by this entire plan.

### Dialog

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BookSearchDialog(
    state: BookSearchState,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,        // NEW
    onClearSearch: () -> Unit,         // NEW
    // ... existing params ...
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard = remember(keyboardController, focusManager) {
        {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // Fix A — typed must match submitted to paginate
    val canShowLoadMore = (state.canLoadMore || state.isLoadingMore) &&
        state.query.trim() == state.lastSubmittedQuery.trim()

    AlertDialog(
        onDismissRequest = {
            dismissKeyboard()                                  // Fix H — always
            if (!state.isLoading) onDismiss()
        },
        title = {
            Column {
                BookSearchBar(
                    searchQuery = state.query,
                    onSearchQueryChange = onQueryChange,
                    onClear = onClearSearch,                   // Fix C wire
                    onImeSearch = {
                        dismissKeyboard()
                        onSubmitSearch()
                    },
                )
                // ... SearchFilters unchanged ...
            }
        },
        text = {
            // Fix B — use lastSubmittedQuery in empty-state strings
            // (no-results-for, no-library-results-for, empty-library hint check)
            // ... rest of body unchanged except: ...
            if (canShowLoadMore) {
                item(key = LOAD_MORE_ITEM_KEY) { /* ... */ }
            }
        },
        // ... confirmButton unchanged ...
    )
}
```

## Test impact (Phase 1)

Existing tests:

- `BookshelfViewModelTest.kt` — any test that did `OnSearchQueryChange("harry")` + `advanceTimeBy(SEARCH_DEBOUNCE_MS)` must insert `OnSubmitSearch` before the advance.
- `LibraryViewModelTest.kt` — same for remote-search tests; library-scope tests unaffected.
- `BookSearchStateTest.kt` — delete `isTyping` assertions; delete the `withBelowMinLength` test added in the current uncommitted diff (replace with a `resetForDialogClose` test).
- Tests that toggle a filter and assert a search re-fires must now seed `lastSubmittedQuery` (via a preceding `OnSubmitSearch`).
- Any test referencing `MIN_SEARCH_QUERY_LENGTH` — delete.

New tests:

- `OnSearchQueryChange alone does not fire remote search` — the headline behaviour change.
- `OnSubmitSearch with blank query does not fire search` — replaces the min-length test.
- `OnSubmitSearch fires remote search and writes lastSubmittedQuery`.
- **`OnSubmitSearch in library scope is a no-op`** — (D3 from review) submit a query in library mode, assert no remote `performSearch`, assert `lastSubmittedQuery` stays whatever the typed-path already set.
- **`OnSearchQueryChange in library scope writes lastSubmittedQuery`** — (B1 from review) type "h" in library mode, assert `state.lastSubmittedQuery == "h"`. Covers the empty-state hint regression.
- **`Library scope empty-state hint check uses lastSubmittedQuery and remains correct`** — corollary: library mode + typed "h" + zero results → the no-results-for-X message renders with "h", NOT the "Search your library" pre-search hint.
- `Filter toggle after submit retriggers with lastSubmittedQuery, not typed query` — regression guard for the typed/submitted split. Type "harr" without submit → toggle Safe Search → assert no `performSearch` invocation.
- `Filter toggle before any submit does nothing remote` — corollary.
- `OnClearSearch resets results + lastSubmittedQuery + canLoadMore` — regression guard for the X-button bug.
- `OnClearSearch cancels in-flight remote search` — slow fake source, submit, clear before completion, assert no late state write.
- `OnDismissSearchDialog cancels in-flight and closes` — same shape via the consolidated `resetSearchState` path.
- `OnDismissSearchDialog and OnClearSearch produce identical bookSearchState modulo the visibility flag` — guards the shared-helper consolidation.
- `OnDismissSearchDialog preserves search prefs across reopen` — `resetForDialogClose()` must NOT touch `searchByTitle` etc.
- `Library scope: OnSearchQueryChange still drives filter` — asymmetry guard.
- `Load More gating: when typed query diverges from lastSubmittedQuery, OnLoadMore is no-op` — VM-side gate (B2). Submit, type past the submitted query without re-submitting, dispatch `OnLoadMore`, assert no `loadMoreFlow` emit.
- **`OnToggleLibraryScope: enter library mode seeds lastSubmittedQuery from query and retriggers local filter`** — §Fix E entering branch.
- **`OnToggleLibraryScope: leave library mode clears lastSubmittedQuery and result-side state, retrigger is no-op`** — §Fix E leaving branch. Set `lastSubmittedQuery = "h"` and `libraryScopeEnabled = true` in initial state, dispatch `OnToggleLibraryScope`, assert: `libraryScopeEnabled == false`, `lastSubmittedQuery == ""`, `results.isEmpty()`, `hasSearched == false`, no `performSearch` invocation.
- **`B3 regression — typed-only library query does not leak into remote retrigger`** — full reproduction: open dialog, toggle library ON, type "h", toggle library OFF; assert zero remote search use case invocations across the entire sequence.
- **`Empty-state predicate uses lastSubmittedQuery, not query`** — submit "harry" with zero results → backspace field to empty (no X tap) → assert empty-state still renders "No results for harry" (predicate non-blank because `lastSubmittedQuery` is non-blank).
- **`Library tab can submit remote search when persisted libraryScopeEnabled is true`** — §Fix F primary regression guard (reviewer N1a). Seed `SearchPreferences` with `libraryScopeEnabled=true`, instantiate `LibraryViewModel`, await `observeSearchPreferences` settling, dispatch `OnRemoteSearchQueryChange("harry")` then `OnSubmitSearch`, assert `libraryUseCases.searchBooks` is invoked.
- **`LibraryViewModel.OnRemoteSearchQueryChange does not write lastSubmittedQuery`** — §Fix F. Asserts the typed-only behaviour is the divergence from Bookshelf.
- **`LibraryViewModel.retriggerRemoteSearchIfNeeded does not consult libraryScopeEnabled`** — §Fix F N1b guard. Seed `libraryScopeEnabled=true` and `lastSubmittedQuery=""`, dispatch a filter toggle, assert no `remoteQueryFlow` emit. Then seed `lastSubmittedQuery="harry"` with `libraryScopeEnabled=true`, dispatch a filter toggle, assert emit fires (the flag is genuinely ignored, not flipped).
- **`Library dialog renders remote empty-state and Google attribution despite leaked libraryScopeEnabled=true`** (reviewer N2). **Manual verification only for now** — see §Execution step 20 ("§Fix F display-side trace"). The assertion itself (with `state.libraryScopeEnabled=true`, `showLibraryScopeToggle=false`, non-empty Google results → Google attribution renders; zero Google results + non-blank `lastSubmittedQuery` → remote empty-state renders) requires a Compose UI test harness which is out of scope for Phase 1. When the harness lands, this is the first regression to encode as an automated test.

**Test plumbing note.** `LibraryViewModelTest.kt:990` has a comment referencing `MIN_SEARCH_QUERY_LENGTH`. Plan §Test impact's "any test referencing MIN_SEARCH_QUERY_LENGTH — delete" is grep-based and will miss prose. Update the comment or remove it explicitly during step 17.

**Call-site audit for `BookSearchBar`.** Currently consumed by `BookSearchDialog` only. `LibraryScreen.kt:247` uses `OutlinedTextField` directly for the local library list's filter bar — NOT `BookSearchBar`, so the new `onClear` param does not propagate there. Confirmed safe.

**Compose UI tests remain out of scope.** Keyboard dismissal, dismiss-during-loading, and Load More gating are verified manually (see §Execution). Documented gap.

## Execution order (Phase 1)

1. **State.** Add `lastSubmittedQuery`, remove `isTyping`, remove `MIN_SEARCH_QUERY_LENGTH` companion. Add `resetForDialogClose()`. Update `withLoading()`/`withFreshSearch()` to drop `isTyping`. Delete `withBelowMinLength()` (no longer called). Update `BookSearchStateTest.kt`.
2. **Actions.** Add `OnSubmitSearch`, `OnClearSearch` to both Action sealed classes.
3. **Callbacks.** Add `onSubmitSearch`, `onClearSearch` to `BookSearchCallbacks`. Add `onClear` to `BookSearchBar` signature.
4. **VM: query change handler.** Remove `queryFlow.tryEmit` and `isTyping` write from `OnSearchQueryChange`. **Add the `lastSubmittedQuery = action.query` write under `libraryScopeEnabled`.** Preserve library-scope retrigger path.
5. **VM: submit handler.** New `OnSubmitSearch` — `libraryScopeEnabled` guard (no-op), `isBlank` guard, write `lastSubmittedQuery`, emit to `queryFlow`.
6. **VM: shared reset helper.** Add private `resetSearchState(closeDialog: Boolean)` — single `_state.update` (visibility flag conditional), `resetForDialogClose()` on bookSearchState, `tryEmit("")`. Wire `OnClearSearch` and `OnDismissSearchDialog` to it. **Grep for any other caller of `closeSearchDialog()`** (reviewer S5) before deleting the manual extension — should be the dismiss handler only, but verify.
7. **VM: retrigger.** `retriggerSearchIfNeeded()` unifies on `lastSubmittedQuery` — the invariant now holds in both modes.
8. **VM: observe loop.** Drop the remote-leg `.debounce(SEARCH_DEBOUNCE_MS)`. Drop the min-length branch. Replace with `isEmpty() && !libraryScope` early-return for the cancellation emit. Rename to `observeSearchTriggers()` (library-mode flows through here too — see reviewer S1).
9. **VM: constants.** `MIN_SEARCH_QUERY_LENGTH` deleted from both VMs. `SEARCH_DEBOUNCE_MS` deleted from `BookshelfViewModel`; kept in `LibraryViewModel` for `observeDebouncedLocalQuery()` only.
10. **VM: `OnLoadMore` gate.** Replace `lastSubmittedQuery.length >= MIN` with `lastSubmittedQuery.isNotBlank()`. **Add `s.query.trim() == s.lastSubmittedQuery.trim()`** to `canFire` (Fix A VM-side gate).
11. **VM: `OnToggleLibraryScope` rewrite (§Fix E).** Bookshelf only. Replace the simple toggle with the entering/leaving branches — entering writes `lastSubmittedQuery = query`; leaving clears `lastSubmittedQuery` AND result-side state (results, hasSearched, canLoadMore, nextStartIndex, filteredCount, isLoadingMore). Library scope toggle is asymmetric by design: remote→library auto-runs local filter, library→remote is a clean slate. Keep the existing `persistSearchPreferences()` + `retriggerSearchIfNeeded()` calls; the latter no-ops on the leaving branch because `lastSubmittedQuery` is now blank.
12. **VM (LibraryViewModel): apply §Fix F carve-outs.** `OnRemoteSearchQueryChange` does NOT write `lastSubmittedQuery`. `OnSubmitSearch` does NOT guard on `libraryScopeEnabled`. `retriggerRemoteSearchIfNeeded()` does NOT consult `libraryScopeEnabled`. No `OnToggleLibraryScope` action added. Update the inert-flag comment in `LibraryViewModel.kt:85-89` to reference §Fix F so future maintainers don't "fix" the divergence.
13. **Dialog: drop `isTyping`.** Remove three `isTyping` branches.
14. **Dialog: keyboard wiring.** `@OptIn(ExperimentalComposeUiApi::class)`, `remember`'d `dismissKeyboard`, wire to IME, Load More, and `onDismissRequest` (outside the loading guard).
15. **Dialog: Fix A — Load More UI gate.** Add `canShowLoadMore = (state.canLoadMore || state.isLoadingMore) && state.query.trim() == state.lastSubmittedQuery.trim()`.
16. **Dialog: Fix B — submitted-query text AND predicate.** Switch the empty-state *predicate* at `:170` (`state.query.isNotBlank()` → `state.lastSubmittedQuery.isNotBlank()`) along with the three empty-state string references. Five total. The predicate change is what avoids the empty-string interpolation regression reviewer S2 flagged.
16a. **Dialog: §Fix F display-side.** Add `val effectiveLibraryScope = state.libraryScopeEnabled && showLibraryScopeToggle` at the top of the dialog body. Replace `state.libraryScopeEnabled` with `effectiveLibraryScope` at the three display sites: the `showGoogleAttribution` memo (`:69-70` — both the `remember` key list AND the condition), the `showLibraryEmptyState` predicate (`:168`), and the `showRemoteEmptyState` predicate (`:170`). Library tab call site already passes `showLibraryScopeToggle=false`, so a leaked-true flag from prefs is ignored for display. Bookshelf dialog already passes `true` so display behaviour is unchanged for that tab. **Update the param doc at `BookSearchDialog.kt:61-63`** (reviewer S14) — the comment currently only describes toggle-UI gating; rewrite it to document the dual semantic: this param both gates the toggle UI visibility AND gates whether `state.libraryScopeEnabled` is consulted for display branching. A future contributor who flips this to `false` to suppress UI must understand they are also flipping display semantics; otherwise they could silently re-create N2 (Google content attributed to "your library").
17. **`BookSearchBar`: X wiring.** Add `onClear` param; X icon calls it instead of `onSearchQueryChange("")`.
18. **Wire through screens.** `BookshelfScreen` / `LibraryScreen` → `ShelfBookSearchDialog` / `LibraryBookSearchDialog` → `BookSearchDialog`. Both new callbacks.
19. **Tests.** Update existing, add new per §Test impact. Pay particular attention to: `OnClearSearch`, dismiss-cancels-in-flight, pre-submit-filter-toggle, library-scope `OnSubmitSearch` no-op, library-scope `OnSearchQueryChange` writes `lastSubmittedQuery`, Load More VM-gate divergence, and Library tab can submit remote search when persisted scope is true (§Fix F). Update the `LibraryViewModelTest.kt:990` comment that mentions `MIN_SEARCH_QUERY_LENGTH`.
20. **Manual device verification.**
    - Type "harry potter" → no search fires while typing → IME Search tap → search fires, keyboard closes, results render (no 300ms wait — debounce gone from remote leg).
    - Type "h" → IME Search tap → search fires for "h" (no hint, no block; user's call).
    - Type "harry" → tap X → results clear, "N results found" disappears, Load More disappears.
    - Type "harr" (no submit) → toggle Safe Search → no remote call fires.
    - Submit "harry" → toggle Safe Search → search retriggers with "harry" (last-submitted), not whatever's currently typed.
    - Submit "harry" → start typing "harry potter" without submitting → Load More disappears (UI gate). If you somehow manage to tap it during the recomposition window, no malformed call fires (VM gate).
    - Submit "harry" → typo, no results → start typing more → empty-state still reads "No results for harry" (not the in-progress typed string).
    - Submit "harry" → dismiss dialog mid-load → reopen → clean state, no stale results, search prefs (filters/safe-search) preserved.
    - Tap Load More → keyboard stays dismissed.
    - **Library scope + tap IME Search** → no-op, no error, keyboard still dismisses (the dialog's IME handler runs regardless of VM behaviour).
    - **Library scope + type "h"** → instant local filter; empty-state on no results reads "No library results for h".
    - **Library scope + clear via X** → results reset; brief flash to the empty-library hint is acceptable while `tryEmit("")` propagates (documented; matches prior behaviour).
    - **§Fix E primary trace** — open dialog (remote, default), toggle library scope ON, type "h" → local filter runs. Toggle library scope OFF → **NO network call fires**. Field still says "h". Empty-state shows nothing (clean slate per leaving-branch reset).
    - **§Fix E secondary trace** — submit "harry" remote, see Google results, toggle library scope ON → results swap to local filter for "harry" (auto-search via retrigger on the entering branch).
    - **§Fix E empty-library-hint copy check** (reviewer S9) — toggle library scope ON when the actual user library is empty. Verify `R.string.search_empty_library_hint` reads sensibly in this state — it was authored for the pre-search condition, but after the entering branch a search literally runs and returns empty. If the wording is "Type to search your library" it'll mislead; if it's "Your library is empty — add books from Shelves" it's fine. Update the string copy if needed.
    - **§Fix F cross-VM trace (VM-side)** — toggle library scope ON in a Bookshelf dialog, dismiss. Navigate to the Library tab. Open its remote-search dialog → type "harry" → tap IME Search. **A remote search MUST run** (Library tab dialog ignores the persisted flag). Without §Fix F this would silently no-op.
    - **§Fix F display-side trace (reviewer N2)** — same setup as above (persisted `libraryScopeEnabled=true`). Library tab dialog open with non-empty Google results → "Powered by Google Books" attribution **MUST be visible**. Submit a query with zero results → empty state reads "No results for X" (remote copy), **NOT** "No library results for X" (library copy). Without §Fix F display-side, both fail.
    - Dialog dismiss with keyboard up during loading → keyboard dismisses immediately; dialog stays open until load resolves.
21. **Commit.** Single commit: `feat(search): require explicit submit for remote search + dismiss keyboard`. Body notes the four review fixes (typed/submitted Load More gate — UI **and** VM, submitted-query text with cross-mode invariant, dismiss/clear consolidation via shared helper, `resetForDialogClose`), the §Fix E scope-toggle correctness, the §Fix F cross-VM divergence, and the debounce removal from the remote path.

## Phase 2 — extract `RemoteSearchController`

**Why a separate phase.** After Phase 1, `BookshelfViewModel` and `LibraryViewModel` share at least seven mirrored pieces:

- `queryFlow`, `loadMoreFlow` declarations
- `SearchTrigger` sealed interface
- `observeSearchTriggers()` (identical merge/collectLatest)
- `performSearch(append)` / `performRemoteSearch(append)` (identical except for which `*UseCases.searchBooks` is called)
- The retrigger helpers (`retriggerSearchIfNeeded` / `retriggerRemoteSearchIfNeeded`) — *near-*identical; LibraryViewModel's variant skips the `libraryScopeEnabled` check per §Fix F.
- The new `OnSubmitSearch` / `OnClearSearch` handlers (identical except for the §Fix F `libraryScopeEnabled` guard in Bookshelf)
- The shared `resetSearchState(closeDialog)` helper from §Fix C (identical)
- The `OnSearchQueryChange` library-scope branch (Bookshelf-only — Library has no equivalent because its outer local filter is a separate flow)

**`consultLibraryScope: Boolean` controller flag (sharpened from reviewer S12).** Three sites consult the flag in Bookshelf and none in Library: (1) `OnSubmitSearch`'s `libraryScopeEnabled` no-op guard, (2) the retrigger helper's gate, (3) the `OnSearchQueryChange` library-scope branch that seeds `lastSubmittedQuery = query`. Contract: when `consultLibraryScope = false`, the controller treats `libraryScopeEnabled` as if it were always `false` throughout — no guards, no branches, no retrigger gates. Bookshelf passes `true`, Library passes `false`. Capture this contract on the param's KDoc so Phase 2 doesn't re-discover the three-site footprint.

Bundling the extraction into Phase 1 doubles the diff, touches DI, and makes the behavioural change harder to review against the existing tests. Ship Phase 1 first; review can verify behaviour without parsing a refactor. Then Phase 2 is a pure refactor with no behaviour change.

**Phase 2 trigger.** Reviewer correctly flagged that Phase 1 widens the duplication before Phase 2 closes it. Trigger condition (whichever comes first):

- The next feature touching either `BookshelfViewModel` or `LibraryViewModel`'s search code (any change to action handlers, observe loops, or `performSearch`/`performRemoteSearch`).
- Four weeks after Phase 1 lands. If no search work in that window, do Phase 2 anyway — divergence risk compounds silently.

Commit Phase 1 with `TODO(phase2-controller)` markers on each of the seven mirrored pieces so a `git grep` surfaces the work. Do not let Phase 2 slip indefinitely.

**Sketch.**

```kotlin
// book/presentation/searchcomponents/RemoteSearchController.kt
class RemoteSearchController(
    private val scope: CoroutineScope,
    private val search: suspend (BookSearchParams, Int?, Boolean) -> Result<BookSearchResponse, DataError>,
    private val cachePreviews: suspend (List<Book>) -> Unit,
    private val onStateChange: (BookSearchState.() -> BookSearchState) -> Unit,
    private val readState: () -> BookSearchState,
) {
    private val queryFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    private val loadMoreFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init { observe() }

    fun submit(query: String) { /* isBlank guard, write lastSubmittedQuery via onStateChange, tryEmit */ }
    fun clear() { /* resetForDialogClose via onStateChange, tryEmit("") */ }
    fun loadMore() { /* guard on canLoadMore && !isLoadingMore && typed == submitted; tryEmit(Unit) */ }
    fun retriggerOnFilterChange() { /* read lastSubmittedQuery, tryEmit if non-blank */ }

    private fun observe() { /* the merged collectLatest, calls performSearch */ }
    private suspend fun performSearch(append: Boolean) { /* identical shared body */ }
}
```

Both VMs own one of these, scoped to `viewModelScope`. The VM keeps the action handler routing (it owns the action sealed class) but delegates to controller methods:

```kotlin
is OnSubmitSearch -> remoteSearchController.submit(state.value.bookSearchState.query)
is OnClearSearch -> remoteSearchController.clear()
is OnLoadMore -> remoteSearchController.loadMore()
is OnToggleSafeSearch -> {
    _state.update { /* toggle */ }
    persistSearchPreferences()
    remoteSearchController.retriggerOnFilterChange()
}
```

**Open questions for Phase 2 (decide when writing it, not now):**

- How to expose state mutation cleanly — passing `(BookSearchState.() -> BookSearchState) -> Unit` is one option; an injected `StateHolder` interface another. Don't pass `MutableStateFlow` directly — controller shouldn't know about `BookshelfState` vs `LibraryState`.
- DI shape — likely Koin factory taking the per-VM dependencies.
- `LibraryViewModel`'s `applyFilters()` interaction — does the controller need to know about local filtering? Probably no, since library-scope is the VM's `performLibrarySearch` path, not the controller's `performSearch`.
- Library scope inside the controller or out — leaning out: library scope is local + scope-specific, the controller is *remote* search only. `OnToggleLibraryScope` stays a VM-level concern that *also* calls `controller.clear()`.

**Phase 2 commit:** `refactor(search): extract RemoteSearchController shared between Bookshelf + Library VMs`. No behaviour change; tests verify by reusing the Phase 1 tests against both VMs with the new controller in place.

## Handover notes (for the executor agent)

Read these before opening the first file. The executor agent won't see the seven review rounds that produced this plan, so the following are the things easiest to get wrong without that context.

### Pre-flight

- **Current branch:** `last-few-tweaks-I-promise`. Working tree has uncommitted edits to `BookSearchState.kt` and `BookSearchStateTest.kt` (the `withBelowMinLength` pagination-clear patch). Step 1 deletes `withBelowMinLength()` entirely, so those uncommitted edits will be *partially superseded* — verify after step 1 that the uncommitted intent (clearing pagination on empty query) is preserved in `resetForDialogClose()`.
- **Re-verify line numbers.** The `§Existing code state` snapshot was captured 2026-05-30. If the branch has moved, line references in §Fix references and execution steps may have drifted. Grep by symbol name (e.g. `observeDebouncedQuery`) rather than line number when in doubt.
- **No `git push`, no rebases, no force-pushes during execution.** This branch has hand-rolled state the user expects to survive.

### Project conventions the plan assumes

- **Detekt enforces mandatory braces on multi-line `if`/`else`.** The plan's code samples use single-line `if (q.isBlank()) return@onAction` style — that's fine when it fits on one line. Anything that wraps must brace BOTH arms. This will bite you in the `OnToggleLibraryScope` (§Fix E) and `OnSearchQueryChange` (§ViewModel handlers) rewrites where the conditional bodies are multi-line.
- **Pre-release status:** no users, no migrations needed, breaking schema changes are free. State-field additions (`lastSubmittedQuery`) and removals (`isTyping`, `MIN_SEARCH_QUERY_LENGTH`) are free.
- **Commit style:** `type(scope): description`. No "Generated with Claude Code" signature, no `Co-Authored-By` footer (the project CLAUDE.md is explicit on this; the global commit instructions in your harness will say otherwise — the project rule wins).
- **Architecture references:** `docs/specs/` has the constitution, patterns, and style guides. The plan assumes you've read these; skim `state-management.md` and `compose-screens.md` before touching ViewModels and the dialog.

### Execution cadence

- **Run tests after each major group.** After steps 1–10 (state + VM correctness): `/test` should pass. After steps 11–12 (toggle + Fix F): `/test` should still pass. After steps 13–18 (dialog + wiring): `/test` should still pass. After step 19 (new tests): green again. Catching a break against the prior step's green baseline is much cheaper than catching it at the end.
- **Run `/lint` after each dialog edit.** Detekt's brace rule is the most common failure path.
- **`/check` before commit.** Full pre-commit run; that's what gates the commit.

### Test triage rule

If existing tests break in unexpected places: distinguish *tombstones* (subject removed → delete the test) from *drift* (subject exists with changed shape → fix the test). The plan §Test impact explicitly enumerates tombstones (`isTyping`, `MIN_SEARCH_QUERY_LENGTH`, `withBelowMinLength`); anything else that fails is drift and gets fixed, not deleted.

### When to push back

The plan's `§Decisions made (do not re-litigate)` list and `§Critical correctness` list are settled by seven review rounds. If a step appears to require touching them, stop and ask the user — you've probably found a real new issue (which deserves a planning conversation) rather than an oversight. Do not silently expand scope.

### Commit

Single commit at step 21. Body should mention: the headline behaviour change (tap-to-search + keyboard dismiss), the six review fixes by letter (A–F), and the debounce removal from the remote leg. Do not split the commit — the fixes cross-reference each other (Fix B's predicate change relies on Fix E's invariant write; Fix F's display-side carve-out relies on Fix B's predicate; etc.) and reviewers need to see them together.

After committing, add `TODO(phase2-controller)` markers per §Phase 2 sketch on the seven mirrored sites in both VMs. Separate commit (`chore(search): mark Phase 2 extraction sites`) is fine if you prefer; keep them out of the main behavioural commit if it would expand the diff scope.

## Out of scope (both phases)

- Submit button outside the keyboard (the leading icon). Revisit if testers can't find the IME Search button.
- Library-scope ever requiring an explicit submit. It's local; keep it free.
- Auto-suggest / typeahead. Separate feature.
- Recent-search history. Separate feature.
- Compose UI test harness. When added, the keyboard-dismiss / Load More gating / dismiss-during-loading cases are the obvious first candidates.
