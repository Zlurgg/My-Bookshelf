package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf

interface ShelfCallbacks {
    fun onRemoveBookshelf(shelf: Bookshelf)
    fun onBookshelfClick(shelf: Bookshelf)
    fun onLongClick(shelf: Bookshelf)
    fun onChangeStyle(shelf: Bookshelf)
    fun onDelete(shelf: Bookshelf)
    fun onShareShelf(shelf: Bookshelf)
    fun onDuplicateShelf(shelf: Bookshelf)
    fun onReorderShelf(shelf: Bookshelf, position: Int)
}
