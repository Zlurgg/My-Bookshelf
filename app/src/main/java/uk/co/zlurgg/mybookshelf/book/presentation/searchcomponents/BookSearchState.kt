package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

import uk.co.zlurgg.mybookshelf.book.domain.model.Book

data class BookSearchState(
    val query: String = "",
    // The query that produced the currently-displayed results.
    // Remote mode: written by OnSubmitSearch only. Library mode: written on every
    // OnSearchQueryChange so the type-to-filter invariant holds. See plan §Decisions.
    val lastSubmittedQuery: String = "",
    val results: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val existingBookIds: Set<String> = emptySet(),
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = false,
    val searchBySubject: Boolean = false,
    val safeSearchEnabled: Boolean = true,
    val libraryScopeEnabled: Boolean = false,
    val filteredCount: Int = 0,
    val errorMessage: String? = null,
    // Pagination: pre-filter cursor into the provider's result stream. Advances
    // by BookSearchResponse.rawPageSize, not results.size — see C1 plan §Critical
    // correctness for why a post-filter advance corrupts page 2 on Google.
    val nextStartIndex: Int = 0,
    val isLoadingMore: Boolean = false,
    // Stored (not a getter) so the predicate (`rawPageSize >= pageSize`) lives in
    // one place in the VM and state.copy() doesn't re-derive it.
    val canLoadMore: Boolean = false,
) {
    /** Title can be unchecked if at least one other is checked. */
    val canToggleTitle: Boolean get() = searchByAuthor || searchBySubject

    /** Author can be unchecked if at least one other is checked. */
    val canToggleAuthor: Boolean get() = searchByTitle || searchBySubject

    /** Subject can be unchecked if at least one other is checked. */
    val canToggleSubject: Boolean get() = searchByTitle || searchByAuthor

    fun withLoading(): BookSearchState = copy(
        isLoading = true,
        errorMessage = null
    )

    // Resets every pagination field alongside the loading flags. Every fresh
    // search must go through this rather than `withLoading()` — manually copying
    // a subset at each call site reliably forgets one when state grows.
    fun withFreshSearch(): BookSearchState = copy(
        isLoading = true,
        isLoadingMore = false,
        errorMessage = null,
        results = emptyList(),
        nextStartIndex = 0,
        canLoadMore = false,
        filteredCount = 0,
    )

    fun withResults(results: List<Book>): BookSearchState = copy(
        isLoading = false,
        hasSearched = true,
        errorMessage = null,
        results = results
    )

    // Opt-in reset of search-result fields. Filter prefs, existingBookIds, and
    // safeSearch/libraryScope flags are preserved by omission from copy(...);
    // future fields default to preserved unless named explicitly here.
    fun resetForDialogClose(): BookSearchState = copy(
        query = "",
        lastSubmittedQuery = "",
        results = emptyList(),
        hasSearched = false,
        isLoading = false,
        isLoadingMore = false,
        canLoadMore = false,
        nextStartIndex = 0,
        filteredCount = 0,
        errorMessage = null,
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
