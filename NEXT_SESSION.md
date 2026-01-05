# Next Session: Book Club Cleanup & Name Uniqueness

## Problem Summary

Book club shelves are not being properly cleaned up from local Room database:
1. Book club shelves marked as `syncStatus = 'DELETED'` but not hard deleted
2. BookshelfBookCrossRef records not cleaned up for book clubs
3. `getShelfByName()` finds "ghost" shelves causing false duplicate detection

**Note**: Personal shelf deletion works correctly - this is book club specific.

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
  - `cleanupLocalClubData()` - ensure it hard deletes (already does!)
  - `convertClubToPersonalShelf()` - only removes BC properties, keeps shelf (correct)

**Current issue**: Some code paths use soft delete instead of calling `cleanupLocalClubData()` which does hard delete.

**Pattern**:
```kotlin
// Use hard delete (what cleanupLocalClubData does):
dao.deleteAllCrossRefsForShelf(shelfId)  // Clean cross-refs first
dao.deleteShelf(shelfId)                  // Then delete shelf
bookClubDao.deleteMembership(code)        // Clean membership record
```

---

### 3. Clean Up Cross-References

**Goal**: Ensure BookshelfBookCrossRef records are cleaned when book club shelves are deleted.

**Check**: Is `deleteAllCrossRefsForShelf(shelfId)` being called before `deleteShelf()`?

---

## Test Plan

After changes:
1. Create book club, member joins
2. Delete book club → verify local shelf AND cross-refs are gone from Room
3. Leave book club → verify local shelf AND cross-refs are gone from Room
4. Convert to personal → verify shelf exists but BC properties removed
5. Create two shelves with same name → should work
6. Check Room database directly - no ghost DELETED records

---

## Files Quick Reference

```
Data Layer:
- BookClubRepositoryImpl.kt - book club CRUD (main focus)
- BookcaseRepositoryImpl.kt - personal shelf CRUD (working fine)
- BookshelfDao.kt - Room queries

Use Cases:
- CreateShelfUseCaseImpl.kt
- RenameShelfUseCaseImpl.kt
- DuplicateShelfUseCaseImpl.kt
```
