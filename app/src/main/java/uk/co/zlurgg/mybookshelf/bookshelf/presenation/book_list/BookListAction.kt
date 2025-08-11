package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_list

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book

sealed interface BookListAction {
    data class OnSearchQueryChange(val query: String): BookListAction
    data class OnBookClick(val book: Book): BookListAction
}