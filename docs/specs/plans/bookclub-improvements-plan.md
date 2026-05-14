# Book Club Improvements Plan

Four cohesive book club fixes targeting the `bc-polish` branch. All touch related code paths (bookcase state, club operations, navigation) and ship together.

**Assumption:** Pre-production, clean database. No migration of existing user data required. This simplifies the sign-out change — no need to handle legacy state from users who signed out under the old cleanup behaviour.

## 1. Preserve Club Shelves on Sign-Out

### Problem
`SignOutUseCaseImpl` calls `clearAllMemberships()` then `deleteClubShelves()`. This wipes all club data from local DB, making the guest-mode club shelf views (added in bc-polish) unreachable.

### Root Cause
Sign-out was implemented before bottom-nav split gave clubs their own tab. The old assumption was "signed out = no clubs". The constitution says "Guest mode: Full bookshelf experience without sign-in" — clubs should survive sign-out.

### Fix

**`SignOutUseCaseImpl`** — Remove `clubOperations.clearAllMemberships()` and `bookcaseRepository.deleteClubShelves(userId)` calls. Keep only Firebase sign-out + auth state update. The methods themselves stay (account deletion still needs them).

**`ShelfDao.getShelvesForUser()`** — Current query:
```sql
WHERE (ownerId IS NULL OR ownerId = :userId OR ownerId = '__system_tutorial__')
```
After sign-out, userId becomes null. Club shelves have a real ownerId, so they vanish. Update to:
```sql
WHERE (ownerId IS NULL OR ownerId = :userId OR ownerId = '__system_tutorial__' OR isBookClub = 1)
```
This ensures club shelves always appear regardless of auth state.

### Files to Change
| File | Change |
|------|--------|
| `auth/domain/usecase/SignOutUseCaseImpl.kt` | Remove lines 39-49 (membership clearing + shelf deletion) |
| `core/data/database/dao/ShelfDao.kt` | Add `OR isBookClub = 1` to `getShelvesForUser()` query |

### Guest Access Model

After this change, guests (signed-out users) can:
- **See** all club shelves that were on the device at sign-out
- **Browse** club shelf contents (books, reviews) in read-only mode

Guests **cannot:**
- **Create** clubs — gated by `isSignedIn` check + `SignInRequiredDialog`
- **Join** clubs — gated by `isSignedIn` check + `SignInRequiredDialog`
- **Leave/delete** clubs — gated by `isSignedIn` check
- **Submit reviews/ratings** — `upsertBookReview()` returns `NOT_SIGNED_IN`
- **Add/remove books** — club write operations require auth

All mutation guards are already in place from bc-polish. This change only makes the shelves *visible*.

### Edge Cases & Analysis

**Room null parameter handling (verified):** After sign-out, `CurrentUserProviderImpl.getCurrentUserId()` returns Kotlin `null` (not empty string). `BookcaseRepositoryImpl.getAllShelves()` passes this null directly to `dao.getShelvesForUser(userId)`. Room substitutes SQL `NULL` for the parameter. `ownerId = NULL` evaluates to `UNKNOWN` (always false in SQL), which is the desired behaviour — it prevents matching any shelves by ownerId when signed out. The `ownerId IS NULL` clause still correctly matches personal shelves. The `OR isBookClub = 1` addition ensures club shelves appear regardless.

**Known limitation — multi-user data leak:** User A signs out, user B signs in. User B sees user A's club shelves until `validateMemberships()` runs on `BookcaseViewModel` init (line 51) and cleans up stale clubs. If user B has no network, `validateMemberships()` fails silently and user A's club shelves persist indefinitely. On a shared/family device this means one user's club data is visible to another.

Accepted risk for pre-production. Before launch, consider scoping the query to the last-signed-in userId instead of blanket `OR isBookClub = 1`.

```kotlin
// TODO: Multi-user data leak — OR isBookClub = 1 shows all club shelves regardless
// of owner. On shared devices with no network, stale clubs from a previous user
// persist until validateMemberships() succeeds. Consider scoping to last-signed-in
// userId before production launch.
```

**Club deleted remotely while signed out:** Local shelf persists. On next sign-in, `validateMemberships()` detects the deletion via Firestore lookup and converts the shelf to a personal one, notifying the user.

**validateMemberships() requires sign-in:** The use case returns empty result if not signed in (line 23-29 of impl). This means stale shelves won't be cleaned up while in guest mode — they'll persist until next sign-in. This is fine: guest users see read-only club data, and cleanup happens on sign-in.

**Security:** Club content (book lists, reviews) persists on device after sign-out. Per constitution, this is shared data the user actively joined. No sensitive personal data is exposed. Account deletion still performs full cleanup via `deleteClubShelves()`.

**DRY:** `clearAllMemberships()` and `deleteClubShelves()` remain as methods — used by `DeleteAccountUseCaseImpl`. No duplication.

**SRP:** `SignOutUseCaseImpl` becomes simpler (auth concerns only), while data cleanup stays in `DeleteAccountUseCaseImpl` where it belongs. This actually improves SRP — sign-out shouldn't be responsible for data cleanup.

---

## 2. Club Creation Loading Indicator

### Problem
When creating a club via `OnCreateBookClubDirect`, the handler closes the dialog immediately (sets `showCreateBookClubDialog = false` on line 93 of `BookcaseClubActionHandler`) then performs the async creation. The user sees nothing between dialog close and shelf appearance.

### Root Cause
The `AddBookshelfDialog` already has a working loading state (`isLoading` prop shows `LinearProgressIndicator` + `CircularProgressIndicator` replacing the confirm button). But the dialog is dismissed before the loading can be seen.

### Fix

**`BookcaseClubActionHandler`** line 93 — Remove `showCreateBookClubDialog = false` from the state update when `OnCreateBookClubDirect` starts. Keep only `isCreatingBookClub = true`. The dialog stays open and shows its built-in loading indicator.

**`BookcaseClubActionHandler.handleCreateResult()`** — Add `showCreateBookClubDialog = false` to the success state update (line 112) and the limit-reached error branch (line 123). Keep dialog open on other errors — see error handling below.

### Files to Change
| File | Change |
|------|--------|
| `bookcase/presentation/handlers/BookcaseClubActionHandler.kt` | Remove `showCreateBookClubDialog = false` from line 93; add it to `handleCreateResult()` success + limit-reached branches |

### Edge Cases & Analysis

**Dialog dismiss during creation:** Already handled — `AddBookshelfDialog` line 47: `if (!isLoading) onDismiss()`. User can't dismiss while `isCreatingBookClub = true`.

**Error handling (resolved):** Two error paths exist in `handleCreateResult()`:
1. **`MAX_BOOK_CLUBS_REACHED`** (line 121-124): Close dialog, show dedicated limit dialog. User sees the limit dialog immediately.
2. **Other errors** (line 126-134): Currently sets `errorMessage` which triggers a snackbar on the main screen (line 222-226 of `BookcaseScreen.kt`). But closing the dialog and showing a snackbar is jarring.

**Decision:** Keep dialog open on non-limit errors. Set `isCreatingBookClub = false` (re-enables inputs) but do NOT set `showCreateBookClubDialog = false`. The `errorMessage` still sets for the snackbar, but the user also sees the dialog is still open and can retry or dismiss manually. This requires adding `errorMessage` display to `AddBookshelfDialog` — or simply relying on the snackbar appearing behind the open dialog. Either approach works; the key is the dialog stays open so the user isn't left wondering what happened.

**OnCreateBookClub (from shelf) path:** Line 79 doesn't touch `showCreateBookClubDialog` — unaffected. This path creates from an existing shelf and the dialog isn't open.

**Simpler alternative considered:** Showing a full-screen loading overlay on the bookcase. Rejected — reusing the existing dialog loading state is simpler, requires fewer changes, and the dialog already has the UX built.

---

## 3. User Display in Reviews

### Problem
The plan asks to verify how users are displayed in reviews and check nickname handling.

### Current Behaviour (Verified)

| Layer | Source | Value |
|-------|--------|-------|
| `GoogleAuthUiClient` | `FirebaseUser.displayName` | Google account display name |
| `UserData.username` | Mapped from above | Google display name (nullable) |
| `BookClubReviewRepositoryImpl.upsertBookReview()` | `user.username ?: "Anonymous"` | Falls back to "Anonymous" |
| `ClubReviewsCard > ReviewItem` | `review.displayName.ifBlank { "Anonymous" }` | Falls back again for blank strings |

### Assessment

The display chain is correct and has two layers of fallback:
1. **Null username** → "Anonymous" at repository level (line 70 of `BookClubReviewRepositoryImpl`)
2. **Blank displayName** → "Anonymous" at UI level (line 164 of `ClubReviewsCard`)

There is no nickname system — the app uses the Google account display name directly. This is appropriate: adding a separate nickname system would add complexity with no clear user need.

### Potential Issue: Stale Display Names

When a user updates their Google display name, existing reviews retain the old name (written to Firestore at review creation time). This is a known trade-off of denormalized data in Firestore and is acceptable for an app of this scale — fixing it would require either:
- Cloud Function trigger on profile changes (over-engineering)
- Joining against a user profile collection on every read (performance cost)

### Action: No code changes needed. Mark as verified.

---

## 4. Cross-Tab Navigation After Operations

### Problem
Two tab-switching flows are broken since the bottom-nav split:
1. **Club creation** → should switch to Book Clubs tab (flag set on line 116 of handler, never observed)
2. **Shelf duplication** → should switch to My Shelves tab when duplicating a club shelf (flag set on line 302 of ViewModel, never observed)

### Root Cause
The old design used internal tab switching within a single screen. The bottom-nav split created separate routes (`bookcase` and `bookclubs`) with separate ViewModel instances. The `switchToBookClubsTab` / `switchToPersonalTab` state flags are set but never consumed by any `LaunchedEffect` or navigation callback.

### Fix

**`BookcaseScreenRoot`** — Add two parameters:
```kotlin
onSwitchToBookClubs: () -> Unit = {},
onSwitchToPersonalTab: () -> Unit = {},
```

Add two `LaunchedEffect` blocks observing the state flags:
```kotlin
LaunchedEffect(state.switchToBookClubsTab) {
    if (state.switchToBookClubsTab) {
        onSwitchToBookClubs()
        viewModel.onAction(BookcaseAction.ResetSwitchToBookClubsTab)
    }
}

LaunchedEffect(state.switchToPersonalTab) {
    if (state.switchToPersonalTab) {
        onSwitchToPersonalTab()
        viewModel.onAction(BookcaseAction.ResetSwitchToPersonalTab)
    }
}
```

**`MyBookShelfApp.kt`** — Wire the callbacks on both routes. Navigation uses the same `popUpTo`/`saveState`/`restoreState`/`launchSingleTop` pattern as `MainScaffold`:
```kotlin
// On Bookcase route:
onSwitchToBookClubs = {
    navController.navigate(NavigationRoute.BookClubs.ROUTE) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// On BookClubs route:
onSwitchToPersonalTab = {
    navController.navigate(NavigationRoute.Bookcase.ROUTE) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

**Dead code cleanup** — Remove `NavigationRoute.Bookcase.ARG_SWITCH_TO_BOOK_CLUBS` (line 27) and its usage in `createRoute()` (line 29-30). Update `createRoute()` to only use `ARG_NEW_SHELF`.

### Files to Change
| File | Change |
|------|--------|
| `bookcase/presentation/BookcaseScreen.kt` | Add `onSwitchToBookClubs` and `onSwitchToPersonalTab` params + LaunchedEffects |
| `app/presentation/MyBookShelfApp.kt` | Wire callbacks on Bookcase route (line ~131) and BookClubs route (line ~167); use `switchToTab` extension |
| `app/presentation/MainScaffold.kt` | Replace inline navigate block with `switchToTab` extension |
| `app/NavigationRoute.kt` | Remove `ARG_SWITCH_TO_BOOK_CLUBS`, simplify `createRoute()` |
| `bookcase/presentation/handlers/BookcaseClubActionHandler.kt` | Move `switchToBookClubsTab = true` from `handleCreateResult()` to `createBookClub()` only |

### Edge Cases & Analysis

**Separate ViewModel instances per route:** Each route's `koinViewModel<BookcaseViewModel>()` creates its own instance (scoped to `NavBackStackEntry`). The flag is set and observed on the same route's ViewModel, so this works. The new route gets a fresh ViewModel that loads its own state.

**Race condition — rapid creation:** `LaunchedEffect` is keyed on the flag value. If the flag flips `true` → triggers effect → reset to `false`, a second rapid creation would flip it `true` again and trigger another effect. This is correct behaviour.

**OnCreateBookClubDirect guard:** `OnCreateBookClubDirect` fires from the BookClubs route FAB — the user is already on the Book Clubs tab. The handler currently sets `switchToBookClubsTab = true` for both creation paths, causing a wasted no-op navigation. **Action:** Remove `switchToBookClubsTab = true` from `handleCreateResult()` (line 116). Instead, set it only in `createBookClub()` (the from-shelf path on the Bookcase route). `createBookClubDirect()` does not set the flag — user is already on the correct tab.

**Route wiring (verified):** Both Bookcase (line 131 of `MyBookShelfApp.kt`) and BookClubs (line 167) routes use `BookcaseScreenRoot` with `koinViewModel<BookcaseViewModel>()`. Each route gets its own ViewModel instance scoped to its `NavBackStackEntry`. The `onSwitchToPersonalTab` callback wired on the BookClubs route observes that route's ViewModel — confirmed correct.

**switchToPersonalTab on BookClubs route:** `duplicateShelf()` sets `switchToPersonalTab = shelf.isBookClub`. Duplication of club shelves only makes sense on the BookClubs tab. The LaunchedEffect fires and navigates to My Shelves. Correct.

**DRY — extract `switchToTab` extension (action item):** Navigation logic in `MyBookShelfApp.kt` duplicates `MainScaffold`'s navigate pattern. Extract to a shared `NavController` extension in this PR:
```kotlin
private fun NavController.switchToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```
Use in both `MainScaffold` and the new callback wiring. Place in `app/presentation/` alongside `MainScaffold.kt`.

**SRP:** `BookcaseScreenRoot` gains navigation callbacks but doesn't perform navigation itself — it delegates upward. Clean separation maintained.

**State flag pattern note:** The `LaunchedEffect` → reset action → navigate pattern is standard Compose one-shot event handling and fine for two flags. If a third or fourth flag is added in future, consider migrating to `Channel`/`SharedFlow` for one-shot events to avoid the set/observe/reset dance scaling poorly.

---

## Implementation Order

1. **Item 1 (Sign-out)** — Independent, foundational for guest club visibility
2. **Item 4 (Cross-tab nav)** — Independent of item 1, but both affect bookcase
3. **Item 2 (Loading indicator)** — Small, self-contained change in handler
4. **Item 3 (Review display)** — Verified, no code changes

## Testing Strategy

| Item | Test Approach |
|------|---------------|
| 1. Sign-out preservation | Update `SignOutUseCaseImpl` tests: verify `clearAllMemberships` and `deleteClubShelves` are NOT called. Add integration test for `getShelvesForUser` with null userId + club shelves present. |
| 2. Loading indicator | **Unit test:** `BookcaseClubActionHandler` — verify `showCreateBookClubDialog` stays `true` when `OnCreateBookClubDirect` fires, becomes `false` only after `handleCreateResult()` success/limit. Stays `true` on other errors. **Manual:** visual confirmation of progress indicator in dialog. |
| 4. Cross-tab nav | **Unit test:** `BookcaseClubActionHandler` — verify `switchToBookClubsTab` is set on `createBookClub()` (from-shelf) but NOT on `createBookClubDirect()`. `BookcaseViewModel` — verify `switchToPersonalTab` is set when duplicating a club shelf. **Manual:** create club from shelf → lands on Book Clubs tab. Duplicate club shelf → lands on My Shelves tab. |

## Summary

| # | Item | Complexity | Code Changes |
|---|------|-----------|--------------|
| 1 | Preserve club shelves on sign-out | Low | 2 files, ~10 lines |
| 2 | Creation loading indicator | Low | 1 file, ~5 lines |
| 3 | User display in reviews | None | Verified, no changes |
| 4 | Cross-tab navigation | Medium | 5 files, ~35 lines |
