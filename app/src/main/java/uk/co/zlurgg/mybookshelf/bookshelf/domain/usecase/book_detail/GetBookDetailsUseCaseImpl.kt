package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * Implementation of GetBookDetailsUseCase that orchestrates book loading and shelf status.
 * Combines reactive data from multiple repositories.
 */
class GetBookDetailsUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository
) : GetBookDetailsUseCase {

    override suspend fun execute(bookId: String, shelfId: String): Flow<BookDetailsWithShelfStatus> {
        // Get the shelf status Flow and combine with book data
        return bookshelfRepository.isBookOnShelf(bookId, shelfId)
            .combine(
                // Convert single book fetch to Flow behavior by getting book once
                kotlinx.coroutines.flow.flow {
                    emit(bookRepository.getBookById(bookId))
                }
            ) { isOnShelf, book ->
                BookDetailsWithShelfStatus(
                    book = book,
                    isOnShelf = isOnShelf
                )
            }
    }

    override suspend fun loadBookDescription(bookId: String): Result<Unit, DataError.Local> {
        return try {
            // Load description from remote and update the book
            val descriptionResult = bookRepository.getBookDescription(bookId)
            when (descriptionResult) {
                is Result.Success -> {
                    // The repository handles updating the book with the description
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    // Convert remote error to local error for this context
                    Result.Error(DataError.Local.UNKNOWN)
                }
            }
        } catch (e: Exception) {
            Result.Error(
                ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                    ?: DataError.Local.UNKNOWN
            )
        }
    }
}