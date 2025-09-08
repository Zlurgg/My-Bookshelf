package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book

sealed interface  BookDetailAction {
    data class OnRateBookDetailClick(val rating: Int) : BookDetailAction
    data object OnPurchaseClick : BookDetailAction
    data class OnAddBookClick(val book: Book) : BookDetailAction
    data class  OnRemoveBookClick(val book: Book): BookDetailAction
}