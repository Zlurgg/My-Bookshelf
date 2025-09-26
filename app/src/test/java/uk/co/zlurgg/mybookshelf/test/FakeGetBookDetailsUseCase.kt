package uk.co.zlurgg.mybookshelf.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.BookDetailsWithShelfStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.GetBookDetailsUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FakeGetBookDetailsUseCase : GetBookDetailsUseCase {

    override suspend fun execute(bookId: String, shelfId: String): Flow<BookDetailsWithShelfStatus> {
        return flowOf(
            BookDetailsWithShelfStatus(
                book = null,
                isOnShelf = false
            )
        )
    }

    override suspend fun loadBookDescription(bookId: String): Result<Unit, DataError.Local> {
        return Result.Success(Unit)
    }
}