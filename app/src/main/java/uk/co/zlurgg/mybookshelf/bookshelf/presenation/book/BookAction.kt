package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book.Book

sealed class  BookAction {
    data class OnRateBookClick(val book: Book) : BookAction()
    data class OnPurchaseClick(val book: Book) : BookAction()
    data class OnAddToBookShelfClick(val book: Book) : BookAction()
}