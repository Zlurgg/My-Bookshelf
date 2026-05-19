package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

import uk.co.zlurgg.mybookshelf.book.domain.model.Book

data class BookSearchState(
    val query: String = "",
    val results: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val isTyping: Boolean = false,
    val hasSearched: Boolean = false,
    val existingBookIds: Set<String> = emptySet(),
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = true,
    val errorMessage: String? = null
)
