# Post-Library Improvements Plan

Follow-up improvements identified during Library screen implementation and testing.

## Book Clubs

All book club items completed on `bc-polish` branch.

- ~~Preserve club shelves on sign-out~~ (fdabcf9)
- ~~Creation loading indicator~~ (fdabcf9)
- ~~User display / nicknames in reviews~~ (verified, no changes needed)
- ~~Cross-tab navigation~~ (fdabcf9)
- ~~Guest book removal from club shelves~~ (fdabcf9)

### Book detail consistency across variants

The book detail screen has 4 viewing contexts with inconsistent card layouts:
1. **Personal shelf** — full view (overview, image, recommendation, notes, community ratings, description, publication, languages, purchased)
2. **Book club (signed in)** — overview, image, club rating, club comments
3. **Book club (guest)** — overview, image, sign-in hint, community ratings, description, publication, languages
4. **Library** — no shelf context (`hasShelfContext = false`)

**Needed:** Audit all 4 variants, map what each shows, decide the correct set of cards per variant, and unify. The club views were recently patched (fdabcf9) but may still be inconsistent with personal/library views.

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
