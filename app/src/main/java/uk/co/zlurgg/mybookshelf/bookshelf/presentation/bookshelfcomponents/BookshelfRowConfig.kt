package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelfcomponents

import uk.co.zlurgg.mybookshelf.book.presentation.util.BookDisplayStyle

data class BookshelfRowConfig(
    val showAddSlot: Boolean = false,
    val isTidyMode: Boolean = false,
    val bookStyles: List<BookDisplayStyle>? = null,
    val onAddClick: (() -> Unit)? = null
)
