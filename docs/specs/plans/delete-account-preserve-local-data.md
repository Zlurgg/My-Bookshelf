# Plan: Fix Delete Account to Preserve Local Data

## Context

When a user deletes their account, remote data and auth are correctly deleted. But local shelves/books become invisible because they have `ownerId = "firebase-uid"` and after deletion `getCurrentUserId()` returns `null`. The DAO query (`WHERE ownerId IS NULL OR ownerId = :userId`) no longer matches.

**Desired behavior:** After account deletion, user drops to guest mode with all their shelves/books intact. They can continue as guest or sign in again and re-adopt the data via the existing guest data import dialog.

## Approach: Revert local data to guest AFTER auth deletion

Reassign the user's local data ownership from their Firebase UID to `null` (guest). This uses the existing guest data model — the mirror of `migrateOrphanData` (guest → user) that already exists.

**Critical ordering:** The revert happens AFTER successful auth deletion, not before. This prevents an inconsistent state where the user is signed in with guest-owned data if auth deletion fails.

### Flow

```
1. Cancel sync                              (existing)
2. Capture userId                            (NEW — before auth deletion nukes currentUser)
3. Delete clubs created by user              (existing)
4. Remove user from club memberships         (existing)  
5. Delete all remote Firestore data          (existing)
6. Delete Firebase Auth account              (existing)
7. ON SUCCESS:
   a. Revert local data to guest             (NEW)
   b. Clear sync metadata                    (NEW)
   c. Set auth state to signed-out           (NEW)
8. ON REQUIRES_RECENT_LOGIN:
   - Return error (local data untouched, user still signed in)
   - retryAfterReAuth: re-auth → delete account → revert + cleanup
```

### Changes

**1. Add DAO methods** — `ShelfDao.kt`, `BookDao.kt`, `CrossRefDao.kt`

```kotlin
// ShelfDao
@Query("UPDATE BookshelfEntity SET ownerId = NULL, syncStatus = 'SYNCED' WHERE ownerId = :userId")
suspend fun revertShelvesToGuest(userId: String)

// BookDao
@Query("UPDATE BookEntity SET ownerId = NULL, syncStatus = 'SYNCED' WHERE ownerId = :userId")
suspend fun revertBooksToGuest(userId: String)

// CrossRefDao — reset pending cross-refs for user's shelves
@Query("""
    UPDATE BookshelfBookCrossRef SET syncStatus = 'SYNCED' 
    WHERE shelfId IN (SELECT id FROM BookshelfEntity WHERE ownerId = :userId)
""")
suspend fun resetCrossRefSyncStatusForOwner(userId: String)
```

Note: `syncStatus = 'SYNCED'` prevents the sync engine from trying to push this data. It's a pragmatic choice — these were once synced as user data, now they're local-only guest data. A `LOCAL_ONLY` status would be more semantic but adds complexity for no functional gain.

Note: CrossRef reset must run BEFORE shelf revert (the WHERE clause references ownerId).

**2. Add `revertUserDataToGuest` to `BookcaseRepository`**

This is the mirror of `clearUserData` — same repository, same scope (shelves + books + cross-refs), but reverts instead of deletes.

Interface method on `BookcaseRepository`:
```kotlin
suspend fun revertUserDataToGuest(userId: String): Result<Unit, DataError.Local>
```

Implementation in `BookcaseRepositoryImpl` — wrapped in `@Transaction` via a new `BookshelfDao` method:
```kotlin
// BookshelfDao (combines all three DAOs)
@Transaction
suspend fun revertAllUserDataToGuest(userId: String) {
    resetCrossRefSyncStatusForOwner(userId)  // Must run first (references ownerId)
    revertBooksToGuest(userId)
    revertShelvesToGuest(userId)
}
```

The `@Transaction` ensures atomicity — no partial revert on crash.

**3. Update `DeleteAccountUseCaseImpl`**

Add dependencies: `BookcaseRepository`, `AuthStateRepository`, inject `SyncRepository` for `clearSyncData`.

```kotlin
class DeleteAccountUseCaseImpl(
    private val currentUserProvider: CurrentUserProvider,
    private val syncScheduler: SyncSchedulerService,
    private val syncRepository: SyncRepository,
    private val clubOperations: ClubOperations,
    private val authService: AuthService,
    private val bookcaseRepository: BookcaseRepository,      // NEW
    private val authStateRepository: AuthStateRepository,    // NEW
) : DeleteAccountUseCase {

    override suspend operator fun invoke(): Result<Unit, DataError> {
        val userId = currentUserProvider.getCurrentUserId()
            ?: return Result.Error(DataError.Local.AUTH_FAILED)

        syncScheduler.cancelAllSync()

        // ... existing club cleanup + remote data deletion ...

        // Delete Firebase Auth account
        val deleteResult = authService.deleteAccount()
        if (deleteResult is Result.Error) return deleteResult

        // Auth succeeded — now safe to revert local data
        finalizeLocalCleanup(userId)

        return Result.Success(Unit)
    }

    override suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError> {
        val userId = currentUserProvider.getCurrentUserId()
            ?: return Result.Error(DataError.Local.AUTH_FAILED)

        val reAuthResult = authService.reauthenticate(idToken)
        if (reAuthResult is Result.Error) return reAuthResult

        val deleteResult = authService.deleteAccount()
        if (deleteResult is Result.Error) return deleteResult

        // Auth succeeded — now safe to revert local data
        finalizeLocalCleanup(userId)

        return Result.Success(Unit)
    }

    private suspend fun finalizeLocalCleanup(userId: String) {
        bookcaseRepository.revertUserDataToGuest(userId)
        syncRepository.clearSyncData(userId)
        authStateRepository.setSignedInState(false)
    }
}
```

**Key design decisions:**
- `retryAfterReAuth` now captures userId and calls `finalizeLocalCleanup` — fixes the broken retry path
- `finalizeLocalCleanup` logs errors but doesn't fail — auth is already gone, these are local-only operations. Errors are logged for observability.
- Local user preferences (DataStore `auth_preferences`) — only stores `SIGNED_IN_KEY` boolean, which `setSignedInState(false)` handles. Sync preferences (`welcomeShown`) are harmless to keep.
- Room 2.8.4 — `@Transaction` on interface default methods is fully supported.

```kotlin
private suspend fun finalizeLocalCleanup(userId: String) {
    when (val result = bookcaseRepository.revertUserDataToGuest(userId)) {
        is Result.Success -> Timber.tag(TAG).d("Local data reverted to guest")
        is Result.Error -> Timber.tag(TAG).e("Failed to revert local data: %s", result.error)
    }
    syncRepository.clearSyncData(userId)
    authStateRepository.setSignedInState(false)
}
```

**4. Startup recovery for crash-safety**

If the process is killed between auth deletion and `finalizeLocalCleanup`, data is orphaned with a UID that no longer exists. Add a lightweight recovery check.

In `CheckSignInStatusUseCaseImpl` (already runs on every app launch):

```kotlin
override suspend operator fun invoke(): Boolean {
    val localState = ...
    val firebaseUser = authService.getSignedInUser()
    val isSignedIn = localState && firebaseUser != null

    // Recovery: if not signed in, revert any orphaned user data to guest
    if (!isSignedIn) {
        bookcaseRepository.revertOrphanedDataToGuest()
    }

    return isSignedIn
}
```

New DAO method + BookcaseRepository method:
```kotlin
// ShelfDao — find non-guest, non-system shelves
@Query("""
    SELECT DISTINCT ownerId FROM BookshelfEntity 
    WHERE ownerId IS NOT NULL AND ownerId != '__system_tutorial__'
    LIMIT 1
""")
suspend fun findOrphanedOwnerId(): String?

// BookshelfDao
@Transaction
suspend fun revertOrphanedDataToGuest(): Boolean {
    val orphanedUserId = findOrphanedOwnerId() ?: return false
    revertAllUserDataToGuest(orphanedUserId)
    return true
}
```

This is a single-query check that returns null in the happy path (no orphans). Only does work if orphans exist. Makes the "revert after auth deletion" approach bulletproof.

**False positive risk:** The check is gated by `!isSignedIn`, which requires `firebaseUser == null`. If Firebase Auth hasn't fully initialized and transiently returns null, the recovery could incorrectly revert a signed-in user's data. In practice, `CheckSignInStatusUseCaseImpl` is called from `SignInViewModel.init`, which runs after Koin initialization and `FirebaseEmulatorConfig` setup — Firebase should be settled by then. Add a code comment documenting this assumption.

**5. Update `DeleteAccountUseCase` interface**

No change needed — `invoke()` and `retryAfterReAuth()` signatures stay the same.

**6. Update tests**

`MockBookcaseRepository` — add `revertUserDataToGuest` and `revertOrphanedDataToGuest` tracking.
`DeleteAccountUseCaseTest` — add:
- Revert called after successful auth deletion
- Revert NOT called when auth deletion fails  
- `retryAfterReAuth` calls revert after success
- Auth state set to false after deletion
- Revert error logged but doesn't fail the use case

`CheckSignInStatusUseCaseTest` (if exists) — add:
- Recovery reverts orphaned data when not signed in
- No revert when signed in

### Files to modify

| File | Change |
|------|--------|
| `core/data/database/dao/ShelfDao.kt` | Add `revertShelvesToGuest`, `findOrphanedOwnerId` |
| `core/data/database/dao/BookDao.kt` | Add `revertBooksToGuest` |
| `core/data/database/dao/CrossRefDao.kt` | Add `resetCrossRefSyncStatusForOwner` |
| `core/data/database/dao/BookshelfDao.kt` | Add `@Transaction revertAllUserDataToGuest`, `revertOrphanedDataToGuest` |
| `book/domain/repository/BookcaseRepository.kt` | Add `revertUserDataToGuest`, `revertOrphanedDataToGuest` |
| `book/data/repository/BookcaseRepositoryImpl.kt` | Implement both revert methods |
| `account/domain/usecase/DeleteAccountUseCaseImpl.kt` | Add revert + auth state, fix retryAfterReAuth |
| `auth/domain/usecase/CheckSignInStatusUseCaseImpl.kt` | Add startup recovery check |
| `testutil/mocks/MockBookcaseRepository.kt` | Add revert mock tracking |
| `account/domain/usecase/DeleteAccountUseCaseTest.kt` | Add revert/ordering/retry/error-logging tests |

### Not changed

- `AccountModule.kt` — uses `singleOf(::DeleteAccountUseCaseImpl)` which auto-resolves new constructor params
- `SyncRepository` — `clearSyncData` already exists, no new methods needed
- Local user preferences — `setSignedInState(false)` is sufficient

## Verification

- `./gradlew assembleDebug` passes
- `./gradlew detekt` passes
- `./gradlew test` passes
- On-device: sign in → create shelf → delete account → see shelf as guest
- On-device: after deletion, sign in again → guest data import dialog appears
- On-device: REQUIRES_RECENT_LOGIN flow → re-auth → account deleted → data preserved as guest
