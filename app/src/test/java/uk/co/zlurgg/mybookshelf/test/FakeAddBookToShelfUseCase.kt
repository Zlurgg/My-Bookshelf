package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

class FakeAddBookToShelfUseCase : AddBookToShelfUseCase {

    var result: Result<Unit, DataError.Local> = Result.Success(Unit)

    // Track calls for verification
    var lastBook: Book? = null
    var lastShelfId: String? = null
    var callCount = 0

    override suspend fun execute(book: Book, shelfId: String): Result<Unit, DataError.Local> {
        lastBook = book
        lastShelfId = shelfId
        callCount++
        return result
    }

    fun reset() {
        result = Result.Success(Unit)
        lastBook = null
        lastShelfId = null
        callCount = 0
    }
}