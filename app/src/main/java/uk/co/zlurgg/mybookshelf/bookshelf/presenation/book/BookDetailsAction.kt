package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book

sealed interface  BookDetailsAction {
    data object OnRateBookDetailsClick : BookDetailsAction
    data object OnPurchaseClick : BookDetailsAction
    data class OnAddBookToBookshelfClick(val book: Book) : BookDetailsAction
}