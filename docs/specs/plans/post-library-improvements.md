# Post-Library Improvements Plan

Follow-up improvements identified during Library screen implementation and testing.

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
