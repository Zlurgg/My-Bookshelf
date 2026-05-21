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
) {
    /** Title can only be unchecked if author is still checked. */
    val canToggleTitle: Boolean get() = searchByAuthor

    /** Author can only be unchecked if title is still checked. */
    val canToggleAuthor: Boolean get() = searchByTitle

    fun withLoading(): BookSearchState = copy(
        isLoading = true,
        isTyping = false,
        errorMessage = null
    )

    fun withResults(results: List<Book>): BookSearchState = copy(
        isLoading = false,
        hasSearched = true,
        errorMessage = null,
        results = results
    )

    // query is untrimmed in state; trim here to detect whitespace-only input
    fun withBelowMinLength(): BookSearchState = copy(
        isLoading = false,
        isTyping = false,
        errorMessage = null,
        results = if (query.trim().isEmpty()) emptyList() else results
    )

    /**
     * Maps filter checkbox state to OpenLibrary API search parameters.
     * Uses this.query (trimmed internally) — callers should not pass a separate query.
     * Exactly one of the returned fields will be non-null.
     */
    fun toSearchParams(): BookSearchParams {
        val trimmedQuery = query.trim()
        return when {
            searchByTitle && searchByAuthor -> BookSearchParams(general = trimmedQuery)
            searchByTitle -> BookSearchParams(title = trimmedQuery)
            searchByAuthor -> BookSearchParams(author = trimmedQuery)
            else -> BookSearchParams(general = trimmedQuery) // defensive fallback
        }
    }
}
