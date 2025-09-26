package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FakeUpsertBookUseCase(
    private val bookRepository: BookRepository
) : UpsertBookUseCase {

    override suspend fun execute(book: Book): Result<Unit, DataError.Local> {
        return try {
            bookRepository.upsertBook(book)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}