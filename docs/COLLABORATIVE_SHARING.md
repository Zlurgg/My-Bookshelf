# Book Club Feature

## Overview

Collaborative "Book Club" shelves where multiple users can build and manage a shared collection together. Complements the existing deep link sharing (which gives recipients a copy).

| Feature | Deep Link Share | Book Club |
|---------|-----------------|-----------|
| Purpose | Give someone a copy | Collaborate together |
| Data Storage | Embedded in URL | Firestore real-time sync |
| Account Required | No | Yes (Google Sign-In) |
| Updates | One-time transfer | Real-time sync |

---

## What's Implemented

### Core Features
- **Create Book Club**: Convert any shelf to a collaborative club with 8-char invite code
- **Join Book Club**: Via invite link, deep link (`mybookshelf://club/{code}`), or manual code entry
- **Real-time Sync**: Add/remove books syncs to all members automatically
- **Leave/Delete**: Members can leave (keeps books), owner can delete entire club
- **Owner Permissions**: Only owner can rename, restyle, or delete the club
- **Owner Badge**: Crown icon on owner's shelf card

### Additional Features
- **Limits**: Max 20 personal shelves, max 5 book clubs per user
- **Guest Protection**: Sign-in required dialog for Book Clubs features
- **Deep Link Handling**: Web landing page + app intent filter
- **Auto-Convert**: When owner deletes, members' shelves become personal (books preserved)

### Key Design Decisions
| Aspect | Decision |
|--------|----------|
| Ownership | Creator is owner with exclusive control |
| Owner Powers | Rename, change style, delete club, invite |
| Member Powers | Add/remove books, leave club |
| Member Leaves | Shelf converts to personal, books preserved |
| Owner Deletes | Club deleted, all members' shelves convert to personal |
| Invite Code | 8-char alphanumeric (excludes confusing chars: 0/O/1/I/L) |

### Firestore Structure
```
/bookClubs/{code}/
  metadata: { name, style, createdAt, createdBy, memberCount, bookCount }
  /members/{userId}: { joinedAt, displayName }
  /books/{bookId}: { title, authors, coverUrl, addedBy, addedAt, ... }
```

---

## What's Remaining

### Future: Reviews (Not Started)
- Add rating/review to any book in club
- See other members' reviews
- Edit/delete own reviews

### Future: Members List (Not Started)
- View list of all club members
- See member names and join dates

### Open Questions
- Notification strategy for new books/members?

---

## Status

**Core functionality: 100% complete**

| Phase | Status |
|-------|--------|
| Create Book Club | Complete |
| Join Book Club | Complete |
| Sync (Add/Remove) | Complete |
| Leave & Delete | Complete |
| Permissions | Complete |
| Deep Links | Complete |
| Limits | Complete |
| Guest Mode | Complete |
| Reviews | Not Started |
| Members List | Not Started |
