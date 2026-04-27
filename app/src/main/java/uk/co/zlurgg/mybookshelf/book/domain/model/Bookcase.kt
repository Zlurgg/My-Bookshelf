package uk.co.zlurgg.mybookshelf.book.domain.model

data class Bookcase(
    val id: String,
    val bookshelves: List<Bookshelf>,
    val bookCounts: Map<String, Int> = emptyMap()
)
