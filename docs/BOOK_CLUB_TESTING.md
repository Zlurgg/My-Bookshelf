# Book Club Feature - Manual Testing

## Prerequisites

- Two devices/emulators with different Google accounts
- Fresh app install recommended (or clear app data)
- Firestore rules deployed (see `firestore.rules`)

---

## Test Summary

### Core Functionality (22 tests)

| # | Test | Description |
|---|------|-------------|
| 1 | Create Book Club | Create from shelf, get 8-char invite code |
| 2 | Join Book Club | Join via code, see preview, books sync |
| 3 | Add Book Syncs | User A adds book → appears for User B |
| 4 | Remove Book Syncs | User B removes book → disappears for User A |
| 5 | Duplicate Prevention | Can't create second club from same shelf |
| 6 | Already Member | Error when joining club you're already in |
| 7 | Invalid Code | Proper error messages for bad codes |
| 8 | URL Parsing | Paste full URL, code extracted correctly |
| 9 | Sign-Out Clears | Book club shelf removed on sign-out |
| 10 | Sign-In Restores | Book club memberships restored on sign-in |
| 11 | Delete Converts | Owner deletes → members' shelves become personal |
| 12 | Member Count | Tracks correctly on join/leave |
| 13 | Owner-Only Ops | Only owner can rename/restyle/delete |
| 14 | Owner Badge | Crown icon on owner's shelf only |
| 15 | Non-Club Unchanged | Regular shelves work normally |
| 16 | Deep Link Join | `mybookshelf://club/{code}` opens join flow |
| 17 | Personal Shelf Limit | Max 20 shelves enforced |
| 18 | Book Club Limit | Max 5 clubs enforced |
| 19 | Guest Mode - Tab | Sign-in dialog when tapping Book Clubs tab |
| 20 | Guest Mode - Create | Sign-in dialog when creating club |
| 21 | Member Swipe | Swipe shows leave dialog (not delete) |
| 22 | Share vs Invite | Separate menu items for each action |

### Edge Cases

| Scenario | Expected |
|----------|----------|
| Create/join offline | Fails gracefully with network error |
| Add/remove book offline | Local succeeds, sync fails (logged warning) |
| Long shelf name | Handled gracefully |
| Special characters | Handled correctly |
| Rapid add/remove | No crashes, consistent state |

---

## What's Remaining

| Feature | Tests Needed |
|---------|--------------|
| Reviews | Add/edit/delete review, see others' reviews |
| Members List | View member list, see join dates |

---

## Debugging

### Logcat Tags
```bash
adb logcat -s BookClubSync,BookClubRepo,BookClubCode,CreateBookClub,JoinBookClub,RestoreBookClub
```

### Firestore Paths
```
/bookClubs/{code}/
  metadata          → name, style, createdAt, createdBy, bookCount, memberCount
  /members/{id}     → userId, displayName, joinedAt
  /books/{id}       → title, authors, coverUrl, addedBy, addedAt

/users/{userId}/settings/preferences
  clubMemberships   → list of club codes
```

---

## Status

**All 22 core tests defined** - Ready for manual testing

| Category | Count |
|----------|-------|
| Core tests | 22 |
| Edge cases | 5 |
| Future (Reviews) | TBD |
| Future (Members) | TBD |
