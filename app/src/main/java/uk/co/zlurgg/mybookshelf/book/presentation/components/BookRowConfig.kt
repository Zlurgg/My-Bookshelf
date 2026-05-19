package uk.co.zlurgg.mybookshelf.book.presentation.components

import uk.co.zlurgg.mybookshelf.book.presentation.util.BookDisplayStyle

data class BookRowConfig(
    val showAddSlot: Boolean = false,
    val isTidyMode: Boolean = false,
    val bookStyles: List<BookDisplayStyle>? = null,
    val onAddClick: (() -> Unit)? = null,
    val isSelectionMode: Boolean = false,
    val selectedBookIds: Set<String> = emptySet(),
)
