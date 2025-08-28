package uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial

data class Bookshelf(
    val id: String,
    val name: String,
    val books: List<Book>,
    val shelfMaterial: ShelfMaterial
)