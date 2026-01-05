# Book Club Feature - Manual Testing Plan

## Overview

This document provides a comprehensive manual testing plan for the Book Club collaborative sharing feature (Phases 1-3).

## What's Implemented

| Phase | Status | Features |
|-------|--------|----------|
| Phase 1: Create | Complete | Create book club from shelf, generate invite code |
| Phase 2: Join | Complete | Join via code/URL, preview before joining |
| Phase 3: Sync | Complete | Add/remove books syncs to all members |

## What's Remaining

| Phase | Status | Features |
|-------|--------|----------|
| Phase 4: Reviews | Not Started | Add rating/review, see other members' reviews |
| Phase 5: Leave & Members | Not Started | View members, leave club, auto-cleanup |

### Deferred Items
- Web page for club preview (`docs/club/index.html`)
- Deep link handling (`mybookshelf://club/{code}`)

---

## Prerequisites

- Two devices or emulators (User A and User B)
- Both signed into different Google accounts
- Fresh app install recommended (or clear app data)
- Firestore console access for verification

---

## Test Cases

### Test 1: Create Book Club (User A)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1.1 | Sign in with Google | Sign-in succeeds, sync icon appears |
| 1.2 | Create a new shelf "Sci-Fi Favorites" | Shelf appears in bookcase |
| 1.3 | Add 2-3 books to the shelf | Books appear on shelf |
| 1.4 | Tap shelf overflow menu (⋮) → "Share" | Share options dialog appears |
| 1.5 | Tap "Create Book Club" | Confirmation dialog appears |
| 1.6 | Confirm creation | Loading indicator, then invite dialog with 8-char code |
| 1.7 | Verify shelf name shows "(Book Club)" suffix | Shelf renamed to "Sci-Fi Favorites (Book Club)" |
| 1.8 | Copy the club code | Code copied to clipboard |
| 1.9 | Check Firestore console | `/bookClubs/{code}/` exists with metadata, members, books |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 2: Join Book Club (User B)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 2.1 | Sign in with Google (different account) | Sign-in succeeds |
| 2.2 | Tap bookcase overflow menu (⋮) | Menu appears |
| 2.3 | Tap "Join Book Club" | Join dialog appears |
| 2.4 | Enter the 8-char code from Test 1 | Code accepted |
| 2.5 | Tap "Join" | Loading indicator, then preview dialog |
| 2.6 | Verify preview shows club name, book count, member count | Correct info displayed |
| 2.7 | Tap "Join Club" | Loading, then success |
| 2.8 | Verify new shelf appears | "Sci-Fi Favorites (Book Club)" shelf created |
| 2.9 | Open the shelf | All books from User A visible |
| 2.10 | Check Firestore console | User B added to `/bookClubs/{code}/members/` |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 3: Add Book Syncs to Club (User A)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 3.1 | User A: Open the book club shelf | Shelf opens |
| 3.2 | Search and add a new book | Book added to shelf |
| 3.3 | Check Firestore console | New book appears in `/bookClubs/{code}/books/` |
| 3.4 | Check `addedBy` field | Shows User A's userId |
| 3.5 | User B: Pull to refresh or reopen shelf | New book appears on User B's shelf |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 4: Remove Book Syncs to Club (User B)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 4.1 | User B: Open the book club shelf | Shelf opens |
| 4.2 | Long-press a book → Remove from shelf | Book removed |
| 4.3 | Check Firestore console | Book document deleted from `/bookClubs/{code}/books/` |
| 4.4 | User A: Pull to refresh or reopen shelf | Book no longer appears |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 5: Duplicate Club Prevention (User A)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 5.1 | On the same book club shelf, tap Share | Share options appear |
| 5.2 | Tap "Create Book Club" again | Should show existing invite link, NOT create new club |
| 5.3 | Verify same club code | Code matches original |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 6: Already Member Prevention (User B)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 6.1 | Open "Join Book Club" dialog | Dialog appears |
| 6.2 | Enter the same club code | Code accepted |
| 6.3 | Tap "Join" | Error: "You're already a member of this book club" |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 7: Invalid Club Code

| Step | Action | Expected Result |
|------|--------|-----------------|
| 7.1 | Open "Join Book Club" dialog | Dialog appears |
| 7.2 | Enter invalid code "XXXXXXXX" | Code accepted (format valid) |
| 7.3 | Tap "Join" | Error: "Book club not found" |
| 7.4 | Enter malformed code "ABC" | Error: "Invalid club code format" |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 8: URL Parsing in Join Dialog

| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.1 | Open "Join Book Club" dialog | Dialog appears |
| 8.2 | Paste full URL: `https://zlurgg.github.io/My-Bookshelf/club/ABC12XYZ` | URL accepted |
| 8.3 | Tap "Join" | Code extracted, join flow proceeds |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 9: Sign-Out Clears Club Data

| Step | Action | Expected Result |
|------|--------|-----------------|
| 9.1 | User B: Note the book club shelf exists | Shelf visible |
| 9.2 | Sign out | Sign-out succeeds |
| 9.3 | Check bookcase | Book club shelf gone (local data cleared) |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 10: Sign-In Restores Club Memberships

| Step | Action | Expected Result |
|------|--------|-----------------|
| 10.1 | User B: Sign back in (same account) | Sign-in succeeds |
| 10.2 | Wait for sync/restore | Loading indicator |
| 10.3 | Check bookcase | Book club shelf restored with all books |
| 10.4 | Check Logcat for "RestoreBookClub" | Logs show restoration process |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 11: Delete Book Club Shelf (Creator)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 11.1 | User A: Long-press book club shelf → Delete | Confirmation dialog |
| 11.2 | Confirm delete | Shelf deleted locally |
| 11.3 | Check Firestore console | `/bookClubs/{code}/` document deleted |
| 11.4 | User B: Refresh | Club shelf should show error or disappear |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

### Test 12: Non-Club Shelf Behavior Unchanged

| Step | Action | Expected Result |
|------|--------|-----------------|
| 12.1 | Create a regular shelf (not book club) | Shelf created |
| 12.2 | Add books | Books added locally only |
| 12.3 | Check Firestore `/bookClubs/` | No new club created |
| 12.4 | Remove books | Books removed locally only |

**Pass:** [ ] **Fail:** [ ] **Notes:** _______________

---

## Edge Cases

| Scenario | Expected Behavior | Pass/Fail |
|----------|-------------------|-----------|
| Create club while offline | Should fail gracefully with network error | [ ] |
| Join club while offline | Should fail gracefully with network error | [ ] |
| Add book while offline | Local add succeeds, Firestore sync fails (logged warning) | [ ] |
| Remove book while offline | Local remove succeeds, Firestore sync fails (logged warning) | [ ] |
| Very long shelf name | Truncated or handled gracefully | [ ] |
| Special characters in shelf name | Handled correctly | [ ] |
| Rapid add/remove of same book | No crashes, consistent state | [ ] |
| Join club you created | Should succeed (you're already a member but from creation) | [ ] |

---

## Debugging

### Logcat Tags

```bash
# All book club related logs
adb logcat -s BookClubSync,BookClubRepo,BookClubCode,CreateBookClub,JoinBookClub,AddBookToShelf,RemoveBookFromShelf,RestoreBookClub

# Sync-specific logs
adb logcat -s SyncTrigger,BookClubSync

# Error logs only
adb logcat *:E | grep -i "bookclub\|club"
```

### Firestore Paths to Check

```
/bookClubs/{code}/
  metadata          → name, style, createdAt, createdBy, bookCount, memberCount
  /members/{id}     → userId, displayName, joinedAt
  /books/{id}       → title, authors, coverUrl, addedBy, addedAt, etc.

/users/{userId}/settings/preferences
  clubMemberships   → list of club codes user belongs to
```

---

## Test Summary

| Test | Description | Result |
|------|-------------|--------|
| 1 | Create Book Club | [ ] Pass [ ] Fail |
| 2 | Join Book Club | [ ] Pass [ ] Fail |
| 3 | Add Book Syncs | [ ] Pass [ ] Fail |
| 4 | Remove Book Syncs | [ ] Pass [ ] Fail |
| 5 | Duplicate Club Prevention | [ ] Pass [ ] Fail |
| 6 | Already Member Prevention | [ ] Pass [ ] Fail |
| 7 | Invalid Club Code | [ ] Pass [ ] Fail |
| 8 | URL Parsing | [ ] Pass [ ] Fail |
| 9 | Sign-Out Clears Data | [ ] Pass [ ] Fail |
| 10 | Sign-In Restores Memberships | [ ] Pass [ ] Fail |
| 11 | Delete Book Club | [ ] Pass [ ] Fail |
| 12 | Non-Club Shelf Unchanged | [ ] Pass [ ] Fail |

**Tester:** _______________
**Date:** _______________
**App Version:** _______________
**Overall Result:** [ ] All Pass [ ] Issues Found

---

## Issues Found

| Issue # | Test | Description | Severity | Status |
|---------|------|-------------|----------|--------|
| | | | | |
| | | | | |
| | | | | |
