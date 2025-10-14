package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.search_components

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort

interface BookSearchCallbacks {
    val onQueryChange: (String) -> Unit
    val onSortChange: (BookSearchSort) -> Unit
    val onToggleSearchByTitle: () -> Unit
    val onToggleSearchByAuthor: () -> Unit
    val onAddBook: (Book) -> Unit
    val onRemoveBook: (Book) -> Unit
    val onBookClick: (Book) -> Unit
    val onDismiss: () -> Unit
}