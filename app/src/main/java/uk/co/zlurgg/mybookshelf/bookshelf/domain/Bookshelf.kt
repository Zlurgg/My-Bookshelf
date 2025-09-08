package uk.co.zlurgg.mybookshelf.bookshelf.domain

import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

data class Bookshelf(
    val id: String,
    val name: String,
    val books: List<Book>,
    val shelfStyle: ShelfStyle
)