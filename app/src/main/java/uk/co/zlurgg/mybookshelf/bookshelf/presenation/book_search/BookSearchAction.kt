package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

sealed interface BookSearchAction {
    data class OnSearchQueryChange(val query: String): BookSearchAction
    data class OnBookClick(val book: Book): BookSearchAction
}