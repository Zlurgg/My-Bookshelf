package uk.co.zlurgg.mybookshelf.bookshelf.domain.util

enum class BookSearchSort(
    val displayName: String,
    val useServerSide: Boolean = false,
    val serverSortParam: String? = null
) {
    BEST_MATCH("Best Match", useServerSide = false),
    NEWEST("Newest First", useServerSide = true, serverSortParam = "new"),
    OLDEST("Oldest First", useServerSide = true, serverSortParam = "old"),
    HIGHEST_RATED("Highest Rated", useServerSide = false),
    MOST_POPULAR("Most Popular", useServerSide = false);

    val isClientSide: Boolean get() = !useServerSide
}