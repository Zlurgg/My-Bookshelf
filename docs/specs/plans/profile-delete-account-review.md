# Refactor: Profile → Account Screen + Simplified Deletion

## Context

The Profile screen and account deletion were implemented on `profile-page` branch. Two review passes found 17 issues. Rather than patching incrementally, we're refactoring:

1. **Move** `auth/presentation/profile/` → top-level `account/` feature package (matching `bookcase/`, `bookdetail/`, etc.)
2. **Fix** the screen to match our Root/Screen pattern and constitution
3. **Simplify** `DeleteAccountUseCase` — delete Firestore data + Firebase Auth, no local cleanup

### Why refactor instead of patch

- Profile was buried in `auth/` — but account management is a distinct feature, not authentication
- The Screen composable had side effects (re-auth `LaunchedEffect`), data layer imports (`GoogleCredentialFetcher`), and Root/Screen in a single file — all spec violations
- `DeleteAccountUseCase` was over-implemented: local cleanup is unnecessary (scoped by ownerId, removed on uninstall), `retryAfterReAuth` with precondition guard added complexity that isn't needed
- The incremental fix list (17 issues) touched the same files repeatedly — a clean rewrite is less churn

### What stays the same

- Club cleanup logic (cross-user data must be cleaned)
- Firestore batch deletion methods (`deleteAllRemoteData`, etc.)
- `REQUIRES_RECENT_LOGIN` handling
- Confirmation dialog before destructive action
- Navigation route and bookcase top bar profile icon

## Before You Start

Read these specs — the refactored code must comply:

| Spec | Key rule |
|------|----------|
| `docs/specs/constitution.md` | Presentation NEVER imports Data. Domain has NO Android imports. |
| `docs/specs/patterns/compose-screens.md` | Root/Screen in **separate files**. Side effects only in Root. Screen is pure. |
| `docs/specs/patterns/state-management.md` | Single StateFlow, sealed Action interface, `_state.update { it.copy(...) }` |
| `docs/specs/patterns/usecase.md` | Interface + Impl, one business operation |
| `docs/specs/style/code-style.md` | Naming, testing conventions |

Also read for reference:
- `bookdetail/` — cleanest example of a small top-level feature package (di/, domain/usecase/, presentation/)
- `auth/domain/usecase/SignOutUseCaseImpl.kt` — pattern for auth lifecycle use case

---

## New Package Structure

```
account/
├── di/
│   └── AccountModule.kt
├── domain/
│   └── usecase/
│       ├── DeleteAccountUseCase.kt          (interface)
│       └── DeleteAccountUseCaseImpl.kt       (simplified)
└── presentation/
    ├── AccountScreenRoot.kt                  (side effects, navigation, credential fetch)
    ├── AccountScreen.kt                      (pure UI — no LaunchedEffect, no injections)
    ├── AccountState.kt
    ├── AccountAction.kt
    ├── AccountViewModel.kt
    └── components/
        └── DeleteAccountConfirmDialog.kt
```

Dependencies flow: `account/` → `auth/` (domain: `AuthUseCases`; presentation: `CredentialFetcher`) → `book/` (domain: `ClubOperations`) → `sync/` (domain: `SyncRepository`). Same direction as existing features. The `CredentialFetcher` import is presentation-to-presentation cross-feature, not a layer violation.

---

## Key Design Decisions

### Deletion ordering: clubs → Firestore → auth

The `invoke()` flow is: cancel sync → delete clubs → delete Firestore data → delete Firebase Auth.

**Why this order:**
- Must query club memberships before deleting Firestore user data (memberships are stored in `users/{uid}/settings/preferences`)
- Club cleanup affects other users' data — must happen while we have the membership list
- Firestore deletion is user-only data — must fully succeed before the irreversible auth deletion

**Partial failure scenario:** If clubs succeed (step 3) but Firestore fails (step 4), the user's clubs are deleted but their account still exists. On retry: `getClubsCreatedByUser` returns empty (clubs gone), `getClubMembershipsForUser` still returns the list (preferences doc still exists), `removeUserFromClub` no-ops on already-cleaned clubs (idempotent), Firestore deletion retries. This is accepted — the user confirmed the destructive action, and the ordering is constrained by data dependencies.

### retryAfterReAuth: only re-auth + delete auth

`REQUIRES_RECENT_LOGIN` only fires from `authService.deleteAccount()` at step 5. By that point steps 1-4 have all succeeded — clubs are cleaned, Firestore data is deleted. So `retryAfterReAuth` only needs to: reauthenticate → delete auth. Two lines, no wasted network calls. KDoc documents the precondition: "Must only be called after invoke() returned REQUIRES_RECENT_LOGIN, meaning all remote data is already deleted."

### One-shot state fields use boolean flags + LaunchedEffect + reset action

This is the established pattern in the codebase — `BookcaseScreenRoot` uses it in at least 5 places (`navigateToSignIn`, `operationSuccess`, `tutorialShelfIdForNavigation`, `switchToPersonalTab`, `switchToBookClubsTab`). Channels/SharedFlow are not used anywhere for one-shot events. The double-fire risk is mitigated by the reset action being dispatched inside the `LaunchedEffect` — once reset, the condition is false and the effect won't re-trigger on recomposition.

### CredentialFetcher is a presentation-layer interface, not domain

`CredentialFetcher` depends on `android.app.Activity` — an Android framework class. The constitution says domain has "NO Android imports, NO UI, NO frameworks." So `CredentialFetcher` lives in `auth/presentation/service/`, not `auth/domain/service/`. This is correct — credential fetching is inherently platform-coupled. The implementation (`GoogleCredentialFetcher`) stays in `auth/data/service/`. Presentation depends on its own interface, data implements it.

### Guest users never reach the Account screen

Navigation handles this. `BookcaseScreenRoot` receives `onAccountClick: (isSignedIn: Boolean) -> Unit`. When tapped:
- Signed in → navigate to `Account` route
- Guest → navigate to `SignIn` route

The Account screen itself does not guard against guests. If a guest somehow reached it (impossible through normal navigation), `getSignedInUser()` returns null and the screen shows empty state. No delete/sign-out buttons appear because `isSignedIn = false`.

### ClubOperations grows to 17 methods

Adding `getClubsCreatedByUser`, `getClubMembershipsForUser`, `removeUserFromClub` takes the interface from 14 to 17 methods. The interface's KDoc already notes: "If the interface grows beyond ~20 methods, consider splitting by concern (e.g. ClubMembershipOps, ClubSyncOps)." We're close but under. These three methods fit "things the app needs from clubs" and splitting now would be premature. Noted for future reference.

---

## Implementation

### Step 1: Add ClubOperations methods (fixes Issue 3)

`DeleteAccountUseCaseImpl` currently imports `BookClubRemoteDataSource` (data layer) directly. Fix by adding the three methods to the existing domain interface.

**File:** `book/domain/service/ClubOperations.kt` — add:
```kotlin
suspend fun getClubsCreatedByUser(userId: String): Result<List<String>, DataError.Sync>
suspend fun getClubMembershipsForUser(userId: String): Result<List<String>, DataError.Sync>
suspend fun removeUserFromClub(clubCode: String, userId: String): Result<Unit, DataError.Sync>
```

**File:** `ClubOperationsImpl` — implement by delegating to `BookClubRemoteDataSource` (data layer stays internal to impl).

**File:** Test stubs/mocks for `ClubOperations` — add stubs for the new methods.

### Step 2: Extract CredentialFetcher interface (fixes Issue 15)

**New file:** `auth/presentation/service/CredentialFetcher.kt`
```kotlin
interface CredentialFetcher {
    suspend fun fetch(activity: Activity): Result<String, DataError.Local>
}
```

This is a presentation-layer interface (Activity dependency makes it platform-coupled). NOT domain.

**File:** `auth/data/service/GoogleCredentialFetcher.kt` — implement `CredentialFetcher` interface.

**File:** `auth/di/AuthModule.kt` — bind `GoogleCredentialFetcher` to `CredentialFetcher`.

### Step 3: Fix GoogleAuthUiClient.deleteAccount() (fixes Issue 11)

Wrap `credentialManager.clearCredentialState()` in its own try-catch after `user.delete().await()` succeeds. Account deletion is the point of no return — credential clearing is best-effort:

```kotlin
user.delete().await()
// Best-effort — account is already deleted, credential clearing is cleanup
try {
    credentialManager.clearCredentialState(ClearCredentialStateRequest())
} catch (e: Exception) {
    Timber.tag(TAG).w(e, "Credential state clear failed after account deletion")
}
```

### Step 4: Simplified DeleteAccountUseCase

**New file:** `account/domain/usecase/DeleteAccountUseCase.kt`
```kotlin
interface DeleteAccountUseCase {
    suspend operator fun invoke(): Result<Unit, DataError>
    suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError>
}
```

**New file:** `account/domain/usecase/DeleteAccountUseCaseImpl.kt`

**Dependencies** (5, down from 8):
- `CurrentUserProvider` — verify user is signed in
- `SyncSchedulerService` — cancel sync
- `SyncRepository` — delete Firestore data
- `ClubOperations` — club cleanup (domain interface, no data layer import)
- `AuthService` — delete Firebase Auth account + reauthenticate

**Removed dependencies:**
- ~~`ClearUserDataUseCase`~~ — local data scoped by ownerId, invisible to other users, removed on uninstall
- ~~`AuthStateRepository`~~ — no need to set signed-in state, app navigates to sign-in screen
- ~~`BookClubRemoteDataSource`~~ — replaced by `ClubOperations` (domain interface)

**`invoke()` flow:**
1. Verify user is signed in (fail fast)
2. Cancel sync (`syncScheduler.cancelAllSync()` — no wait, just cancel)
3. Clean up clubs via `ClubOperations`:
   - `getClubsCreatedByUser(userId)` → `deleteBookClub(code)` for each
   - `getClubMembershipsForUser(userId)` → subtract created club codes (fixes Issue 1) → `removeUserFromClub(code, userId)` for remaining
4. Delete Firestore data (`syncRepository.deleteAllRemoteData(userId)`) — must succeed before proceeding
5. Delete Firebase Auth account (`authService.deleteAccount()`) — on `REQUIRES_RECENT_LOGIN`, return error to ViewModel

**`retryAfterReAuth(idToken)` flow:**

`REQUIRES_RECENT_LOGIN` only fires at step 5. By that point steps 1-4 have succeeded. So:
```kotlin
/**
 * Retries account deletion after re-authentication.
 * Must only be called after [invoke] returned [DataError.Local.REQUIRES_RECENT_LOGIN],
 * meaning clubs are cleaned and all remote data is already deleted.
 */
override suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError> {
    val reAuthResult = authService.reauthenticate(idToken)
    if (reAuthResult is Result.Error) return reAuthResult
    return authService.deleteAccount()
}
```

No club cleanup, no Firestore deletion, no precondition guard. Two operations.

**Removed:**
- ~~`cleanUpLocalData()`~~ — unnecessary
- ~~`waitForSyncIdle()`~~ — unnecessary complexity, just cancel
- ~~Precondition guard / `hasRemoteData` check~~ — unnecessary, retryAfterReAuth has a clear precondition documented via KDoc

### Step 5: AccountState + AccountAction

**New file:** `account/presentation/AccountState.kt`
```kotlin
data class AccountState(
    val userEmail: String? = null,
    val userName: String? = null,
    val profilePictureUrl: String? = null,
    val isSignedIn: Boolean = false,
    val showSignOutDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val requestReAuth: Boolean = false,       // one-shot, Root observes — triggers Google picker
    val navigateToSignIn: Boolean = false,     // one-shot
    val errorMessage: String? = null,
)
```

**New file:** `account/presentation/AccountAction.kt`
```kotlin
sealed interface AccountAction {
    data object ShowSignOutDialog : AccountAction
    data object DismissSignOutDialog : AccountAction
    data object ConfirmSignOut : AccountAction
    data object RequestDeleteAccount : AccountAction
    data object DismissDeleteConfirm : AccountAction
    data object ConfirmDeleteAccount : AccountAction
    data class OnReAuthCompleted(val idToken: String) : AccountAction
    data object OnReAuthFailed : AccountAction           // fixes Issue 5
    data object DismissError : AccountAction
    data object ResetNavigation : AccountAction
    data object ResetReAuth : AccountAction
}
```

Changes from ProfileState/Action:
- `showReAuthDialog` → `requestReAuth` (one-shot observed by Root, not a dialog — fixes Issue 4)
- Added `OnReAuthFailed` action (fixes Issue 5)
- Added `ResetReAuth` action

### Step 6: AccountViewModel

**New file:** `account/presentation/AccountViewModel.kt`

**Dependencies:**
- `AuthUseCases` — for `getSignedInUser()` and `signOut()`
- `DeleteAccountUseCase` — for deletion (injected directly, not via `AuthUseCases`)

```kotlin
class AccountViewModel(
    private val authUseCases: AuthUseCases,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : ViewModel()
```

Key differences from ProfileViewModel:
- `deleteAccount()` has `isDeleting` guard (fixes Issue 12):
  ```kotlin
  private fun deleteAccount() {
      if (_state.value.isDeleting) return
      viewModelScope.launch { ... }
  }
  ```
- `handleDeleteError` sets `requestReAuth = true` (not `showReAuthDialog`) for `REQUIRES_RECENT_LOGIN`
- `OnReAuthFailed` sets `errorMessage` instead of silently dismissing

### Step 7: AccountScreenRoot (separate file)

**New file:** `account/presentation/AccountScreenRoot.kt`

**All side effects live here:**
- `LaunchedEffect(state.navigateToSignIn)` → navigate + reset
- `LaunchedEffect(state.errorMessage)` → show snackbar + dismiss
- `LaunchedEffect(state.requestReAuth)` → fetch credential → dispatch `OnReAuthCompleted` or `OnReAuthFailed` + reset (fixes Issues 4, 5)

**Credential fetching** — injects `CredentialFetcher` (presentation-layer interface, not `GoogleCredentialFetcher`):
```kotlin
@Composable
fun AccountScreenRoot(
    viewModel: AccountViewModel = koinViewModel(),
    credentialFetcher: CredentialFetcher = koinInject(),
    onNavigateToSignIn: () -> Unit,
    onBack: () -> Unit,
)
```

No data layer imports. Re-auth triggers the Google picker directly from Root's `LaunchedEffect` — no intermediate dialog (fixes Issue 4).

### Step 8: AccountScreen (separate file, pure)

**New file:** `account/presentation/AccountScreen.kt`

Pure composable — receives state and callbacks, zero side effects:
```kotlin
@Composable
fun AccountScreen(
    state: AccountState,
    snackbarHostState: SnackbarHostState,
    onAction: (AccountAction) -> Unit,
    onBack: () -> Unit,
)
```

Contains:
- TopAppBar with back button
- Profile header (avatar, name, email)
- Sign out button (outlined)
- Delete account button (error color, `DeleteForever` icon)
- Sign out confirmation dialog (pure — just state-driven visibility)
- Delete confirmation dialog (`DeleteAccountConfirmDialog`)
- Deleting progress dialog (non-dismissible)

**No `LaunchedEffect` in this composable.** No credential fetching. No snackbar triggering. Pure rendering.

Preview at the bottom.

### Step 9: DeleteAccountConfirmDialog

**New file:** `account/presentation/components/DeleteAccountConfirmDialog.kt`

Same as current implementation — move from `auth/presentation/profile/components/`.

### Step 10: AccountModule DI

**New file:** `account/di/AccountModule.kt`
```kotlin
val accountModule = module {
    singleOf(::DeleteAccountUseCaseImpl).bind<DeleteAccountUseCase>()
    viewModel { AccountViewModel(get(), get()) }
}
```

Register in app's module list.

### Step 11: Navigation + Bookcase cleanup

**File:** `app/NavigationRoute.kt` — rename `Profile` → `Account` (route string: `"account"`)

**File:** `app/presentation/MyBookShelfApp.kt` — update composable to use `AccountScreenRoot`

**File:** `bookcase/presentation/BookcaseScreen.kt`:
- Rename `onProfileClick` → `onAccountClick` in both Root and Screen (fixes Issue 7)
- Update content description on the `AccountCircle` icon button
- Update any related string resources

**File:** `bookcase/presentation/BookcaseScreenRoot.kt` (or wherever Root lives):
- Rename callback parameter to `onAccountClick: (Boolean) -> Unit`

### Step 12: Remove old profile code

Delete from `auth/`:
- `auth/presentation/profile/ProfileScreen.kt`
- `auth/presentation/profile/ProfileState.kt`
- `auth/presentation/profile/ProfileAction.kt`
- `auth/presentation/profile/ProfileViewModel.kt`
- `auth/presentation/profile/components/DeleteAccountConfirmDialog.kt`
- `auth/domain/usecase/DeleteAccountUseCase.kt`
- `auth/domain/usecase/DeleteAccountUseCaseImpl.kt`
- `auth/domain/usecase/GetSignedInUserUseCase.kt` — keep (still used by AccountViewModel via AuthUseCases)
- `auth/domain/usecase/GetSignedInUserUseCaseImpl.kt` — keep

**File:** `auth/domain/usecase/AuthUseCases.kt` — remove `deleteAccount` field. `DeleteAccountUseCase` is now in `account/` and injected directly into `AccountViewModel`, not via the aggregator.

**Verify:** No other ViewModel references `authUseCases.deleteAccount` before removing.

**File:** `auth/di/AuthModule.kt` — remove `DeleteAccountUseCaseImpl` and `ProfileViewModel` registrations.

### Step 13: Issue 10 — AuthUseCases over-injection tech debt

The constitution says: "Never describe a violation as 'acceptable' or 'consistent with existing patterns'." `BookcaseViewModel` now uses 2 of 5 use cases from `AuthUseCases`. Since we're already touching `AuthUseCases` (removing `deleteAccount`), add a tech-debt comment:

**File:** `bookcase/presentation/BookcaseViewModel.kt` — add comment at injection site:
```kotlin
// TODO: BookcaseViewModel only uses checkSignInStatus + getCurrentUserId from AuthUseCases.
// Inject the two individual use cases instead of the full aggregator.
```

### Step 14: Update tests

**New file:** `account/domain/usecase/DeleteAccountUseCaseTest.kt`
- Simplified — no local cleanup tests, no precondition guard test, no sync wait test
- Cover: full success, Firestore failure stops auth deletion, club cleanup failure stops cascade, `REQUIRES_RECENT_LOGIN`, `retryAfterReAuth` success, `retryAfterReAuth` re-auth failure, not signed in, club filtering (created clubs excluded from membership removal)
- Mock `ClubOperations` instead of `StubBookClubRemoteDataSource` (fixes Issue 3 test simplification)

**New file:** `account/presentation/AccountViewModelTest.kt`
- Same coverage as ProfileViewModelTest plus:
  - `OnReAuthFailed` sets error message (Issue 5)
  - Double-tap guard: `ConfirmDeleteAccount` while `isDeleting` is a no-op (Issues 12, 17)
  - Re-auth succeeds but `retryAfterReAuth` fails → shows error (Issue 16)

**Delete:**
- `auth/domain/usecase/DeleteAccountUseCaseTest.kt`
- `auth/presentation/profile/ProfileViewModelTest.kt`

**Update:**
- `testutil/mocks/MockAuthService.kt` — add `email` parameter to `configureSignedIn` with default `"test@example.com"` (Issue 8)

### Step 15: Remaining targeted fixes

**Issue 6** — `SyncRepositoryImpl.deleteAllRemoteData()`: add `firestore.collection("users").document(userId).delete().await()` at the end.

**Issue 2** — Document decision: comments persist when user leaves/deletes account. Standard social behavior. Add a comment in `removeUserFromClub` explaining this:
```kotlin
// Reviews are keyed by userId (direct delete). Comments use auto-generated IDs
// and are intentionally kept — removing them creates gaps in discussions.
// The user's attribution remains but the account is gone.
```

**Issue 9** — Resolved by rewrite (new `DeleteAccountUseCaseImpl` won't have the unnecessary suppressions).

---

## Issues Resolved by This Refactor

| Issue | How resolved |
|-------|-------------|
| 1. Membership list includes created clubs | Subtract in use case |
| 3. Domain imports data layer | `ClubOperations` domain interface |
| 4. Re-auth dialog flashes | Replaced with one-shot `requestReAuth` in Root |
| 5. Silent re-auth failure | `OnReAuthFailed` action sets error message |
| 7. `onProfileClick` name collision | Renamed to `onAccountClick` |
| 9. Unnecessary `@Suppress` | Clean rewrite |
| 10. Over-injection accepted as tech debt | Tech-debt TODO comment added (constitution compliant) |
| 11. Credential clear masks deletion | Try-catch in `GoogleAuthUiClient` |
| 12. No double-tap guard | `isDeleting` guard in ViewModel |
| 13. `retryAfterReAuth` recursion | Simplified — reauthenticate + deleteAccount only |
| 14. Local cleanup errors swallowed | Local cleanup removed entirely |
| 15. Data layer import in Screen | `CredentialFetcher` presentation-layer interface |
| 16. Missing retry-failure test | Added in new test file |
| 17. Missing concurrent deletion test | Added in new test file |

---

## Verification

- `./gradlew assembleDebug` passes
- `./gradlew detekt` passes
- `./gradlew test` passes (all existing + new tests)
- Guest: account icon → sign-in screen (never reaches Account screen)
- Signed in: account icon → account screen with email/name
- Sign out from account → sign-in screen
- Delete account → confirm → loading → sign-in screen
- Delete with stale session → Google picker → completion
- Delete with network failure → error shown, still signed in, retry works
- Delete as club creator → club deleted
- Firebase Console: user subcollections empty, Auth account gone
- No orphaned `auth/presentation/profile/` files remain
- No ProGuard/R8 issues with new `account/` package (verify in release build)
