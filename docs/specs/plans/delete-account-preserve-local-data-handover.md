# Handover: Delete Account — Preserve Local Data

## What This Is

Fix the delete account flow so local shelves/books are preserved as guest data after account deletion. Currently, local data becomes invisible because it has `ownerId = "firebase-uid"` and after deletion `getCurrentUserId()` returns `null`.

The full reviewed plan is at `docs/specs/plans/delete-account-preserve-local-data.md` — read it first. This document tells you how to execute it.

## Branch

Continue on `profile-page`.

## Before You Start

Read these specs — the code must comply:

| Spec | Why |
|------|-----|
| `docs/specs/constitution.md` | Domain purity, error handling, dependency direction |
| `docs/specs/patterns/usecase.md` | UseCase pattern, sync decorator section |
| `docs/specs/patterns/repository.md` | Repository implementation pattern |
| `docs/specs/plans/delete-account-preserve-local-data.md` | The reviewed plan — all design decisions are made |

Read these existing files for reference patterns:

| File | Why |
|------|-----|
| `book/data/repository/BookcaseRepositoryImpl.kt:85-102` | `clearUserData` — the mirror pattern for delete; revert follows same structure |
| `sync/data/repository/SyncRepositoryImpl.kt:216-260` | `migrateOrphanData` — the reverse operation (guest → user) |
| `core/data/database/dao/ShelfDao.kt:53` | Existing `assignOrphanShelvesToUser` — the pattern to mirror |
| `core/data/database/dao/BookDao.kt:30` | Existing `assignOrphanBooksToUser` — the pattern to mirror |
| `account/domain/usecase/DeleteAccountUseCaseImpl.kt` | Current flow — you're modifying this |
| `auth/domain/usecase/CheckSignInStatusUseCaseImpl.kt` | Where startup recovery check goes |
| `testutil/mocks/MockBookcaseRepository.kt` | Mock to update |
| `account/domain/usecase/DeleteAccountUseCaseTest.kt` | Tests to update |

## Key Decisions (Already Reviewed — Don't Revisit)

- **Revert AFTER auth deletion, not before** — prevents inconsistent state if auth deletion fails
- **`revertUserDataToGuest` on `BookcaseRepository`** — mirrors `clearUserData`, same SRP scope
- **`@Transaction` on `BookshelfDao`** — wraps cross-ref reset + book revert + shelf revert atomically. Room 2.8.4 supports `@Transaction` on interface default methods
- **CrossRef syncStatus must be reset** — prevents sync engine from pushing ghost data
- **CrossRef reset runs BEFORE shelf revert** — the WHERE clause references `ownerId` on shelves
- **`syncStatus = 'SYNCED'`** — pragmatic choice to prevent sync pushes. Not semantically perfect but functionally correct
- **`finalizeLocalCleanup` logs errors but doesn't fail** — auth is already gone, these are local-only ops
- **`clearSyncData` runs even if revert fails** — no short-circuit in the `when` block
- **Startup recovery in `CheckSignInStatusUseCaseImpl`** — catches process-kill between auth deletion and revert
- **False positive risk documented** — recovery gated by `!isSignedIn` which requires `firebaseUser == null`. Firebase should be initialized by the time `SignInViewModel.init` runs. Add code comment.

## Implementation Order

Work in this order. Each step should compile. Commit after each phase.

---

### Phase 1: Add DAO Methods (3 files)

**Step 1 — `ShelfDao.kt`**

Add two methods:
```kotlin
@Query("UPDATE BookshelfEntity SET ownerId = NULL, syncStatus = 'SYNCED' WHERE ownerId = :userId")
suspend fun revertShelvesToGuest(userId: String)

@Query("""
    SELECT DISTINCT ownerId FROM BookshelfEntity 
    WHERE ownerId IS NOT NULL AND ownerId != '__system_tutorial__'
    LIMIT 1
""")
suspend fun findOrphanedOwnerId(): String?
```

**Step 2 — `BookDao.kt`**

Add:
```kotlin
@Query("UPDATE BookEntity SET ownerId = NULL, syncStatus = 'SYNCED' WHERE ownerId = :userId")
suspend fun revertBooksToGuest(userId: String)
```

**Step 3 — `CrossRefDao.kt`**

Add:
```kotlin
@Query("""
    UPDATE BookshelfBookCrossRef SET syncStatus = 'SYNCED' 
    WHERE shelfId IN (SELECT id FROM BookshelfEntity WHERE ownerId = :userId)
""")
suspend fun resetCrossRefSyncStatusForOwner(userId: String)
```

**Step 4 — `BookshelfDao.kt`**

Add two `@Transaction` default methods:
```kotlin
@Transaction
suspend fun revertAllUserDataToGuest(userId: String) {
    resetCrossRefSyncStatusForOwner(userId)
    revertBooksToGuest(userId)
    revertShelvesToGuest(userId)
}

@Transaction
suspend fun revertOrphanedDataToGuest(): Boolean {
    val orphanedUserId = findOrphanedOwnerId() ?: return false
    revertAllUserDataToGuest(orphanedUserId)
    return true
}
```

**Compile check:** `./gradlew assembleDebug`

---

### Phase 2: Add Repository Methods (2 files)

**Step 5 — `BookcaseRepository.kt` (interface)**

Add:
```kotlin
suspend fun revertUserDataToGuest(userId: String): Result<Unit, DataError.Local>
suspend fun revertOrphanedDataToGuest(): Result<Unit, DataError.Local>
```

**Step 6 — `BookcaseRepositoryImpl.kt`**

Add implementations using `ErrorMapper.safeSuspendCall(TAG)`:
```kotlin
override suspend fun revertUserDataToGuest(userId: String): Result<Unit, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        dao.revertAllUserDataToGuest(userId)
    }
}

override suspend fun revertOrphanedDataToGuest(): Result<Unit, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        dao.revertOrphanedDataToGuest()
    }
}
```

**Step 7 — Update mocks**

`MockBookcaseRepository.kt` — add tracking fields and implementations for both new methods.

**Compile check:** `./gradlew assembleDebug`

---

### Phase 3: Update DeleteAccountUseCaseImpl (1 file)

**Step 8 — `DeleteAccountUseCaseImpl.kt`**

1. Add constructor params: `bookcaseRepository: BookcaseRepository`, `authStateRepository: AuthStateRepository`
2. Add `finalizeLocalCleanup(userId)` private method (logs revert errors, always runs clearSyncData + setSignedInState)
3. In `invoke()`: capture `userId` at start, call `finalizeLocalCleanup(userId)` after successful `authService.deleteAccount()`
4. In `retryAfterReAuth()`: capture `userId`, call `finalizeLocalCleanup(userId)` after successful auth deletion
5. Add `companion object { private const val TAG = "DeleteAccount" }` if not present

**Compile check:** `./gradlew assembleDebug`

---

### Phase 4: Add Startup Recovery (1 file)

**Step 9 — `CheckSignInStatusUseCaseImpl.kt`**

1. Add `bookcaseRepository: BookcaseRepository` constructor param
2. After determining `!isSignedIn`, call `bookcaseRepository.revertOrphanedDataToGuest()`
3. Add code comment documenting the Firebase initialization assumption

**Compile check:** `./gradlew assembleDebug`

---

### Phase 5: Tests (2 files)

**Step 10 — `DeleteAccountUseCaseTest.kt`**

Update constructor with new params. Add test cases:
- `invoke - success - reverts local data to guest`
- `invoke - success - sets auth state to false`
- `invoke - success - clears sync data`
- `invoke - auth fails - does NOT revert local data`
- `invoke - REQUIRES_RECENT_LOGIN - does NOT revert local data`
- `retryAfterReAuth - success - reverts local data to guest`
- `retryAfterReAuth - success - sets auth state to false`
- `invoke - revert fails - still clears sync data and auth state`

**Step 11 — `CheckSignInStatusUseCaseTest.kt`** (if exists, else create)

- `not signed in with orphaned data - reverts to guest`
- `signed in - does not revert`
- `not signed in with no orphaned data - no-op`

**Test check:** `./gradlew test`

---

## Files Quick Reference

### Modified files (10)
| File | Purpose |
|------|---------|
| `core/data/database/dao/ShelfDao.kt` | `revertShelvesToGuest`, `findOrphanedOwnerId` |
| `core/data/database/dao/BookDao.kt` | `revertBooksToGuest` |
| `core/data/database/dao/CrossRefDao.kt` | `resetCrossRefSyncStatusForOwner` |
| `core/data/database/dao/BookshelfDao.kt` | `@Transaction revertAllUserDataToGuest`, `revertOrphanedDataToGuest` |
| `book/domain/repository/BookcaseRepository.kt` | Interface: `revertUserDataToGuest`, `revertOrphanedDataToGuest` |
| `book/data/repository/BookcaseRepositoryImpl.kt` | Implementations |
| `account/domain/usecase/DeleteAccountUseCaseImpl.kt` | Revert + auth state + fix retryAfterReAuth |
| `auth/domain/usecase/CheckSignInStatusUseCaseImpl.kt` | Startup recovery |
| `testutil/mocks/MockBookcaseRepository.kt` | Mock tracking |
| `account/domain/usecase/DeleteAccountUseCaseTest.kt` | Updated + new tests |

### NOT modified
| File | Reason |
|------|--------|
| `AccountModule.kt` | `singleOf` auto-resolves new constructor params |
| `SyncRepository` | `clearSyncData` already exists |
| `DeleteAccountUseCase.kt` (interface) | Signatures unchanged |

## Gotchas

1. **CrossRef reset BEFORE shelf revert** — `resetCrossRefSyncStatusForOwner` uses a subquery `WHERE ownerId = :userId` on shelves. If shelves are reverted first (ownerId set to NULL), the subquery finds nothing.
2. **`retryAfterReAuth` must capture userId BEFORE calling `authService.deleteAccount()`** — after deletion, `getCurrentUserId()` returns null.
3. **`findOrphanedOwnerId` returns the first non-null, non-system ownerId** — assumes only one user's data exists locally at a time. This is correct given the sign-out flow wipes data.
4. **Don't add `BookcaseRepository` import to `CheckSignInStatusUseCaseImpl`'s package** — it's in `auth/domain/usecase/`, which depends on `book/domain/`. Check dependency direction is correct (`auth` → `book` is fine per CLAUDE.md).

## Verification Checklist

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew detekt` passes
- [ ] `./gradlew test` passes
- [ ] On-device (debug): sign in → create shelf → delete account → shelf visible as guest
- [ ] On-device (debug): after deletion → sign in again → guest data import dialog appears
- [ ] On-device (debug): verify clubs are deleted from Firestore emulator
- [ ] On-device (debug): verify user's Firestore data (books/shelves/preferences) deleted
- [ ] `retryAfterReAuth` path tested if REQUIRES_RECENT_LOGIN can be triggered
