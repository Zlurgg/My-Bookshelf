package uk.co.zlurgg.mybookshelf.bookshelf.presentation.searchcomponents

import uk.co.zlurgg.mybookshelf.book.domain.model.Book

interface BookSearchCallbacks {
    val onQueryChange: (String) -> Unit
    val onToggleSearchByTitle: () -> Unit
    val onToggleSearchByAuthor: () -> Unit
    val onToggleSearchBySubject: () -> Unit
    val onToggleSafeSearch: () -> Unit
    val onToggleLibraryScope: () -> Unit
    val onAddBook: (Book) -> Unit
    val onRemoveBook: (Book) -> Unit
    val onBookClick: (Book) -> Unit
    val onDismiss: () -> Unit
}
