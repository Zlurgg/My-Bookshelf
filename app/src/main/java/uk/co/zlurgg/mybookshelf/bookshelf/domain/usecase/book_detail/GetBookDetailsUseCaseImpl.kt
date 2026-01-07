package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookDetailsWithShelfStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of GetBookDetailsUseCase that orchestrates book loading and shelf status.
 * Combines reactive data from multiple repositories.
 */
class GetBookDetailsUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookcaseRepository: BookcaseRepository
) : GetBookDetailsUseCase {

    override suspend fun execute(bookId: String, shelfId: String): Flow<BookDetailsWithShelfStatus> {
        // Get shelf info to check if it's a book club
        val shelf = bookcaseRepository.getShelfById(shelfId)
        val isBookClub = shelf?.isBookClub ?: false
        val clubCode = shelf?.clubCode

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
                    isOnShelf = isOnShelf,
                    isBookClub = isBookClub,
                    clubCode = clubCode
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