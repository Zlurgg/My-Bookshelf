package uk.co.zlurgg.mybookshelf.bookshelf.domain.model

data class Bookcase(
    val id: String,
    val bookshelves: List<Bookshelf>
)