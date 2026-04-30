# Handover: Profile → Account Refactor

## What This Is

Refactor the Profile screen into a top-level `account/` feature package, fix spec violations, and simplify `DeleteAccountUseCase`. The full rationale and design decisions are in `docs/specs/plans/profile-delete-account-review.md` — read it first. This document tells you how to execute it.

## Branch

Continue on `profile-page`.

## Before You Start

Read these specs — the refactored code must comply:

| Spec | Why |
|------|-----|
| `docs/specs/constitution.md` | Domain: no Android imports. Presentation: never imports Data. |
| `docs/specs/patterns/compose-screens.md` | Root/Screen in **separate files**. Side effects only in Root. |
| `docs/specs/patterns/state-management.md` | StateFlow, sealed Action, `_state.update { it.copy(...) }` |
| `docs/specs/patterns/usecase.md` | Interface + Impl pattern |

Read these existing files for reference patterns:

| File | Why |
|------|-----|
| `bookdetail/` package structure | Cleanest small top-level feature (di/, domain/usecase/, presentation/) |
| `bookdetail/di/BookDetailModule.kt` | DI module pattern |
| `bookcase/presentation/BookcaseScreen.kt:68-192` | Root/Screen in same file (BookcaseScreenRoot uses LaunchedEffect + reset for one-shot navigation) |
| `auth/domain/usecase/SignOutUseCaseImpl.kt` | Auth lifecycle use case pattern |
| `auth/data/service/GoogleCredentialFetcher.kt` | Credential fetch implementation to extract interface from |
| `auth/data/service/GoogleAuthUiClient.kt:84-103` | `deleteAccount()` method that needs the try-catch fix |

## Key Decisions (Already Reviewed — Don't Revisit)

- **No local cleanup on delete** — Room data is scoped by ownerId, invisible to other users, removed on uninstall
- **Deletion order**: cancel sync → clubs → Firestore → auth. Must query memberships before deleting Firestore (stored there). See review doc "Key Design Decisions" for partial failure analysis
- **`retryAfterReAuth`**: only reauthenticate + deleteAccount. By the time `REQUIRES_RECENT_LOGIN` fires, steps 1-4 have succeeded. KDoc precondition, not a runtime guard
- **`CredentialFetcher`**: presentation-layer interface (`auth/presentation/service/`), NOT domain. Activity dependency makes it platform-coupled
- **One-shot state fields**: boolean flags + LaunchedEffect + reset action — established pattern (5 examples in BookcaseScreenRoot)
- **Guest users**: navigation handles it. BookcaseScreenRoot routes guests to sign-in, never to account screen

## Implementation Order

Work in this order. Each step should compile. Commit after each phase.

---

### Phase 1: Infrastructure Changes (Steps 1-3)

These modify existing code. No new `account/` package yet. Existing tests should still pass.

**Step 1 — Add 3 methods to `ClubOperations` domain interface**

**File:** `book/domain/service/ClubOperations.kt` — add at the end, before the closing brace:
```kotlin
suspend fun getClubsCreatedByUser(userId: String): Result<List<String>, DataError.Sync>
suspend fun getClubMembershipsForUser(userId: String): Result<List<String>, DataError.Sync>
suspend fun removeUserFromClub(clubCode: String, userId: String): Result<Unit, DataError.Sync>
```

**File:** `bookclub/presentation/handlers/ClubOperationsImpl.kt` — add implementations. This class takes `BookClubOperationUseCases` and `BookClubRepository`. The new methods need `BookClubRemoteDataSource` (data layer), which `ClubOperationsImpl` doesn't currently have.

**Delegation chain:** `ClubOperationsImpl` should delegate through `BookClubRepository` (domain), not `BookClubRemoteDataSource` (data) directly. Check what's available:
- `BookClubMembershipRepository` already has `getRemoteClubMemberships(userId: String): Result<List<String>, DataError.Sync>` — use this for `getClubMembershipsForUser`
- For `getClubsCreatedByUser` and `removeUserFromClub`, add to `BookClubManagementRepository` (domain interface in `bookclub/domain/repository/`) and implement in the repository impl. The impl delegates to `BookClubRemoteDataSource` internally.

**Files to modify:**
- `bookclub/domain/repository/BookClubManagementRepository.kt` — add `getClubsCreatedByUser(userId)` and `removeUserFromClub(clubCode, userId)`
- The `BookClubRepository` impl (find it — implements `BookClubManagementRepository`) — implement by delegating to `BookClubRemoteDataSource`
- `bookclub/presentation/handlers/ClubOperationsImpl.kt` — implement the 3 new `ClubOperations` methods by delegating to `bookClubRepository`

**Update mocks:** Any test stubs for `ClubOperations` need the 3 new methods. Search for classes implementing `ClubOperations` in test code.

**Compile check:** `./gradlew assembleDebug`

**Step 2 — Extract `CredentialFetcher` presentation-layer interface**

**New file:** `auth/presentation/service/CredentialFetcher.kt`
```kotlin
package uk.co.zlurgg.mybookshelf.auth.presentation.service

import android.app.Activity
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface CredentialFetcher {
    suspend fun fetch(activity: Activity): Result<String, DataError.Local>
}
```

**File:** `auth/data/service/GoogleCredentialFetcher.kt` — add `: CredentialFetcher` to the class declaration. Import the interface. No other changes — the method signature already matches.

**File:** `auth/di/AuthModule.kt` line 45 — update binding:
```kotlin
// Before:
single { GoogleCredentialFetcher(authConfig = get()) }
// After:
single<CredentialFetcher> { GoogleCredentialFetcher(authConfig = get()) }
```
Import `CredentialFetcher` from `auth.presentation.service`.

**Compile check:** `./gradlew assembleDebug`

**Step 3 — Fix `GoogleAuthUiClient.deleteAccount()` credential clear**

**File:** `auth/data/service/GoogleAuthUiClient.kt` lines 91-102. Change:
```kotlin
// Before:
user.delete().await()
credentialManager.clearCredentialState(ClearCredentialStateRequest())
Timber.tag(TAG).d("=== DELETE ACCOUNT COMPLETE ===")
Result.Success(Unit)

// After:
user.delete().await()
// Best-effort — account is already deleted, credential clearing is cleanup
try {
    credentialManager.clearCredentialState(ClearCredentialStateRequest())
} catch (e: Exception) {
    Timber.tag(TAG).w(e, "Credential state clear failed after account deletion")
}
Timber.tag(TAG).d("=== DELETE ACCOUNT COMPLETE ===")
Result.Success(Unit)
```

The outer `catch (e: Exception)` stays as-is — it only catches pre-deletion failures now.

**Compile check:** `./gradlew assembleDebug && ./gradlew test`

---

### Phase 2: Create `account/` Package (Steps 4-10)

Create the new feature package. Old profile code still exists — we'll remove it in Phase 3.

**Step 4 — DeleteAccountUseCase (domain)**

**New file:** `account/domain/usecase/DeleteAccountUseCase.kt`
```kotlin
package uk.co.zlurgg.mybookshelf.account.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface DeleteAccountUseCase {
    suspend operator fun invoke(): Result<Unit, DataError>
    suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError>
}
```

**New file:** `account/domain/usecase/DeleteAccountUseCaseImpl.kt`

**Dependencies (5):**
```kotlin
class DeleteAccountUseCaseImpl(
    private val currentUserProvider: CurrentUserProvider,
    private val syncScheduler: SyncSchedulerService,
    private val syncRepository: SyncRepository,
    private val clubOperations: ClubOperations,
    private val authService: AuthService,
) : DeleteAccountUseCase
```

**`invoke()` flow:**
1. `currentUserProvider.getCurrentUserId()` — null → return `AUTH_FAILED`
2. `syncScheduler.cancelAllSync()`
3. Club cleanup (see below)
4. `syncRepository.deleteAllRemoteData(userId)` — on error, return it (auth preserved)
5. `authService.deleteAccount()` — on `REQUIRES_RECENT_LOGIN`, return it. On other error, return it

**Club cleanup in `invoke()`:**
```kotlin
// Get clubs created by user
val createdResult = clubOperations.getClubsCreatedByUser(userId)
if (createdResult is Result.Error) return createdResult
val createdCodes = (createdResult as Result.Success).data

// Delete each created club
for (code in createdCodes) {
    val deleteResult = clubOperations.deleteBookClub(code)
    if (deleteResult is Result.Error) return deleteResult
}

// Get memberships, subtract created clubs, remove from remaining
val memberResult = clubOperations.getClubMembershipsForUser(userId)
if (memberResult is Result.Error) return memberResult
val memberCodes = (memberResult as Result.Success).data - createdCodes.toSet()

for (code in memberCodes) {
    val removeResult = clubOperations.removeUserFromClub(code, userId)
    if (removeResult is Result.Error) return removeResult
}
```

**`retryAfterReAuth()`:**
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

**No `@Suppress("TooGenericExceptionCaught")`** — no try/catch blocks in this use case. Add `@Suppress("ReturnCount")` on `invoke()` only if detekt flags it.

**Step 5 — AccountState**

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
    val requestReAuth: Boolean = false,
    val navigateToSignIn: Boolean = false,
    val errorMessage: String? = null,
)
```

**Step 6 — AccountAction**

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
    data object OnReAuthFailed : AccountAction
    data object DismissError : AccountAction
    data object ResetNavigation : AccountAction
    data object ResetReAuth : AccountAction
}
```

**Step 7 — AccountViewModel**

**New file:** `account/presentation/AccountViewModel.kt`

```kotlin
class AccountViewModel(
    private val authUseCases: AuthUseCases,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : ViewModel()
```

Key implementation details:
- `init { loadUser() }` — calls `authUseCases.getSignedInUser()`
- `deleteAccount()` — **must have `isDeleting` guard**: `if (_state.value.isDeleting) return`
- `handleDeleteError` — check `error == DataError.Local.REQUIRES_RECENT_LOGIN` → set `requestReAuth = true`
- `OnReAuthFailed` → set `errorMessage` (e.g., "Sign-in was cancelled or failed"), clear `requestReAuth`
- Use `ErrorFormatter.formatDataErrorMessage(error, "operation")` for error messages — same as `ProfileViewModel`

Reference `auth/presentation/profile/ProfileViewModel.kt` for the existing logic, adapting the changes above.

**Step 8 — AccountScreenRoot (separate file)**

**New file:** `account/presentation/AccountScreenRoot.kt`

```kotlin
@Composable
fun AccountScreenRoot(
    viewModel: AccountViewModel = koinViewModel(),
    credentialFetcher: CredentialFetcher = koinInject(),  // presentation interface, NOT GoogleCredentialFetcher
    onNavigateToSignIn: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current

    // One-shot: navigate to sign-in
    LaunchedEffect(state.navigateToSignIn) {
        if (state.navigateToSignIn) {
            onNavigateToSignIn()
            viewModel.onAction(AccountAction.ResetNavigation)
        }
    }

    // One-shot: show error snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(AccountAction.DismissError)
        }
    }

    // One-shot: re-auth credential fetch
    LaunchedEffect(state.requestReAuth) {
        if (state.requestReAuth) {
            val currentActivity = activity ?: run {
                viewModel.onAction(AccountAction.OnReAuthFailed)
                viewModel.onAction(AccountAction.ResetReAuth)
                return@LaunchedEffect
            }
            when (val result = credentialFetcher.fetch(currentActivity)) {
                is Result.Success -> viewModel.onAction(AccountAction.OnReAuthCompleted(result.data))
                is Result.Error -> viewModel.onAction(AccountAction.OnReAuthFailed)
            }
            viewModel.onAction(AccountAction.ResetReAuth)
        }
    }

    AccountScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}
```

**Imports:** `CredentialFetcher` from `auth.presentation.service` (NOT `auth.data.service`). `Result` from `core.domain.result`.

**Step 9 — AccountScreen (separate file, pure)**

**New file:** `account/presentation/AccountScreen.kt`

Pure composable — **no `LaunchedEffect`, no `koinInject`, no side effects**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    state: AccountState,
    snackbarHostState: SnackbarHostState,
    onAction: (AccountAction) -> Unit,
    onBack: () -> Unit,
)
```

Contains:
- `Scaffold` with `TopAppBar` (back button + title from `R.string.profile_title`)
- `SnackbarHost(snackbarHostState)`
- Profile avatar (`AsyncImage` or `AccountCircle` fallback), name, email
- "Sign Out" `OutlinedButton`
- `HorizontalDivider`
- "Delete Account" `Button` (error color, `DeleteForever` icon)
- Sign out confirmation `AlertDialog` (state-driven: `state.showSignOutDialog`)
- `DeleteAccountConfirmDialog` (state-driven: `state.showDeleteConfirmDialog`)
- Deleting progress `AlertDialog` (non-dismissible: `state.isDeleting`)

**No re-auth dialog** — re-auth is handled by `requestReAuth` one-shot in Root.

Reference `auth/presentation/profile/ProfileScreen.kt:99-253` for the UI layout. Copy the composable structure but remove:
- The re-auth dialog (lines 256-272)
- The `LaunchedEffect` inside the dialog
- The `onReAuthFetchCredential` parameter

Add `@Preview` at the bottom.

**Step 10 — DeleteAccountConfirmDialog**

**New file:** `account/presentation/components/DeleteAccountConfirmDialog.kt`

Copy from `auth/presentation/profile/components/DeleteAccountConfirmDialog.kt`. Update package declaration only.

---

### Phase 3: Wire Navigation + DI (Steps 11-12)

**Step 11 — AccountModule DI**

**New file:** `account/di/AccountModule.kt`
```kotlin
package uk.co.zlurgg.mybookshelf.account.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.account.domain.usecase.DeleteAccountUseCase
import uk.co.zlurgg.mybookshelf.account.domain.usecase.DeleteAccountUseCaseImpl
import uk.co.zlurgg.mybookshelf.account.presentation.AccountViewModel

val accountModule = module {
    singleOf(::DeleteAccountUseCaseImpl).bind<DeleteAccountUseCase>()
    viewModel { AccountViewModel(get(), get()) }
}
```

**File:** `di/AppModule.kt` — add `accountModule` to the includes list. Add import.

**Step 12 — Navigation routing**

**File:** `app/NavigationRoute.kt` — rename `Profile` to `Account`:
```kotlin
data object Account : NavigationRoute {
    const val ROUTE = "account"
    fun createRoute() = ROUTE
}
```

**File:** `app/presentation/MyBookShelfApp.kt` lines 155-166:
- Change `NavigationRoute.Profile.ROUTE` → `NavigationRoute.Account.ROUTE`
- Change `ProfileScreenRoot` → `AccountScreenRoot`
- Update import: remove `auth.presentation.profile.ProfileScreenRoot`, add `account.presentation.AccountScreenRoot`

**File:** `app/presentation/MyBookShelfApp.kt` — find where `NavigationRoute.Profile.createRoute()` is called for navigation (around line 135-143). Change to `NavigationRoute.Account.createRoute()`.

**File:** `bookcase/presentation/BookcaseScreen.kt`:
- Line 74: rename `onProfileClick` → `onAccountClick` in `BookcaseScreenRoot` signature
- Line 191: rename `onProfileClick` → `onAccountClick` in the `BookcaseScreen` call
- Line 204: rename `onProfileClick` → `onAccountClick` in `BookcaseScreen` signature
- Update the `AccountCircle` icon button's content description to use `R.string.cd_account` (or keep `cd_profile` if renaming strings is deferred)

**File:** `app/presentation/MyBookShelfApp.kt` — update the callback name in the `BookcaseScreenRoot` call site.

**Compile check:** `./gradlew assembleDebug`

---

### Phase 4: Remove Old Profile Code (Steps 13-14)

**Step 13 — Clean up AuthModule + AuthUseCases**

**File:** `auth/domain/usecase/AuthUseCases.kt` — remove `deleteAccount` field:
```kotlin
// Before:
data class AuthUseCases(
    val signIn: SignInUseCase,
    val signOut: SignOutUseCase,
    val checkSignInStatus: CheckSignInStatusUseCase,
    val getCurrentUserId: GetCurrentUserIdUseCase,
    val deleteAccount: DeleteAccountUseCase,
    val getSignedInUser: GetSignedInUserUseCase,
)

// After:
data class AuthUseCases(
    val signIn: SignInUseCase,
    val signOut: SignOutUseCase,
    val checkSignInStatus: CheckSignInStatusUseCase,
    val getCurrentUserId: GetCurrentUserIdUseCase,
    val getSignedInUser: GetSignedInUserUseCase,
)
```

**File:** `auth/di/AuthModule.kt`:
- Remove line 27-28: `DeleteAccountUseCase` / `DeleteAccountUseCaseImpl` imports
- Remove line 32: `ProfileViewModel` import
- Remove line 61: `single<DeleteAccountUseCase> { ... }` registration
- Remove line 66: `viewModel { ProfileViewModel(get()) }` registration
- Update line 63: `AuthUseCases` constructor — remove `get()` for the deleted `deleteAccount` parameter (goes from 6 args to 5)

**File:** `bookcase/presentation/BookcaseViewModel.kt` — add tech-debt TODO:
```kotlin
// TODO: BookcaseViewModel only uses checkSignInStatus + getCurrentUserId from AuthUseCases.
// Inject the two individual use cases instead of the full aggregator.
```

**Step 14 — Delete old profile files**

Delete these files:
- `auth/presentation/profile/ProfileScreen.kt`
- `auth/presentation/profile/ProfileState.kt`
- `auth/presentation/profile/ProfileAction.kt`
- `auth/presentation/profile/ProfileViewModel.kt`
- `auth/presentation/profile/components/DeleteAccountConfirmDialog.kt`
- `auth/domain/usecase/DeleteAccountUseCase.kt`
- `auth/domain/usecase/DeleteAccountUseCaseImpl.kt`

**Keep** these (still used by AccountViewModel via AuthUseCases):
- `auth/domain/usecase/GetSignedInUserUseCase.kt`
- `auth/domain/usecase/GetSignedInUserUseCaseImpl.kt`

**Compile check:** `./gradlew assembleDebug`

---

### Phase 5: Tests (Step 15)

**Step 15a — DeleteAccountUseCaseTest**

**New file:** `app/src/test/java/uk/co/zlurgg/mybookshelf/account/domain/usecase/DeleteAccountUseCaseTest.kt`

Test cases:
1. `invoke - full success - returns success` — verify sync cancelled, clubs cleaned, remote data deleted, auth deleted
2. `invoke - not signed in - returns AUTH_FAILED immediately`
3. `invoke - club query fails - remote data NOT deleted, auth NOT deleted`
4. `invoke - club delete fails - remote data NOT deleted, auth NOT deleted`
5. `invoke - firestore deletion fails - auth NOT deleted`
6. `invoke - auth requires recent login - returns REQUIRES_RECENT_LOGIN`
7. `invoke - deletes clubs created by user`
8. `invoke - removes user from member clubs excluding created clubs` — verify subtraction
9. `retryAfterReAuth - success - returns success` — verify reauthenticate called, deleteAccount called
10. `retryAfterReAuth - reauth fails - returns error, auth NOT deleted`

Mock `ClubOperations` (domain interface) — NOT `BookClubRemoteDataSource`. Use the same pattern as `DeleteAccountUseCaseTest.kt` but replace `StubBookClubRemoteDataSource` with a `ClubOperations` mock. The mock only needs: `getClubsCreatedByUser`, `getClubMembershipsForUser`, `removeUserFromClub`, `deleteBookClub`, `clearAllMemberships` — all others can `TODO()`. Use Kotlin's `object : ClubOperations by baseImpl { override ... }` delegation to keep it concise.

Reference `testutil/mocks/MockSyncRepository.kt` and `testutil/mocks/MockAuthService.kt` for mock patterns.

**Step 15b — AccountViewModelTest**

**New file:** `app/src/test/java/uk/co/zlurgg/mybookshelf/account/presentation/AccountViewModelTest.kt`

Test cases:
1. `init - loads user data into state`
2. `init - no user - state remains empty`
3. `sign out - success - sets navigateToSignIn`
4. `sign out - failure - sets errorMessage`
5. `delete - success - sets navigateToSignIn`
6. `delete - requires recent login - sets requestReAuth`
7. `delete - other failure - sets errorMessage`
8. `delete - while already deleting - no-op` (double-tap guard)
9. `reauth retry - success - navigates to sign in`
10. `reauth retry - failure - sets errorMessage`
11. `OnReAuthFailed - sets errorMessage`
12. `dismiss error - clears errorMessage`
13. `reset navigation - clears navigateToSignIn`

Reference `auth/presentation/profile/ProfileViewModelTest.kt` for setup patterns — `testHelper`, `StandardTestDispatcher`, `Dispatchers.setMain`.

**Step 15c — Delete old tests + update mocks**

Delete:
- `auth/domain/usecase/DeleteAccountUseCaseTest.kt`
- `auth/presentation/profile/ProfileViewModelTest.kt`

**File:** `testutil/mocks/MockAuthService.kt` — add `email` parameter to `configureSignedIn`:
```kotlin
// Before:
fun configureSignedIn(userId: String, username: String) {
    signedInUser = UserData(userId, username, profilePictureUrl = null)
}

// After:
fun configureSignedIn(userId: String, username: String, email: String = "test@example.com") {
    signedInUser = UserData(userId, username, email = email, profilePictureUrl = null)
}
```

---

### Phase 6: Final Fixes (Step 16)

**Step 16a — Delete parent user doc**

**File:** `sync/data/repository/SyncRepositoryImpl.kt` — in `deleteAllRemoteData()`, after the three subcollection deletions, add:
```kotlin
// Delete the user document itself
firestore.collection("users").document(userId).delete().await()
```

Check `FirestoreCollections.kt` for the collection constant name — use that instead of the hardcoded string.

**Step 16b — Document comment deletion decision**

**File:** `sync/data/service/FirestoreBookClubRemoteDataSourceImpl.kt` — in `removeUserFromClub()`, add a comment after the reviews deletion loop:
```kotlin
// Reviews are keyed by userId (direct delete). Comments use auto-generated IDs
// and are intentionally kept — removing them creates gaps in discussions.
// The user's attribution remains but the account is gone.
```

**Step 16c — Rename string resources (optional)**

**File:** `app/src/main/res/values/strings.xml` lines 331-342 — rename `profile_*` keys to `account_*` and update all references in AccountScreen.kt. Or keep `profile_*` keys — they work, it's cosmetic. If renaming, also update `cd_profile` → `cd_account`.

---

## Files Quick Reference

**All paths relative to `app/src/main/java/uk/co/zlurgg/mybookshelf/`**

### New files (11)
| File | Purpose |
|------|---------|
| `account/di/AccountModule.kt` | DI registrations |
| `account/domain/usecase/DeleteAccountUseCase.kt` | Interface |
| `account/domain/usecase/DeleteAccountUseCaseImpl.kt` | Simplified deletion (5 deps) |
| `account/presentation/AccountScreenRoot.kt` | Side effects, navigation, credential fetch |
| `account/presentation/AccountScreen.kt` | Pure UI |
| `account/presentation/AccountState.kt` | State data class |
| `account/presentation/AccountAction.kt` | Sealed action interface |
| `account/presentation/AccountViewModel.kt` | State management |
| `account/presentation/components/DeleteAccountConfirmDialog.kt` | Confirmation dialog |
| `auth/presentation/service/CredentialFetcher.kt` | Presentation-layer interface |
| Tests: `account/domain/usecase/DeleteAccountUseCaseTest.kt`, `account/presentation/AccountViewModelTest.kt` | |

### Modified files (12)
| File | Change |
|------|--------|
| `book/domain/service/ClubOperations.kt` | Add 3 methods |
| `bookclub/domain/repository/BookClubManagementRepository.kt` | Add 2 methods |
| `bookclub/` repository impl | Implement 2 new repo methods |
| `bookclub/presentation/handlers/ClubOperationsImpl.kt` | Implement 3 new methods |
| `auth/data/service/GoogleCredentialFetcher.kt` | Implement `CredentialFetcher` interface |
| `auth/data/service/GoogleAuthUiClient.kt` | Try-catch on credential clear |
| `auth/di/AuthModule.kt` | Remove DeleteAccountUseCase, ProfileViewModel; bind CredentialFetcher |
| `auth/domain/usecase/AuthUseCases.kt` | Remove `deleteAccount` field |
| `di/AppModule.kt` | Add `accountModule` |
| `app/NavigationRoute.kt` | `Profile` → `Account` |
| `app/presentation/MyBookShelfApp.kt` | Route to `AccountScreenRoot` |
| `bookcase/presentation/BookcaseScreen.kt` | `onProfileClick` → `onAccountClick` |

### Deleted files (7 + 2 tests)
| File |
|------|
| `auth/presentation/profile/ProfileScreen.kt` |
| `auth/presentation/profile/ProfileState.kt` |
| `auth/presentation/profile/ProfileAction.kt` |
| `auth/presentation/profile/ProfileViewModel.kt` |
| `auth/presentation/profile/components/DeleteAccountConfirmDialog.kt` |
| `auth/domain/usecase/DeleteAccountUseCase.kt` |
| `auth/domain/usecase/DeleteAccountUseCaseImpl.kt` |
| `auth/domain/usecase/DeleteAccountUseCaseTest.kt` (test) |
| `auth/presentation/profile/ProfileViewModelTest.kt` (test) |

## Gotchas

1. **`ClubOperationsImpl` delegation chain** — it can't import `BookClubRemoteDataSource` (data layer). Route through `BookClubRepository` (domain). `getRemoteClubMemberships` already exists on `BookClubMembershipRepository`. The other two methods need adding to `BookClubManagementRepository`.
2. **`CredentialFetcher` is presentation, not domain** — `auth/presentation/service/`, not `auth/domain/service/`. Activity is an Android framework class.
3. **`AuthUseCases` constructor arg count changes** — from 6 to 5. Update the `single { AuthUseCases(...) }` call in `AuthModule.kt` — one fewer `get()`.
4. **`!!` is banned** — use safe calls or `requireNotNull()`.
5. **`SignOutUseCase` is NOT removed** — it's still used by `AccountViewModel` via `AuthUseCases`. Only `DeleteAccountUseCase` moves out.
6. **Detekt** — run `./gradlew detekt` before committing. The `@Suppress("ReturnCount")` may be needed on `invoke()` if it has multiple early returns.
7. **Don't rename `GetSignedInUserUseCase`** — it stays in `auth/`. Account screen uses it via `AuthUseCases.getSignedInUser`.
8. **String resources** — the existing `profile_*` strings in `strings.xml` still work. Renaming to `account_*` is optional cosmetic cleanup.

## Verification Checklist

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew detekt` passes
- [ ] `./gradlew test` passes
- [ ] No files remain in `auth/presentation/profile/`
- [ ] No `DeleteAccountUseCase` import from `auth/domain/usecase/` anywhere
- [ ] No `BookClubRemoteDataSource` import in `account/` package
- [ ] No `GoogleCredentialFetcher` import in `account/` package
- [ ] `AccountScreen.kt` has zero `LaunchedEffect` calls
- [ ] `AccountScreenRoot.kt` imports `CredentialFetcher` from `auth.presentation.service`
- [ ] Guest: account icon → sign-in screen
- [ ] Signed in: account icon → account screen with email/name
- [ ] Sign out from account → sign-in screen
- [ ] Delete account → confirm → loading → sign-in screen
- [ ] Delete with stale session → Google picker → completion
- [ ] Delete with network failure → error shown, still signed in, retry works
