package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.BookShelf

sealed interface BookshelfAction {
    data class OnBookshelfClick(val bookshelf: BookShelf): BookshelfAction
    data class OnSearchQueryChange(val query: String): BookshelfAction
}