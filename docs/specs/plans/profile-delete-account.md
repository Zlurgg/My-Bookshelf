# Plan: Profile Screen + Delete Account Flow

## Context

MyBookshelf is moving from GitHub portfolio to Play Store production. Play Store requires users to be able to delete their account and all associated data. Currently:
- Sign-in/out lives in the bookcase dropdown `SettingsMenu`
- Sign-out only clears **local** data — Firestore data and Firebase Auth account persist
- No profile screen, no account deletion capability
- The `delete-account.html` page has been updated to describe a manual request process as a stopgap

This plan adds a dedicated Profile screen with proper account deletion that cascades through Firestore, Firebase Auth, and local storage.

## Decisions

- **Profile UI**: New screen with `NavigationRoute.Profile`, accessed via profile icon in bookcase top bar
- **Auth flow**: Profile icon handles both states — navigates to sign-in when guest, profile screen when signed in. Sign-in/out removed from `SettingsMenu`
- **Firestore deletion**: Batch delete per collection (books, bookshelves, settings subcollections under `users/{uid}`). Book club cleanup handled separately by the use case
- **Book club creator deletion**: Deleting your account deletes any clubs you created (same as existing "delete book club" behavior — other members' local shelves convert to personal shelves). For clubs you're a member of, your membership/reviews/comments are removed
- **Partial failure strategy**: Firestore deletion must fully succeed before Firebase Auth deletion. If Firestore fails, the user is still authenticated and can retry. Auth deletion is the point of no return
- **Guest users**: Profile icon navigates to sign-in. Guests have no cloud data or account to delete — local data is removed by uninstalling the app. No delete-account flow for guests
- **DeleteAccountUseCase location**: Lives in `auth/` despite cross-cutting dependencies. This is the same pattern as `SignOutUseCaseImpl` which already depends on sync/, bookcase/, and core/. Intentional — auth orchestrates account lifecycle
- **Profile Root/Screen split**: Both composables in a single file (`ProfileScreen.kt`). Conscious deviation from the two-file pattern used in bookcase/bookshelf — justified because the profile screen is small (header + two buttons + dialogs). If the screen grows, split later
- **Idempotency**: Verified — Firestore `document.delete()` is a no-op on non-existent docs. `deleteBookClub()` queries subcollections first (returns empty if club gone), then deletes the doc (no-op). `FieldValue.arrayRemove` is idempotent. However, `removeClubMembership()` uses `.update()` which throws `NOT_FOUND` on a non-existent document — the new `removeUserFromClub()` must use `.set(data, SetOptions.merge())` instead of `.update()` to be idempotent on retry
- **getSignedInUser access**: `AuthService.getSignedInUser()` returns `UserData` (email/name/photo) but no use case wraps it today. `GetCurrentUserIdUseCase` only returns the UID. Plan adds a `GetSignedInUserUseCase` (synchronous, no business logic) to maintain the ViewModel → UseCase dependency rule

---

## Implementation Steps

### Step 1: Domain — Error Types & AuthService

**Files:**
- `core/domain/error/DataError.kt` — Add `REQUIRES_RECENT_LOGIN` to `DataError.Local`
- `auth/domain/service/AuthService.kt` — Add `deleteAccount()` and `reauthenticate(idToken: String)` methods

### Step 2: Domain — DeleteAccountUseCase

**New files:**
- `auth/domain/usecase/DeleteAccountUseCase.kt` (interface with two methods)
- `auth/domain/usecase/DeleteAccountUseCaseImpl.kt`

**Interface:**
```kotlin
interface DeleteAccountUseCase {
    suspend operator fun invoke(): Result<Unit, DataError>
    suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError>
}
```

**`invoke()` — Full deletion (steps 1-5):**
1. Verify user is signed in — fail fast if not
2. Wait for any active sync to complete (`observeSyncState()` → wait for idle with timeout), then cancel sync workers (`SyncSchedulerService.cancelAllSync()`)
3. Clean up book clubs (use case orchestrates this directly — see Step 4)
4. Delete user's Firestore data (`SyncRepository.deleteAllRemoteData(userId)`) — **must fully succeed before proceeding**. On failure, return error — user is still authenticated and can retry
5. Delete Firebase Auth account (`AuthService.deleteAccount()`) — point of no return. On `REQUIRES_RECENT_LOGIN`, return this error to the ViewModel
6. Clear local data (`ClearUserDataUseCase`) + sync metadata (`SyncRepository.clearSyncData(userId)`)

**`retryAfterReAuth(idToken)` — Re-auth retry (steps 5-6 only):**
Called by ViewModel after user re-authenticates. Calls `authService.reauthenticate(idToken)`, then executes steps 5-6 only (Auth deletion + local cleanup). Remote data is already gone — this method explicitly skips remote deletion rather than inferring state from Firestore queries.

**Precondition guard:** `retryAfterReAuth` performs a lightweight check before proceeding — verifies the user's Firestore document (`users/{uid}`) does not exist. If it does, falls back to `invoke()` to run the full sequence. One Firestore read is cheap insurance against a ViewModel bug calling this method prematurely. Method includes a KDoc comment explaining the precondition: "Must only be called after invoke() returned REQUIRES_RECENT_LOGIN, meaning remote data is already deleted."

**Return type:** `Result<Unit, DataError>` — the parent sealed interface, since the use case spans both `DataError.Sync` (Firestore) and `DataError.Local` (Auth, local DB) error domains.

### Step 3: Data — AuthService Implementation

**File:** `auth/domain/service/AuthService.kt` — Add interface methods
**File:** `auth/data/service/GoogleAuthUiClient.kt`

Add two methods:
- `deleteAccount()`: Gets `auth.currentUser`, null-checks with early return error, calls `user.delete().await()`, then clears credential state. Catches `FirebaseAuthRecentLoginRequiredException` → maps to `REQUIRES_RECENT_LOGIN`. No `!!` operator
- `reauthenticate(idToken)`: Gets `auth.currentUser`, null-checks, builds `GoogleAuthProvider.getCredential(idToken, null)`, calls `user.reauthenticate(credential).await()`

### Step 4: Data — Remote Data Deletion

**`SyncRepository.deleteAllRemoteData()` — User's own Firestore data only:**

**Files:**
- `sync/domain/repository/SyncRepository.kt` — Add `deleteAllRemoteData(userId: String): Result<Unit, DataError.Sync>`
- `sync/data/repository/SyncRepositoryImpl.kt` — Implement: deletes books, shelves, preferences subcollections under `users/{uid}`. Does NOT handle book club cleanup (that's the use case's job — see below)
- `sync/data/repository/BookSyncDataSource.kt` — Add `deleteAllBooks(userId: String): Result<Unit, DataError.Sync>`
- `sync/data/repository/ShelfSyncDataSource.kt` — Add `deleteAllBookshelves(userId: String): Result<Unit, DataError.Sync>`
- `sync/data/repository/UserPreferencesDataSource.kt` — Add `deleteUserPreferences(userId: String): Result<Unit, DataError.Sync>`
- Firestore implementations (`FirestoreBookSyncDataSourceImpl`, `FirestoreShelfSyncDataSourceImpl`, `FirestoreUserPreferencesDataSourceImpl`) — Implement batch deletion: query all docs in subcollection, batch delete in groups of 500

**Book club cleanup — orchestrated by `DeleteAccountUseCaseImpl` (not the repository):**

Club cleanup is business logic (deciding which clubs to delete vs. leave) and belongs in the use case, not the repository. Same pattern as `SignOutUseCaseImpl` which calls `ClearUserDataUseCase` → `ClubOperations.clearAllMemberships()`.

1. Query `BookClubRemoteDataSource.getClubsCreatedByUser(userId)` → call `deleteBookClub(code)` for each
2. Query `BookClubRemoteDataSource.getClubMembershipsForUser(userId)` → call `removeUserFromClub(code, userId)` for each
3. Clear local club memberships via `ClubOperations.clearAllMemberships()`

**New methods on `BookClubRemoteDataSource`:**
- `getClubsCreatedByUser(userId: String): Result<List<String>, DataError.Sync>` — returns club codes
- `getClubMembershipsForUser(userId: String): Result<List<String>, DataError.Sync>` — returns club codes where user is member but not creator
- `removeUserFromClub(clubCode: String, userId: String): Result<Unit, DataError.Sync>` — removes membership + user's reviews/comments

**Implementations:** `FirestoreBookClubRemoteDataSourceImpl`

### Step 5: Domain — GetSignedInUserUseCase

**New files:**
- `auth/domain/usecase/GetSignedInUserUseCase.kt` (interface)
- `auth/domain/usecase/GetSignedInUserUseCaseImpl.kt`

Wraps `AuthService.getSignedInUser()` → returns `UserData?`. Synchronous, no business logic — exists solely to maintain ViewModel → UseCase dependency rule. Added to `AuthUseCases`.

### Step 6: Presentation — Profile State & Actions

**New files in `auth/presentation/profile/`:**
- `ProfileState.kt`
- `ProfileAction.kt`
- `ProfileViewModel.kt`

**ProfileState:**
```kotlin
data class ProfileState(
    val userEmail: String? = null,
    val userName: String? = null,
    val profilePictureUrl: String? = null,
    val isSignedIn: Boolean = false,
    // Sign out
    val showSignOutDialog: Boolean = false,
    val navigateToSignIn: Boolean = false, // Unified one-shot flag for both sign-out and deletion
    // Delete account
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val showReAuthDialog: Boolean = false,
    val errorMessage: String? = null,
)
```

**One-shot navigation:** Single `navigateToSignIn: Boolean` field handles both sign-out and deletion completion. Reset via `ResetNavigation` action. Follows `compose-screens.md` LaunchedEffect + reset pattern.

**ProfileAction:** `ShowSignOutDialog`, `DismissSignOutDialog`, `ConfirmSignOut`, `RequestDeleteAccount`, `DismissDeleteConfirm`, `ConfirmDeleteAccount`, `OnReAuthCompleted(idToken: String)`, `DismissReAuth`, `DismissError`, `ResetNavigation`

**ProfileViewModel:**
- Init: Load current user from `AuthUseCases.getSignedInUser()` (via `GetSignedInUserUseCase`)
- Sign out: Reuse existing `SignOutUseCase`, set `navigateToSignIn = true`
- Delete account: Set `isDeleting = true`, call `DeleteAccountUseCase()`. On success → `navigateToSignIn = true`. On `REQUIRES_RECENT_LOGIN` → `showReAuthDialog = true`. On other error → `errorMessage`
- Re-auth: ViewModel receives credential from UI, calls `DeleteAccountUseCase.retryAfterReAuth(idToken)`. On success → `navigateToSignIn = true`. All through the use case — ViewModel never calls `AuthService` directly (respects constitution)

### Step 7: Presentation — Profile Screen UI

**New files in `auth/presentation/profile/`:**
- `ProfileScreen.kt` (contains `ProfileScreenRoot` + `ProfileScreen` — single file, see Decisions)

**Root composable:**
- Collects state from ViewModel
- `LaunchedEffect(state.navigateToSignIn)` → calls `onNavigateToSignIn()` + `viewModel.onAction(ResetNavigation)`
- Provides credential fetch lambda (Activity context) for re-auth
- Snackbar host for error messages

**Screen composable (pure, preview-friendly):**
- Profile header: avatar (or default `AccountCircle` icon), name, email
- "Sign Out" button (outlined style)
- Divider
- "Delete Account" button (error/destructive color, `DeleteForever` icon)
- Loading overlay when `isDeleting`

**Dialogs:**
- `DeleteAccountConfirmDialog` — Two-step: warns what gets deleted (cloud data, book clubs, reviews, Firebase account). Confirm button in destructive color reads "Delete My Account". Standard accessible confirmation, no typing required
- Loading overlay: When `isDeleting`, show non-dismissible dialog with indeterminate `CircularProgressIndicator` and "Deleting account..." text
- Re-auth dialog: Simple `AlertDialog` — "For security, please sign in again to confirm account deletion." Confirm triggers credential fetch → `OnReAuthCompleted`

**New file:** `auth/presentation/profile/components/DeleteAccountConfirmDialog.kt`

### Step 8: Navigation & Top Bar Changes

**Files:**
- `app/NavigationRoute.kt` — Add `Profile` route
- `app/presentation/MyBookShelfApp.kt` — Add `composable(Profile.ROUTE)` with `ProfileScreenRoot`, wire `onNavigateToSignIn` to navigate to SignIn (clearing backstack), wire `onBack` to `popBackStack()`
- `bookcase/presentation/BookcaseScreen.kt` — Add profile icon button in `TopAppBar` actions (before `SettingsMenu`). Uses `AccountCircle` icon. When tapped: callback `onProfileClick(isSignedIn)`. Remove `SignOutDialog` usage
- `bookcase/presentation/BookcaseScreenRoot.kt` — Add `onProfileClick` callback. Handle: if signed in → navigate to Profile; if guest → navigate to SignIn
- `bookcase/presentation/components/SettingsMenu.kt` — Remove `onSignIn`, `onSignOut`, `isSignedIn` params and all auth-related menu items. Keep Help, About, Join Book Club
- `bookcase/presentation/BookcaseAction.kt` — Remove `OnSignInClick`, `ShowSignOutDialog`, `DismissSignOutDialog`, `ConfirmSignOut`, `ResetNavigateToSignIn`
- `bookcase/presentation/BookcaseState.kt` — Remove `showSignOutDialog`, `signedOutSuccessfully`, `navigateToSignIn` fields
- `bookcase/presentation/BookcaseViewModel.kt` — Remove sign-out handling logic, remove `SignOutUseCase` dependency

### Step 9: DI Wiring

**File:** `auth/di/AuthModule.kt`
- Register `DeleteAccountUseCaseImpl` bound to `DeleteAccountUseCase`
- Register `GetSignedInUserUseCaseImpl` bound to `GetSignedInUserUseCase`
- Register `ProfileViewModel`
- Update `AuthUseCases` to include `deleteAccount` and `getSignedInUser`

### Step 10: Update AuthUseCases

**File:** `auth/domain/usecase/AuthUseCases.kt` — Add `deleteAccount: DeleteAccountUseCase` and `getSignedInUser: GetSignedInUserUseCase`

### Step 11: String Resources

**File:** `app/src/main/res/values/strings.xml` — Add strings for:
- Profile screen title ("Account")
- Delete account button, confirmation dialog title/body/confirm button
- Deletion in progress message
- Re-auth prompt
- Error messages

---

## Files Modified (Summary)

| File | Change |
|------|--------|
| `core/domain/error/DataError.kt` | Add `REQUIRES_RECENT_LOGIN` |
| `auth/domain/service/AuthService.kt` | Add `deleteAccount()`, `reauthenticate()` |
| `auth/data/service/GoogleAuthUiClient.kt` | Implement new methods (no `!!`) |
| `sync/domain/repository/SyncRepository.kt` | Add `deleteAllRemoteData()` |
| `sync/data/repository/SyncRepositoryImpl.kt` | Implement user-data-only remote deletion |
| `sync/data/repository/BookSyncDataSource.kt` | Add `deleteAllBooks()` |
| `sync/data/repository/ShelfSyncDataSource.kt` | Add `deleteAllBookshelves()` |
| `sync/data/repository/UserPreferencesDataSource.kt` | Add `deleteUserPreferences()` |
| `sync/data/repository/BookClubRemoteDataSource.kt` | Add discovery + removal methods |
| `sync/data/service/FirestoreBookSyncDataSourceImpl.kt` | Implement batch delete |
| `sync/data/service/FirestoreShelfSyncDataSourceImpl.kt` | Implement batch delete |
| `sync/data/service/FirestoreUserPreferencesDataSourceImpl.kt` | Implement delete |
| `sync/data/service/FirestoreBookClubRemoteDataSourceImpl.kt` | Implement new methods |
| `app/NavigationRoute.kt` | Add Profile route |
| `app/presentation/MyBookShelfApp.kt` | Add Profile composable + navigation |
| `bookcase/presentation/BookcaseScreen.kt` | Profile icon in top bar, remove SignOutDialog |
| `bookcase/presentation/BookcaseScreenRoot.kt` | Add `onProfileClick` callback |
| `bookcase/presentation/components/SettingsMenu.kt` | Remove auth items |
| `bookcase/presentation/BookcaseAction.kt` | Remove auth actions |
| `bookcase/presentation/BookcaseState.kt` | Remove auth state fields |
| `bookcase/presentation/BookcaseViewModel.kt` | Remove sign-out logic + dependency |
| `auth/di/AuthModule.kt` | Register new use case + ViewModel |
| `auth/domain/usecase/AuthUseCases.kt` | Add `deleteAccount`, `getSignedInUser` |
| `app/src/main/res/values/strings.xml` | New strings |
| `testutil/mocks/MockAuthService.kt` | Add `deleteAccount()`, `reauthenticate()` stubs |
| `testutil/mocks/MockSyncRepository.kt` | Add `deleteAllRemoteData()` stub |

## New Files

| File | Purpose |
|------|---------|
| `auth/domain/usecase/DeleteAccountUseCase.kt` | Interface (invoke + retryAfterReAuth) |
| `auth/domain/usecase/DeleteAccountUseCaseImpl.kt` | Orchestrates cascade deletion |
| `auth/domain/usecase/GetSignedInUserUseCase.kt` | Interface — wraps AuthService.getSignedInUser() |
| `auth/domain/usecase/GetSignedInUserUseCaseImpl.kt` | Implementation |
| `auth/presentation/profile/ProfileState.kt` | UI state |
| `auth/presentation/profile/ProfileAction.kt` | UI actions |
| `auth/presentation/profile/ProfileViewModel.kt` | State management |
| `auth/presentation/profile/ProfileScreen.kt` | Root + Screen composables (single file) |
| `auth/presentation/profile/components/DeleteAccountConfirmDialog.kt` | Two-step destructive confirmation |
| `auth/domain/usecase/DeleteAccountUseCaseTest.kt` | Use case tests |
| `auth/presentation/profile/ProfileViewModelTest.kt` | ViewModel tests |

## Removed from Original Plan

| Item | Reason |
|------|--------|
| `DeletionProgress.kt` sealed interface | Over-engineered for ~2s operation. Simple `isDeleting` boolean instead |
| `DeletionProgressDialog.kt` | Replaced with simple loading overlay |
| `ReauthenticateUseCase.kt` | ViewModel must not call AuthService directly (constitution). Re-auth folded into `DeleteAccountUseCase.retryAfterReAuth()` instead |
| `WelcomePreferences.clearForUser()` | Dead key after deletion — new UID on re-registration |
| `ACCOUNT_DELETION_FAILED` error type | Not needed — existing error types cover failure cases |
| Step 5 (sync guard in repository) | Duplicate — sync wait owned by use case in Step 2, not the repository |

---

## Testing

### New Test Files

| File | Covers |
|------|--------|
| `auth/domain/usecase/DeleteAccountUseCaseTest.kt` | Full success path; Firestore failure → verify Auth NOT deleted; club cleanup partial failure → verify Firestore user data NOT deleted, Auth NOT deleted; `REQUIRES_RECENT_LOGIN` returned; `retryAfterReAuth` success path; `retryAfterReAuth` re-auth failure; `retryAfterReAuth` precondition guard (remote data still exists → falls back to full invoke); sync wait timeout handling |
| `auth/presentation/profile/ProfileViewModelTest.kt` | Init loads user; sign-out sets `navigateToSignIn`; delete success sets `navigateToSignIn`; delete with re-auth → `showReAuthDialog` → retry → success; delete failure → `errorMessage`; error dismissal; `ResetNavigation` clears flag |

### Updated Test Files

| File | Change |
|------|--------|
| `bookcase/presentation/BookcaseViewModelTest.kt` | Remove tests for sign-out actions/state that moved to ProfileViewModel |
| `testutil/mocks/MockAuthService.kt` | Add `deleteAccount()`, `reauthenticate()` stubs |
| `testutil/mocks/MockSyncRepository.kt` | Add `deleteAllRemoteData()` stub |

### Manual Testing

1. Guest: profile icon → navigates to sign-in screen
2. Signed in: profile icon → profile screen shows email/name/photo
3. Sign out from profile → returns to sign-in screen
4. Delete account → confirm dialog → "Delete My Account" → loading → completion → sign-in screen
5. Delete account with stale session → re-auth dialog → re-sign-in → deletion completes
6. Delete account with network failure during Firestore deletion → error shown, user still signed in, can retry
7. Delete account as book club creator → club deleted, other members' shelves convert to personal
8. **Firestore verification**: After deletion, check Firebase Console — user's subcollections empty, Auth account gone
