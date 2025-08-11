package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_details

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book

sealed interface  BookDetailsAction {
    data object OnRateBookDetailsClick : BookDetailsAction
    data object OnPurchaseClick : BookDetailsAction
    data class OnAddBookClick(val book: Book) : BookDetailsAction
    data class  OnRemoveBookClick(val book: Book): BookDetailsAction
}