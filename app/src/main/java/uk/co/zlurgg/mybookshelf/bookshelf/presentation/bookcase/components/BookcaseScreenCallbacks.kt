package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseAction

fun createShelfCallbacks(
    onAction: (BookcaseAction) -> Unit,
    isTutorialShelf: Boolean,
    currentUserId: String? = null
): ShelfCallbacks = object : ShelfCallbacks {
    override fun onRemoveBookshelf(shelf: Bookshelf) {
        if (shelf.isBookClub) {
            if (shelf.clubCreatorId == currentUserId) {
                // Owner deletes the book club
                onAction(BookcaseAction.ShowDeleteBookClubDialog(shelf))
            } else {
                // Member leaves the book club
                onAction(BookcaseAction.ShowLeaveBookClubDialog(shelf))
            }
        } else {
            onAction(BookcaseAction.OnRemoveBookShelf(shelf))
        }
    }

    override fun onBookshelfClick(shelf: Bookshelf) {
        onAction(BookcaseAction.OnBookshelfClick(shelf))
    }

    override fun onLongClick(shelf: Bookshelf) {
        if (!isTutorialShelf) {
            onAction(BookcaseAction.ShowRenameDialog(shelf))
        }
    }

    override fun onChangeStyle(shelf: Bookshelf) {
        onAction(BookcaseAction.ShowChangeStyleDialog(shelf))
    }

    override fun onDelete(shelf: Bookshelf) {
        if (shelf.isBookClub) {
            if (shelf.clubCreatorId == currentUserId) {
                // Owner deletes the book club
                onAction(BookcaseAction.ShowDeleteBookClubDialog(shelf))
            } else {
                // Member leaves the book club
                onAction(BookcaseAction.ShowLeaveBookClubDialog(shelf))
            }
        } else {
            onAction(BookcaseAction.OnRemoveBookShelf(shelf))
        }
    }

    override fun onCreateBookClub(shelf: Bookshelf) {
        onAction(BookcaseAction.OnCreateBookClub(shelf))
    }

    override fun onInviteToClub(shelf: Bookshelf) {
        onAction(BookcaseAction.OnInviteToClub(shelf))
    }

    override fun onDuplicateShelf(shelf: Bookshelf) {
        onAction(BookcaseAction.OnDuplicateShelfClick(shelf))
    }

    override fun onReorderShelf(shelf: Bookshelf, position: Int) {
        onAction(BookcaseAction.OnReorderShelf(shelf, position))
    }

    override fun onLeaveBookClub(shelf: Bookshelf) {
        onAction(BookcaseAction.ShowLeaveBookClubDialog(shelf))
    }
}
