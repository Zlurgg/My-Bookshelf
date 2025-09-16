package uk.co.zlurgg.mybookshelf.bookshelf.domain.util

enum class BookSearchSort(val displayName: String) {
    BEST_MATCH("Best Match"),
    HIGHEST_RATED("Highest Rated"),
    MOST_POPULAR("Most Popular"),
    NEWEST("Newest"),
    OLDEST("Oldest"),
    TITLE_A_Z("Title A-Z"),
    TITLE_Z_A("Title Z-A"),
    AUTHOR_A_Z("Author A-Z"),
    AUTHOR_Z_A("Author Z-A")
}