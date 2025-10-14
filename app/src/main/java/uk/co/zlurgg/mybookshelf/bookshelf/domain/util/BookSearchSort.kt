package uk.co.zlurgg.mybookshelf.bookshelf.domain.util

/**
 * Book search sort options.
 * - BEST_MATCH: Uses OpenLibrary API's default relevance sorting (no client-side processing)
 * - NEWEST/OLDEST: Uses OpenLibrary's server-side date sorting via sort parameter
 */
enum class BookSearchSort(
    val displayName: String,
    val useServerSide: Boolean = false,
    val serverSortParam: String? = null
) {
    BEST_MATCH("Best Match", useServerSide = false),
    NEWEST("Newest First", useServerSide = true, serverSortParam = "new"),
    OLDEST("Oldest First", useServerSide = true, serverSortParam = "old");

    val isClientSide: Boolean get() = !useServerSide
}