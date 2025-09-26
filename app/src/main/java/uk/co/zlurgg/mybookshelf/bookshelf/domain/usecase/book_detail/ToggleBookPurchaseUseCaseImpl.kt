package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ToggleBookPurchaseUseCaseImpl(
    private val bookRepository: BookRepository
) : ToggleBookPurchaseUseCase {

    override suspend fun execute(book: Book, purchased: Boolean): Result<Book, DataError.Local> {
        return try {
            val updatedBook = book.copy(purchased = purchased)
            bookRepository.upsertBook(updatedBook)
            Result.Success(updatedBook)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}