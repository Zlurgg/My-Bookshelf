package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf

sealed interface BookshelfAction {
    data class OnBookClick(val book: Book): BookshelfAction
    data class OnSearchQueryChange(val query: String): BookshelfAction
}