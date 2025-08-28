package uk.co.zlurgg.mybookshelf.bookshelf.domain.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf

data class Bookcase(
    val id: String,
    val bookshelves: List<Bookshelf>
)