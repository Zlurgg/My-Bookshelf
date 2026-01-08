package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components

import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.BookDisplayStyle

data class BookshelfRowConfig(
    val showAddSlot: Boolean = false,
    val isTidyMode: Boolean = false,
    val bookStyles: List<BookDisplayStyle>? = null,
    val onAddClick: (() -> Unit)? = null,
)
