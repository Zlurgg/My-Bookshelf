package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.search_components

import androidx.compose.runtime.Stable
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

@Stable
data class BookSearchState(
    val query: String,
    val results: List<Book>,
    val isLoading: Boolean,
    val isTyping: Boolean = false, // Shows user is typing during debounce period
    val hasSearched: Boolean = false, // Tracks if any search has completed (prevents showing "no results" before first search)
    val inShelfIds: Set<String>,
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = true,
)
