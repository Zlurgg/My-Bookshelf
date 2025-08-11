package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

sealed interface  BookDetailAction {
    data object OnRateBookDetailClick : BookDetailAction
    data object OnPurchaseClick : BookDetailAction
    data class OnAddBookClick(val book: Book) : BookDetailAction
    data class  OnRemoveBookClick(val book: Book): BookDetailAction
}