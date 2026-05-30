package uk.co.zlurgg.mybookshelf.book.domain.model

data class BookSearchResponse(
    val books: List<Book>,
    // Pre-filter count of items the provider actually returned. Used to advance
    // pagination (post-filter `books.size` would lie when language/safe-search
    // drops items) and to detect end-of-results uniformly: rawPageSize < pageSize.
    val rawPageSize: Int,
    // The page size the data source asked the provider for on THIS request.
    // Per-response so a Google→OL fallback on page 1 advertises OL's 100, not
    // Google's 40, when the VM computes canLoadMore.
    val pageSize: Int,
)
