package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FakeRemoveBookFromShelfUseCase : RemoveBookFromShelfUseCase {

    var result: Result<Unit, DataError.Local> = Result.Success(Unit)

    // Track calls for verification
    var lastBookId: String? = null
    var lastShelfId: String? = null
    var callCount = 0

    override suspend fun execute(bookId: String, shelfId: String): Result<Unit, DataError.Local> {
        lastBookId = bookId
        lastShelfId = shelfId
        callCount++
        return result
    }

    fun reset() {
        result = Result.Success(Unit)
        lastBookId = null
        lastShelfId = null
        callCount = 0
    }
}