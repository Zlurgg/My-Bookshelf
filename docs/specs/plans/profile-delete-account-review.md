# Handover: Profile + Delete Account — Code Review Fixes

## Context

The Profile screen and account deletion flow were implemented on the `profile-page` branch. A code review identified 10 issues. This document is the plan for a follow-up session to review each finding, decide keep/fix/skip, and implement the fixes.

The implementation is across 4 commits:
```
8173be6 docs: Add profile and delete account specs
dd53e3c test(auth): Add DeleteAccountUseCase and ProfileViewModel tests
af1887e feat(auth): Add Profile screen and move auth out of bookcase
e70c19e feat(auth): Add account deletion domain and data layer
```

Build, tests, and detekt all pass. No regressions.

## Before You Start

Read these files to understand the current state:
- `docs/specs/plans/profile-delete-account.md` — the original architectural plan
- `app/src/main/java/uk/co/zlurgg/mybookshelf/auth/domain/usecase/DeleteAccountUseCaseImpl.kt` — the core use case
- `app/src/main/java/uk/co/zlurgg/mybookshelf/auth/presentation/profile/ProfileScreen.kt` — the UI
- `app/src/main/java/uk/co/zlurgg/mybookshelf/auth/presentation/profile/ProfileViewModel.kt`

## Issues to Review

Each issue has a recommendation, but all should be reviewed with fresh eyes — the recommendation may be wrong.

---

### Issue 1: `getClubMembershipsForUser` returns ALL memberships, not "member but not creator"

**Severity**: Minor bug (redundant no-op Firestore calls, misleading logs)

**Location**: `DeleteAccountUseCaseImpl.cleanUpBookClubs()` (lines 133-178)

**Problem**: `cleanUpBookClubs` first deletes clubs created by the user via `clubOperations.deleteBookClub(code)`, then calls `getClubMembershipsForUser(userId)` to remove the user from remaining clubs. But `getClubMembershipsForUser` reads the full `club_memberships` array from `users/{uid}/settings/preferences` — it includes clubs the user created, not just ones they joined.

After deleting club-A (step 1), the code then tries `removeUserFromClub("club-A", userId)` (step 2) on a club that was just fully deleted. Firestore `delete()` on a non-existent doc is a no-op, so this doesn't crash, but:
- Wasted network calls (member doc delete, book query, review deletes, preferences merge)
- Log lines like "Removing user from club: club-A" after "Deleting club created by user: club-A" are confusing

**The plan said**: "`getClubMembershipsForUser` returns club codes where user is member but not creator." The Firestore implementation doesn't filter.

**Options**:
1. Filter in the use case: subtract `createdClubCodes` from the membership list before iterating
2. Filter in the data source query: cross-reference with club metadata `created_by` field
3. Accept the redundancy: document it as idempotent by design

**Recommendation**: Option 1 — simplest, one line in the use case. Pass the created club codes list and subtract.

---

### Issue 2: `removeUserFromClub` deletes reviews but not comments

**Severity**: Design question

**Location**: `FirestoreBookClubRemoteDataSourceImpl.removeUserFromClub()` (lines 413-449)

**Problem**: The method removes the member doc and iterates all books to delete the user's reviews (`reviews/{userId}`), but doesn't touch comments. Comments use auto-generated IDs (not userId), so deleting them requires a query (`where userId == X`), not a direct doc path.

**Question**: Is this intentional? Two reasonable positions:
- **Keep comments**: They're part of the discussion. Removing them creates confusing gaps (replies to deleted comments). Attribution stays — username is embedded in the comment doc.
- **Delete comments**: Privacy-first. When a user leaves/deletes, their content should go. But this is heavier (query + batch delete per book).

The plan doesn't mention comments in `removeUserFromClub`. The existing `deleteBookClub` method also doesn't individually delete comments — it deletes the entire club doc tree.

**Recommendation**: Keep as-is for now. Comments persist when leaving a club, which is standard social behavior. Account deletion already removes the club membership list doc (via `deleteAllRemoteData`), and the comments become "orphaned attribution" — the user's name stays but the account is gone. If privacy is a concern for Play Store review, revisit. Document the decision.

---

### Issue 3: Domain use case imports data layer `BookClubRemoteDataSource`

**Severity**: Architecture violation

**Location**: `DeleteAccountUseCaseImpl.kt` line 13

**Problem**: `DeleteAccountUseCaseImpl` (domain layer, `auth/domain/usecase/`) imports `BookClubRemoteDataSource` from `sync/data/repository/`. The constitution says domain depends on nothing from data layers.

`SyncRepository` (which the use case also depends on) is fine — it lives in `sync/domain/repository/`. But `BookClubRemoteDataSource` is in `sync/data/`.

**Why it happened**: The three methods needed (`getClubsCreatedByUser`, `getClubMembershipsForUser`, `removeUserFromClub`) don't exist on any domain-layer interface. The existing `ClubOperations` domain interface has `deleteBookClub` and `clearAllMemberships` but not the discovery/removal methods.

**Options**:
1. Add the three methods to `ClubOperations` (domain interface) and implement in `ClubOperationsImpl`. Remove `BookClubRemoteDataSource` dependency from the use case.
2. Create a new domain interface (e.g., `ClubCleanupOperations`) with just these three methods.
3. Move `BookClubRemoteDataSource` to domain layer — but it exposes DTOs, so this is worse.

**Recommendation**: Option 1. `ClubOperations` already has `deleteBookClub` and `clearAllMemberships`. Adding `getClubsCreatedByUser`, `getClubMembershipsForUser`, and `removeUserFromClub` keeps the "things the app needs from clubs" contract cohesive. The impl delegates to `BookClubRemoteDataSource` internally.

**Impact**: `DeleteAccountUseCaseImpl` constructor drops `bookClubRemoteDataSource` parameter. DI wiring in `AuthModule` simplifies. Tests simplify (no more `StubBookClubRemoteDataSource`).

---

### Issue 4: Re-auth dialog flashes before Google picker covers it

**Severity**: UX polish

**Location**: `ProfileScreen.kt` lines 256-272

**Problem**: When `showReAuthDialog` becomes true, the dialog renders AND `LaunchedEffect(Unit)` immediately triggers `onReAuthFetchCredential()`, which launches the Google credential picker. The user sees the dialog text ("For security, please sign in again...") for a fraction of a second before the picker covers it.

The dialog serves no purpose if it's immediately covered.

**Options**:
1. Remove the dialog entirely — trigger the picker directly when `REQUIRES_RECENT_LOGIN` is received, skip the dialog state
2. Keep the dialog with a confirm button — user taps "Sign In Again" to trigger the picker
3. Keep as-is — the Google picker is self-explanatory

**Recommendation**: Option 1. The dialog adds a UI step that communicates nothing. Remove `showReAuthDialog` from state, trigger the credential fetch directly in the Root's `LaunchedEffect` when the ViewModel signals re-auth is needed. Add a new one-shot state field like `requestReAuth: Boolean` that the Root observes.

---

### Issue 5: Re-auth credential fetch failure silently dismisses

**Severity**: UX bug

**Location**: `ProfileScreen.kt` lines 86-94

**Problem**: In `ProfileScreenRoot`, when `credentialFetcher.fetch()` returns `Result.Error`, the code dispatches `ProfileAction.DismissReAuth`. The re-auth dialog disappears with no feedback. The user is back on the profile screen with no error message and no indication of what happened or that they can retry.

**Fix**: On credential fetch failure, dispatch an action that sets an error message instead of silently dismissing. Could map the credential error to a user-facing message like "Sign-in was cancelled" or "Failed to authenticate."

**Recommendation**: Add a `ProfileAction.OnReAuthFailed` action that sets `errorMessage` and clears `showReAuthDialog`. Or reuse the existing error handling — set `errorMessage` from the credential error.

---

### Issue 6: `deleteAllRemoteData` doesn't delete the `users/{uid}` doc itself

**Severity**: Cosmetic / minor

**Location**: `SyncRepositoryImpl.deleteAllRemoteData()` (lines 303-326)

**Problem**: Deletes books, shelves, and preferences subcollections but not the `users/{uid}` parent document. This leaves a potentially empty document in Firestore.

**Reality check**: In Firestore, parent documents are not required for subcollections to exist. The `users/{uid}` document may never have been explicitly created — Firestore auto-creates the path when subcollection docs are written. After deleting all subcollection docs, the parent doc may or may not exist.

**Recommendation**: Add a final `firestore.collection("users").document(userId).delete().await()` at the end of `deleteAllRemoteData`. One extra call, full cleanup. But verify this doesn't affect other code that checks for the user doc's existence.

---

### Issue 7: Same `onProfileClick` name at two levels with different signatures

**Severity**: Readability

**Location**: `BookcaseScreenRoot` (line 74) vs `BookcaseScreen` (line 201)

**Problem**: `BookcaseScreenRoot` has `onProfileClick: (Boolean) -> Unit` (receives `isSignedIn`), while `BookcaseScreen` has `onProfileClick: () -> Unit` (no args). The Root wraps it: `onProfileClick = { onProfileClick(state.isSignedIn) }`. Same name, different types — confusing when reading.

**Recommendation**: Rename the Screen's callback to `onProfileIconClick: () -> Unit` to distinguish it from the Root's navigation callback. Small change, clearer intent.

---

### Issue 8: `MockAuthService.configureSignedIn` doesn't set email

**Severity**: Test gap

**Location**: `testutil/mocks/MockAuthService.kt` line 46

**Problem**: `configureSignedIn` creates `UserData(userId, username, profilePictureUrl = null)` without email. Tests using this mock won't have email in the user data.

**Fix**: Add `email` parameter with default: `configureSignedIn(userId, username, email = "test@example.com")`

---

### Issue 9: Unnecessary `@Suppress("TooGenericExceptionCaught")` annotations

**Severity**: Cleanup

**Location**: `DeleteAccountUseCaseImpl.kt` lines 29, 85

**Problem**: Both `invoke()` and `retryAfterReAuth()` have `@Suppress("TooGenericExceptionCaught")` but neither method has a `catch` block. The `invoke()` method has a `try/catch(Exception)` around `waitForSyncIdle()`, but the suppress annotation is on the method, not the try block. `retryAfterReAuth` has no try/catch at all.

**Fix**: Remove `"TooGenericExceptionCaught"` from both suppressions. Keep `"ReturnCount"`. For the try/catch in `invoke()`, either add `@Suppress` to the try block itself, or restructure `waitForSyncIdle` to catch internally (it already uses `withTimeoutOrNull` which doesn't throw).

---

### Issue 10: `BookcaseViewModel` over-injects via full `AuthUseCases`

**Severity**: Design / low priority

**Location**: `BookcaseViewModel.kt` line 33

**Problem**: After removing sign-out, `BookcaseViewModel` only calls `checkSignInStatus()` and `getCurrentUserId()` from `AuthUseCases`. But it receives the full aggregator which now includes `signIn`, `signOut`, `deleteAccount`, and `getSignedInUser` — 4 unused dependencies.

**Context**: This is an existing pattern. The `AuthUseCases` aggregator was designed for ViewModels that need multiple auth operations. Before this change, BookcaseViewModel used 3 of 4. Now it uses 2 of 6. The ratio got worse.

**Options**:
1. Inject the two individual use cases instead of the aggregator
2. Leave as-is — it's the existing pattern and changing it is churn

**Recommendation**: Leave as-is. The aggregator pattern is established. Splitting for BookcaseViewModel alone would be inconsistent. If the pattern becomes a problem across the app, refactor all ViewModels at once.

---

## Suggested Fix Order

1. **Issue 9** — Remove unnecessary suppressions (trivial, no behavior change)
2. **Issue 8** — Fix mock email (trivial, improves test fidelity)
3. **Issue 5** — Fix silent re-auth failure (UX bug, small change)
4. **Issue 1** — Filter created clubs from membership list (minor bug, one-line fix)
5. **Issue 3** — Move club cleanup methods to `ClubOperations` domain interface (architecture fix, moderate effort)
6. **Issue 4** — Simplify re-auth flow (UX polish, tied to issue 5)
7. **Issue 7** — Rename `onProfileClick` in Screen (readability, trivial)
8. **Issue 6** — Delete parent user doc (cosmetic, verify no side effects)
9. **Issue 2** — Decide on comment deletion policy (document decision)
10. **Issue 10** — Skip (accepted tech debt)

## Verification

After fixes:
- `./gradlew assembleDebug` passes
- `./gradlew test` passes
- `./gradlew detekt` passes
- Existing manual test scenarios from the original handover still work
