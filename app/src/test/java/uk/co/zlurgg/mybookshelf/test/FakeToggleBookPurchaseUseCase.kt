package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.ToggleBookPurchaseUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FakeToggleBookPurchaseUseCase : ToggleBookPurchaseUseCase {

    override suspend fun execute(book: Book, purchased: Boolean): Result<Book, DataError.Local> {
        return Result.Success(book.copy(purchased = purchased))
    }
}