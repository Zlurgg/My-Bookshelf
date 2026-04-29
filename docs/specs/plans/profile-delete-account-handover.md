# Handover: Profile Screen + Delete Account

## What This Is

Implement a Profile screen with account deletion for Play Store compliance. The full architectural plan is in `docs/specs/plans/profile-delete-account.md` — read it first. This document tells you how to execute it.

## Branch

Create from `main`: `feat/profile-delete-account`

## Before You Start

Read these specs — the plan was reviewed against them and must comply:

| Spec | Why |
|------|-----|
| `docs/specs/constitution.md` | ViewModels depend on UseCases only, never services/repositories directly |
| `docs/specs/patterns/usecase.md` | UseCase interface + impl pattern |
| `docs/specs/patterns/state-management.md` | ViewModel state/action pattern |
| `docs/specs/patterns/compose-screens.md` | Root/Screen split, LaunchedEffect + reset for one-shot navigation |
| `docs/specs/style/code-style.md` | Naming, testing conventions, file locations |

## Key Architectural Decisions (Already Reviewed — Don't Revisit)

- **Deletion order**: Club cleanup → Firestore user data → Firebase Auth → local data. Auth deletion is the point of no return — everything before it must succeed first
- **No DeletionProgress Flow** — simple `isDeleting: Boolean`. The operation takes ~2 seconds
- **No ReauthenticateUseCase** — re-auth is folded into `DeleteAccountUseCase.retryAfterReAuth(idToken)`
- **No WelcomePreferences cleanup** — dead key after Firebase UID is deleted
- **`removeUserFromClub()` must use `.set(data, SetOptions.merge())`** not `.update()` for retry idempotency
- **Return type `Result<Unit, DataError>`** (parent sealed interface) — spans both Sync and Local error domains
- **Single-file Root/Screen** for ProfileScreen.kt — conscious deviation, documented

## Implementation Order

Work in this order. Each step builds on the previous. Commit after each step compiles.

### Phase 1: Domain + Data (Steps 1-5)

These are backend changes with no UI. The app should compile and all existing tests should pass after each step.

**Step 1 — Error types + AuthService interface**
- Add `REQUIRES_RECENT_LOGIN` to `DataError.Local` enum
- Add `deleteAccount(): Result<Unit, DataError.Local>` and `reauthenticate(idToken: String): Result<Unit, DataError.Local>` to `AuthService` interface
- Update `MockAuthService` in test utils with stubs (return `Result.Error(AUTH_FAILED)` by default)
- **Compile check**: existing tests still pass since mocks implement the new methods

**Step 2 — DeleteAccountUseCase**
- Create interface with `invoke()` and `retryAfterReAuth(idToken: String)`, both returning `Result<Unit, DataError>`
- Create impl — wire dependencies but initially throw `NotImplementedError` in method bodies
- This is the hardest file. Reference `SignOutUseCaseImpl` for the pattern, but the deletion logic is more complex

**Step 3 — GoogleAuthUiClient implementation**
- `deleteAccount()`: null-check `auth.currentUser` (no `!!`), call `user.delete().await()`, clear credential state. Catch `FirebaseAuthRecentLoginRequiredException` → `REQUIRES_RECENT_LOGIN`
- `reauthenticate()`: null-check, build Google credential, call `user.reauthenticate(credential).await()`
- Reference existing `signIn()` and `signOut()` in the same file for error handling pattern

**Step 4 — Remote data deletion**
- Add `deleteAllBooks()`, `deleteAllBookshelves()`, `deleteUserPreferences()` to data source interfaces
- Implement in Firestore classes: query subcollection → batch delete in groups of 500. Use `FirestoreOperationHelper.execute()` wrapper
- Add `deleteAllRemoteData(userId)` to `SyncRepository` — calls the three methods above. Does NOT handle clubs
- Add `getClubsCreatedByUser()`, `getClubMembershipsForUser()`, `removeUserFromClub()` to `BookClubRemoteDataSource`
- Implement in `FirestoreBookClubRemoteDataSourceImpl`. **Critical**: `removeUserFromClub` must use `.set(data, SetOptions.merge())` not `.update()`
- Update `MockSyncRepository` with stub

**Step 5 — GetSignedInUserUseCase**
- Simple interface + impl wrapping `AuthService.getSignedInUser()` → `UserData?`
- Add to `AuthUseCases` data class

**Now go back and implement `DeleteAccountUseCaseImpl` properly:**
- Wire the full deletion sequence (see plan Step 2 for exact order)
- Sync wait: collect `observeSyncState()`, wait for non-in-progress state with timeout (~10s)
- Club cleanup before Firestore deletion (see plan Step 4 for club logic)
- `retryAfterReAuth`: lightweight check — does `users/{uid}` doc exist? If yes, fall back to `invoke()`. If no, proceed with Auth deletion + local cleanup only
- Add KDoc on `retryAfterReAuth` explaining the precondition

### Phase 2: Presentation (Steps 6-7)

**Step 6 — State, Actions, ViewModel**
- `ProfileState` — see plan for exact fields. Single `navigateToSignIn` one-shot flag
- `ProfileAction` — sealed interface with all actions listed in plan
- `ProfileViewModel` — inject `AuthUseCases` (which includes `signOut`, `deleteAccount`, `getSignedInUser`) and `DeleteAccountUseCase`
- Init: load user from `getSignedInUser()`
- `ConfirmSignOut`: call `signOut()`, set `navigateToSignIn = true`
- `ConfirmDeleteAccount`: set `isDeleting = true`, call `deleteAccount()`. Map result
- `OnReAuthCompleted(idToken)`: call `deleteAccount.retryAfterReAuth(idToken)`. Map result

**Step 7 — Screen composables**
- `ProfileScreenRoot` + `ProfileScreen` in single file
- Follow `compose-screens.md` pattern — Root handles state collection, side effects, navigation. Screen is pure/preview-friendly
- `LaunchedEffect(state.navigateToSignIn)` pattern with `ResetNavigation`
- Credential fetch lambda in Root (needs Activity context — see `SignInScreenRoot` for reference pattern)
- `DeleteAccountConfirmDialog` in separate component file

### Phase 3: Navigation + Cleanup (Step 8)

This is the riskiest step — it touches the most files and removes existing functionality.

**Step 8 — Wire navigation, update bookcase top bar**
- Add `Profile` to `NavigationRoute`
- Add composable in `MyBookShelfApp` — wire `onNavigateToSignIn` to navigate to SignIn clearing backstack
- Add `AccountCircle` icon to bookcase `TopAppBar` actions. Tapped → `onProfileClick(isSignedIn)`
- In `BookcaseScreenRoot`: if signed in → navigate to Profile route; if guest → navigate to SignIn
- **Remove from SettingsMenu**: `onSignIn`, `onSignOut`, `isSignedIn` params + related menu items
- **Remove from BookcaseAction**: `OnSignInClick`, `ShowSignOutDialog`, `DismissSignOutDialog`, `ConfirmSignOut`, `ResetNavigateToSignIn`
- **Remove from BookcaseState**: `showSignOutDialog`, `signedOutSuccessfully`, `navigateToSignIn`
- **Remove from BookcaseViewModel**: sign-out handling, `SignOutUseCase` dependency
- **Remove**: `SignOutDialog` import/usage from `BookcaseScreen`
- **Update BookcaseViewModelTest**: remove sign-out tests

### Phase 4: DI + Strings (Steps 9-11)

**Step 9 — DI wiring** in `AuthModule.kt`
- Register `DeleteAccountUseCaseImpl` → `DeleteAccountUseCase`
- Register `GetSignedInUserUseCaseImpl` → `GetSignedInUserUseCase`
- Register `ProfileViewModel` via `viewModel { }`
- Update `AuthUseCases` constructor call to include new use cases

**Step 10 — Update AuthUseCases** data class with new fields

**Step 11 — String resources** — add all strings before compiling UI steps. Missing strings cause build failures

### Phase 5: Tests

**`DeleteAccountUseCaseTest`** — Critical test file. Cover:
1. Full success path (all steps complete)
2. Firestore failure → Auth NOT deleted (verify `deleteAccount()` never called)
3. Club cleanup partial failure → Firestore user data NOT deleted, Auth NOT deleted
4. `REQUIRES_RECENT_LOGIN` returned from `deleteAccount()`
5. `retryAfterReAuth` success (remote already gone → Auth deleted → local cleared)
6. `retryAfterReAuth` re-auth failure
7. `retryAfterReAuth` precondition guard (remote data still exists → falls back to full invoke)
8. Sync wait timeout handling

Use fake/mock repositories. Reference `SignOutUseCaseTest` for setup patterns.

**`ProfileViewModelTest`** — Cover:
1. Init loads user data into state
2. Sign-out → `navigateToSignIn = true`
3. Delete success → `navigateToSignIn = true`
4. Delete with re-auth → `showReAuthDialog = true` → retry → success
5. Delete failure → `errorMessage` set
6. Error dismissal clears `errorMessage`
7. `ResetNavigation` clears `navigateToSignIn`

Use `advanceUntilIdle()`. Collect StateFlow to trigger init. Reference `BookcaseViewModelTest` for setup.

**Update `BookcaseViewModelTest`** — Remove sign-out action/state tests.

## Files Quick Reference

All paths relative to `app/src/main/java/uk/co/zlurgg/mybookshelf/`. Tests under `app/src/test/java/uk/co/zlurgg/mybookshelf/`.

**Key existing files to read before starting:**
- `auth/domain/usecase/SignOutUseCaseImpl.kt` — closest pattern to DeleteAccountUseCase
- `auth/data/service/GoogleAuthUiClient.kt` — where Auth methods go
- `bookcase/presentation/BookcaseScreen.kt:260-295` — current TopAppBar actions (what you're modifying)
- `bookcase/presentation/components/SettingsMenu.kt` — what you're simplifying
- `sync/data/service/FirestoreOperationHelper.kt` — error handling wrapper for Firestore calls
- `sync/data/service/FirestoreBookClubRemoteDataSourceImpl.kt:209-225` — existing `deleteBookClub()` for reference
- `sync/data/service/FirestoreCollections.kt` — Firestore collection constants

## Gotchas

1. **`!!` is banned** — `CLAUDE.md` anti-patterns. Use safe calls or `requireNotNull` with message
2. **Firestore `.update()` throws on non-existent docs** — this is why `removeUserFromClub` must use `.set(merge)` instead. The retry scenario (partial failure, user retries) means the target doc may not exist
3. **Firestore batch limit is 500 operations** — chunk batch deletes. Most users won't hit this but code defensively
4. **Credential fetch needs Activity context** — this stays in the Root composable, passed to ViewModel as a lambda. See `SignInScreenRoot` for the pattern with `context.findActivity()`
5. **`SignOutUseCase` is still used** — by `ProfileViewModel` for sign-out. It's removed from `BookcaseViewModel`, not from the app
6. **Auth deletion is irreversible** — if you're testing manually, the Firebase Auth account is gone. You'll need to sign in fresh to create a new one
7. **`SettingsMenu` still exists after changes** — it keeps Help, About, and Join Book Club. Only auth items are removed
8. **Detekt**: Run `./gradlew detekt` before committing. The project has strict rules in `app/detekt.yml`

## Verification Checklist

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew detekt` passes
- [ ] `./gradlew test` passes (all existing + new tests)
- [ ] Guest: profile icon → sign-in screen
- [ ] Signed in: profile icon → profile screen with email/name
- [ ] Sign out from profile → sign-in screen
- [ ] Delete account → confirm → loading → sign-in screen
- [ ] Delete with stale session → re-auth → completion
- [ ] Delete with network failure → error, still signed in, can retry
- [ ] Delete as club creator → club deleted
- [ ] Firebase Console: user subcollections empty, Auth account gone
