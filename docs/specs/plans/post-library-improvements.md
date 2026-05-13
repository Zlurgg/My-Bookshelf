# Post-Library Improvements Plan

Follow-up improvements identified during Library screen implementation and testing.

## Book Clubs

### View-only for guests
Remove sign-in gate on Book Clubs tab navigation. Guests can browse clubs they were previously in but cannot join, create, or add to clubs. Action-level guards already exist — just need to disable UI buttons when not signed in rather than blocking navigation.

### Creation delay — loading indicator
When creating a book club there is a delay between creation and appearance. Add a loading state to indicate the club is being created.

### User display / nicknames in reviews
Check how users are displayed in reviews. Verify nickname handling and display formatting.

### Cross-tab navigation broken after bottom nav rework

The old design used internal tab switching within a single BookcaseScreen/ViewModel. The bottom nav split My Shelves and Book Clubs into separate routes with separate ViewModel instances. State-driven tab switching was never rewired.

**Broken flows:**
1. **Club creation → switch to Book Clubs tab**: `switchToBookClubsTab` is set in `BookcaseClubActionHandler.handleCreateResult()` but never observed. User stays on My Shelves after creating a club.
2. **Duplicate club shelf → switch to My Shelves tab**: `switchToPersonalTab` is set in `BookcaseViewModel.duplicateShelf()` but never observed. User stays on Book Clubs and can't see the personal copy.

**Fix:**
- Add `onSwitchToBookClubs: () -> Unit` and `onSwitchToPersonalTab: () -> Unit` callbacks to `BookcaseScreenRoot`
- Observe `state.switchToBookClubsTab` and `state.switchToPersonalTab` with `LaunchedEffect` in `BookcaseScreenRoot`, call the callbacks and reset
- Wire the callbacks in `MyBookShelfApp.kt` with bottom-nav-style `navController.navigate` (popUpTo start destination, saveState, restoreState, launchSingleTop)

**Other findings:**
- `NavigationRoute.Bookcase.ARG_SWITCH_TO_BOOK_CLUBS` is dead code — defined but never read. Clean up.
- BookClubs route is missing `createClubForShelfId` / `onCreateClubConsumed` (low risk — club creation only triggers from personal shelves on the Bookcase route)
- Separate ViewModel instances per route is fine for this fix since the flag is observed and consumed on the same route where it's set

## Library / BookDetail

### Delete book from library
Allow users to permanently delete a book from their library. Two entry points:
1. **Long press** on a book in the Library grid — show confirmation dialog, then delete
2. **Inside BookDetail** — add a delete action (e.g. overflow menu or button) when navigated from Library (no shelf context)

Deletion should remove the `BookEntity` row and any remaining `BookshelfBookCrossRef` rows for that book. Show a confirmation dialog since this destroys user data (ratings, notes, reading status).

### Add book to any existing shelf from BookDetail
When navigating to BookDetail from Library, there is no shelf context. Currently the add/remove shelf actions are hidden (`hasShelfContext = false`). Future improvement: show a shelf picker allowing the user to add the book to any existing shelf. This is a new flow distinct from the current single-shelf add/remove pattern.

## Bookshelf UI

### Replace FAB with inline add-book slot
Replace the floating action button with an inline add-book slot at the end of the shelf row. Should display as an empty book with `+`, matching the style and tilt of surrounding books. Row spacing and overflow calculations (`calculateBookRows`) need to account for this slot.

### Spine color bug — first book on shelf
Investigate possible issue where the first book on a shelf has its spine color matching the shelf color. May be a color generation or rendering issue.

## Account

### Anchor delete account at bottom
Move the delete account button to the bottom of the account page and anchor it there, away from other actions.
