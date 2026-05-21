package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

/**
 * Search parameters for the OpenLibrary API.
 * Exactly one field should be non-null — produced by [BookSearchState.toSearchParams].
 */
data class BookSearchParams(
    val general: String? = null,
    val title: String? = null,
    val author: String? = null
)
