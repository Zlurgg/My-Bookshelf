package uk.co.zlurgg.mybookshelf.bookshelf.domain

data class Bookshelf(
    val id: String,
    val name: String,
    val books: List<Book>,
    val shelfStyle: ShelfStyle
)