package uk.co.zlurgg.mybookshelf.bookshelf.domain.model

/**
 * Result data class for getAllShelves operations.
 * Contains both the list of shelves and their book counts.
 */
data class AllShelvesResult(
    val shelves: List<Bookshelf>,
    val bookCounts: Map<String, Int>
)