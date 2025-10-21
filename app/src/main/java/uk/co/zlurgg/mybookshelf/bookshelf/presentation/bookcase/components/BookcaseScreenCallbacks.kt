package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseAction

fun createShelfCallbacks(
    onAction: (BookcaseAction) -> Unit,
    isTutorialShelf: Boolean
): ShelfCallbacks = object : ShelfCallbacks {
    override fun onRemoveBookshelf(shelf: Bookshelf) {
        onAction(BookcaseAction.OnRemoveBookShelf(shelf))
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
        onAction(BookcaseAction.OnRemoveBookShelf(shelf))
    }

    override fun onShareShelf(shelf: Bookshelf) {
        onAction(BookcaseAction.OnShareShelfClick(shelf))
    }

    override fun onDuplicateShelf(shelf: Bookshelf) {
        onAction(BookcaseAction.OnDuplicateShelfClick(shelf))
    }

    override fun onReorderShelf(shelf: Bookshelf, position: Int) {
        onAction(BookcaseAction.OnReorderShelf(shelf, position))
    }
}
