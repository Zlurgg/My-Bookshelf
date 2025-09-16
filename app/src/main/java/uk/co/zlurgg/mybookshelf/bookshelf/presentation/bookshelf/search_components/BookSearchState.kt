package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.search_components

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort

data class BookSearchState(
    val query: String,
    val results: List<Book>,
    val isLoading: Boolean,
    val inShelfIds: Set<String>,
    val selectedSort: BookSearchSort,
    val selectedLanguage: String? = null
)