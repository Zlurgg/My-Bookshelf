package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.searchcomponents

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

data class BookSearchState(
    val query: String,
    val results: List<Book>,
    val isLoading: Boolean,
    val isTyping: Boolean = false, // Shows user is typing during debounce period
    // Tracks if any search has completed (prevents showing "no results" before first search)
    val hasSearched: Boolean = false,
    val inShelfIds: Set<String>,
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = true
)
