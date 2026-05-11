# Plan: Post Sync-Removal Cleanup

## Context

Code review of the sync removal refactor (8 commits, 144 files) surfaced 5 issues. We're pre-release but targeting production soon, which elevates the `deleteUserDocument` concern from "clean up manually" to a data-deletion compliance risk. All issues are in committed-but-not-pushed code on `main`.

---

## Issue 1 + Issue 4: Harden `deleteUserDocument` in account deletion

### Problem

Two related gaps in `DeleteAccountUseCaseImpl`:

1. **`invoke()` line 47-49**: If `deleteUserDocument()` fails, it logs a warning and proceeds to `authService.deleteAccount()`. If auth deletion succeeds, the Firestore user document is orphaned forever — the user can't delete it (auth gone, rules require auth). This is a data-deletion compliance risk for production (GDPR, Google Play requirements).

2. **`retryAfterReAuth()` line 61-76**: Only does re-auth → delete auth → local cleanup. Does **not** call `deleteUserDocument()`. If `invoke()` previously failed at `deleteUserDocument` but `authService.deleteAccount()` then returned `REQUIRES_RECENT_LOGIN`, the retry path orphans the document.

### Idempotency analysis (verified)

The `invoke()` retry-after-failure flow is safe because input lists are **re-fetched from Firestore on each call**, not cached:
- `getClubsCreatedByUser(userId)` queries Firestore for clubs with `created_by == userId` — already-deleted clubs won't appear
- `getClubMembershipsForUser(userId)` reads from user's settings document — already-removed memberships won't appear
- `removeUserFromClub()` is fully idempotent (handles non-existent club, member doc, array entries)

Note: `deleteBookClub()` itself is NOT idempotent (checks existence first, returns `CLUB_NOT_FOUND`), but it's never called for already-deleted clubs because the query that feeds the loop won't include them.

### Changes

**`account/domain/usecase/DeleteAccountUseCaseImpl.kt`**

Make `deleteUserDocument` failure a hard error in `invoke()`:

```kotlin
// Before (line 46-49)
val deleteDocResult = clubOperations.deleteUserDocument(userId)
if (deleteDocResult is Result.Error) {
    Timber.tag(TAG).w("Failed to delete user document: %s", deleteDocResult.error)
}

// After
val deleteDocResult = clubOperations.deleteUserDocument(userId)
if (deleteDocResult is Result.Error) return deleteDocResult
```

Add `deleteUserDocument` to `retryAfterReAuth()`:

```kotlin
override suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError> {
    val userId = currentUserProvider.getCurrentUserId()
        ?: return Result.Error(DataError.Local.AUTH_FAILED)

    val reAuthResult = authService.reauthenticate(idToken)
    if (reAuthResult is Result.Error) return reAuthResult

    // Retry user document deletion (may have failed in initial invoke())
    val deleteDocResult = clubOperations.deleteUserDocument(userId)
    if (deleteDocResult is Result.Error) return deleteDocResult

    val deleteResult = authService.deleteAccount()
    if (deleteResult is Result.Error) return deleteResult

    finalizeLocalCleanup(userId)
    return Result.Success(Unit)
}
```

**Why hard error in both paths:** After re-auth, the user has fresh credentials. If Firestore deletion still fails, something is genuinely wrong (rules misconfigured, Firestore outage). Don't proceed to delete auth and orphan the data.

---

## Issue 2: Dead `BookClubDao` sync methods

### Problem

Two methods in `BookClubDao.kt` are never called anywhere:
- `updateMembershipSyncStatus()` (line 33-36) — zero callers
- `getPendingSyncMemberships()` (line 56-57) — zero callers

Additionally, `observeAllMemberships()` (line 24) filters `WHERE syncStatus != 'DELETED'`, but nothing ever sets a membership to `DELETED` status — memberships are hard-deleted via `deleteMembership()`. The filter is dead logic. The destructive migration makes phantom-data risk moot.

### Changes

**`core/data/database/dao/BookClubDao.kt`**

1. Delete `updateMembershipSyncStatus()` method entirely
2. Delete `getPendingSyncMemberships()` method entirely  
3. Delete the `// ========== Sync Queries ==========` section header
4. Simplify `observeAllMemberships()` query:

```kotlin
// Before
@Query("SELECT * FROM book_club_memberships WHERE syncStatus != 'DELETED'")
fun observeAllMemberships(): Flow<List<BookClubMembershipEntity>>

// After
@Query("SELECT * FROM book_club_memberships")
fun observeAllMemberships(): Flow<List<BookClubMembershipEntity>>
```

---

## Issue 3: Vestigial `syncStatus` and `lastSyncedAt` on `BookClubMembershipEntity`

### Problem

`BookClubMembershipEntity` has two dead fields:
- `syncStatus` (default `"PENDING"`) — hardcoded to `"SYNCED"` in mapper, never read or updated by any logic
- `lastSyncedAt` — **written** in 4 places but **never read** for any conditional, query, or display. Pure write-only dead weight.

`lastSyncedAt` write sites (all become no-ops after removal):
- `BookClubMembershipRepositoryImpl.kt:115` (join)
- `BookClubMembershipRepositoryImpl.kt:203` (restore)
- `BookClubManagementRepositoryImpl.kt:200` (create)
- `BookClubSyncRepositoryImpl.kt:175` (after club book sync — updates entity directly)

### Changes

**`core/data/database/entity/BookClubMembershipEntity.kt`** — Remove both fields:

```kotlin
data class BookClubMembershipEntity(
    @PrimaryKey val id: String,
    val clubCode: String,
    val localShelfId: String,
    val joinedAt: Long,
)
```

**`bookclub/domain/model/BookClubMembership.kt`** — Remove `lastSyncedAt`:

```kotlin
data class BookClubMembership(
    val clubCode: String,
    val localShelfId: String,
    val joinedAt: Long,
)
```

**`bookclub/data/mappers/BookClubMappers.kt`** — Update both mappers:

```kotlin
fun BookClubMembership.toMembershipEntity(id: String): BookClubMembershipEntity = BookClubMembershipEntity(
    id = id,
    clubCode = clubCode,
    localShelfId = localShelfId,
    joinedAt = joinedAt,
)

fun BookClubMembershipEntity.toMembership(): BookClubMembership = BookClubMembership(
    clubCode = clubCode,
    localShelfId = localShelfId,
    joinedAt = joinedAt,
)
```

**Ripple — source callers constructing `BookClubMembership` with `lastSyncedAt`:**
- `BookClubMembershipRepositoryImpl.kt:115` — remove `lastSyncedAt = now`
- `BookClubMembershipRepositoryImpl.kt:203` — remove `lastSyncedAt = now`
- `BookClubManagementRepositoryImpl.kt:200` — remove `lastSyncedAt = now`

**Ripple — entity direct write:**
- `BookClubSyncRepositoryImpl.kt:172-177` — delete the "Update last synced timestamp" block (getMembershipByClubCode + copy + upsert). The field no longer exists.

**`core/data/database/MyBookshelfRoomDatabase.kt`** — Bump version 1 → 2 (destructive migration already in place via `fallbackToDestructiveMigration`).

**Schema export** — Room will auto-regenerate `app/schemas/.../2.json`. Commit this file.

**Ripple — test callers constructing `BookClubMembership` with `lastSyncedAt`:**
- `CreateBookClubUseCaseImplTest.kt:130, 151` — remove `lastSyncedAt = 1000L`
- `ValidateBookClubMembershipsUseCaseImplTest.kt:79, 122, 149, 155, 200, 225` — remove `lastSyncedAt = currentTime`

No integration test infrastructure hardcodes the DB version — `DatabaseFactory` uses `fallbackToDestructiveMigration`, and in-memory test DBs auto-create at current version.

---

## Issue 5: Test gaps

### Tests for Issue 1+4 (`DeleteAccountUseCaseTest.kt`)

The `mockClubOperations` anonymous object (line 49-81) needs:
- A `deleteUserDocumentResult` field (currently hardcoded to `Result.Success(Unit)` on line 80)
- A `deleteUserDocumentCalledWithUserId` tracker

**New tests:**

1. `invoke - deleteUserDocument fails - returns error, auth NOT deleted`
   - Set `deleteUserDocumentResult = Result.Error(DataError.Sync.NETWORK_ERROR)`
   - Assert result is error, `mockAuthService.deleteAccountCalled` is false

2. `retryAfterReAuth - calls deleteUserDocument before auth deletion`
   - Track that `deleteUserDocument` was called with correct userId
   - Assert auth is deleted after

3. `retryAfterReAuth - deleteUserDocument fails - returns error, auth NOT deleted`
   - Set `deleteUserDocumentResult = Result.Error(DataError.Sync.NETWORK_ERROR)`
   - Call `retryAfterReAuth("fresh-token")`
   - Assert auth not deleted

4. `invoke - retry after partial club cleanup succeeds` (validates query-level idempotency)
   - First call: `clubsCreatedByUser = listOf("club-a")`, `deleteUserDocumentResult = Error`
   - Assert first call fails
   - Second call: `clubsCreatedByUser = emptyList()` (club-a already deleted from Firestore), `deleteUserDocumentResult = Success`
   - Assert second call succeeds, auth deleted
   - This validates the retry safety claim: re-querying Firestore skips already-deleted clubs

---

## Commit Strategy

Single commit — all issues are tightly coupled (entity change triggers migration bump, test changes cover code changes):

```
fix(account): harden account deletion and clean up membership entity

- Make deleteUserDocument failure block auth deletion (data compliance)
- Add deleteUserDocument to retryAfterReAuth path
- Remove vestigial syncStatus/lastSyncedAt from BookClubMembershipEntity
- Remove dead BookClubDao sync methods
- Add missing test coverage for deletion edge cases
```

---

## Verification

1. `./gradlew test` — all unit tests pass
2. Grep for `syncStatus` in non-test, non-plan `.kt` files — zero hits
3. Grep for `lastSyncedAt` in non-test, non-plan `.kt` files — zero hits
4. Grep for `getPendingSyncMemberships|updateMembershipSyncStatus` — only in plan docs
5. Manual: account deletion flow (success + re-auth retry + failure-then-retry)
6. Verify `app/schemas/.../2.json` is generated and committed
