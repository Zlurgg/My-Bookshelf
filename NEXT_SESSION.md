# Next Session: Database Cleanup & Name Uniqueness

## Problem Summary

Local Room database is not properly cleaning up deleted data:
1. Shelves marked as `syncStatus = 'DELETED'` but not hard deleted
2. BookshelfBookCrossRef records not cleaned up
3. `getShelfByName()` finds "ghost" shelves causing false duplicate detection
4. Data accumulates indefinitely in local database

## Tasks

### 1. Remove Shelf Name Uniqueness Checks

**Goal**: Allow users to have multiple shelves with the same name (their choice).

**Files to check/modify**:
- [ ] `BookClubRepositoryImpl.kt` - `generateUniqueShelfName()` function
- [ ] `CreateShelfUseCaseImpl.kt` - any name validation
- [ ] `RenameShelfUseCaseImpl.kt` - any name validation
- [ ] `DuplicateShelfUseCaseImpl.kt` - name generation for duplicates
- [ ] UI components - any name validation/error messages
- [ ] `BookshelfDao.kt` - `getShelfByName()` may no longer be needed

**Changes**:
- Remove or simplify `generateUniqueShelfName()` - just use the name directly
- Remove duplicate name error handling from create/rename flows
- Keep `getShelfByName()` only if needed for other purposes (like share code lookup)

---

### 2. Hard Delete for Book Clubs

**Goal**: When leaving/deleting book clubs, hard delete local data since Firestore is source of truth.

**Files to modify**:
- [ ] `BookClubRepositoryImpl.kt`:
  - `leaveBookClub()` - use hard delete
  - `deleteBookClub()` - use hard delete
  - `cleanupLocalClubData()` - ensure it hard deletes
  - `convertClubToPersonalShelf()` - only removes BC properties, keeps shelf

**Pattern**:
```kotlin
// Instead of soft delete:
dao.updateShelfSyncStatus(shelfId, "DELETED", timestamp)

// Use hard delete:
dao.deleteAllCrossRefsForShelf(shelfId)
dao.deleteShelf(shelfId)
```

---

### 3. Review Personal Shelf Deletion Flow

**Current flow**:
1. `BookcaseRepositoryImpl.removeShelf()` marks as DELETED (soft delete)
2. SyncEngine should push delete to Firestore
3. SyncEngine should hard delete after cloud confirmation

**Question**: Is step 3 actually happening?

**Files to check**:
- [ ] `SyncEngine.kt` or equivalent - find the sync implementation
- [ ] Look for cleanup logic after successful cloud sync

**Options**:
- **A**: Fix SyncEngine to hard delete after successful cloud sync
- **B**: Add periodic cleanup job for DELETED records older than X days
- **C**: Hard delete immediately if user is signed out (no sync needed)

---

### 4. Clean Up Cross-References

**Goal**: Ensure BookshelfBookCrossRef records are cleaned when shelves are deleted.

**Current behavior**:
- `deleteAllCrossRefsForShelf(shelfId)` exists in DAO
- But is it being called consistently?

**Files to check**:
- [ ] All places that delete shelves - ensure cross-refs are cleaned

---

## Questions to Answer

1. Do we sync personal shelf data to Firestore for backup?
2. If yes, is there a cleanup step after successful cloud delete?
3. If no sync, should personal shelves just hard delete immediately?

---

## Test Plan

After changes:
1. Create book club, member joins
2. Delete book club → verify local shelf AND cross-refs are gone
3. Leave book club → verify local shelf AND cross-refs are gone
4. Convert to personal → verify shelf exists but BC properties removed
5. Create two shelves with same name → should work
6. Delete personal shelf → verify it's actually gone from database

---

## Files Quick Reference

```
Data Layer:
- BookClubRepositoryImpl.kt - book club CRUD
- BookcaseRepositoryImpl.kt - personal shelf CRUD
- BookshelfDao.kt - Room queries

Use Cases:
- DeleteShelfUseCaseImpl.kt
- CreateShelfUseCaseImpl.kt
- RenameShelfUseCaseImpl.kt
- DuplicateShelfUseCaseImpl.kt
- LeaveBookClubUseCaseImpl.kt

Sync:
- SyncEngine.kt (or equivalent) - need to find this
```
