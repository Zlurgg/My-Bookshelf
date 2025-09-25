package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * Implementation of RemoveBookFromShelfUseCase that handles removing book-shelf associations.
 * The book remains in the database; only the relationship to the shelf is removed.
 */
class RemoveBookFromShelfUseCaseImpl(
    private val bookshelfRepository: BookshelfRepository
) : RemoveBookFromShelfUseCase {

    override suspend fun execute(bookId: String, shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // Remove the book-shelf association
            bookshelfRepository.removeBookFromShelf(shelfId, bookId)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                    ?: DataError.Local.UNKNOWN
            )
        }
    }
}