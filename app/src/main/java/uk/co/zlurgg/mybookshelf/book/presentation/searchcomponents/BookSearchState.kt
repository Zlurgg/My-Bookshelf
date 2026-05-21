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
    val searchBySubject: Boolean = false,
    val safeSearchEnabled: Boolean = true,
    val filteredCount: Int = 0,
    val errorMessage: String? = null
) {
    /** Title can be unchecked if at least one other is checked. */
    val canToggleTitle: Boolean get() = searchByAuthor || searchBySubject

    /** Author can be unchecked if at least one other is checked. */
    val canToggleAuthor: Boolean get() = searchByTitle || searchBySubject

    /** Subject can be unchecked if at least one other is checked. */
    val canToggleSubject: Boolean get() = searchByTitle || searchByAuthor

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

    fun withFilteredResults(allResults: List<Book>, safeResults: List<Book>): BookSearchState = copy(
        isLoading = false,
        hasSearched = true,
        errorMessage = null,
        results = safeResults,
        filteredCount = allResults.size - safeResults.size
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
     *
     * Title+Author both checked = use general q= (searches across all metadata).
     * Subject is always an explicit subject: qualifier when checked.
     */
    fun toSearchParams(): BookSearchParams {
        val trimmedQuery = query.trim()
        val useGeneral = searchByTitle && searchByAuthor
        // Defensive fallback: if no filter is checked, use general search
        val needsFallback = !searchByTitle && !searchByAuthor && !searchBySubject
        return BookSearchParams(
            general = if (useGeneral || needsFallback) trimmedQuery else null,
            title = if (!useGeneral && searchByTitle) trimmedQuery else null,
            author = if (!useGeneral && searchByAuthor) trimmedQuery else null,
            subject = if (searchBySubject) trimmedQuery else null
        )
    }
}
