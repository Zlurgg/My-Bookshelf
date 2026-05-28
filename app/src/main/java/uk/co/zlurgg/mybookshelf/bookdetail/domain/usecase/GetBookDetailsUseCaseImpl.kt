package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.book.domain.model.BookDetailsWithShelfStatus
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
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

    override suspend operator fun invoke(bookId: String, shelfId: String?): Flow<BookDetailsWithShelfStatus> {
        // Get shelf info to check if it's a book club
        val shelf = if (shelfId != null) {
            when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
                is Result.Success -> getResult.data
                is Result.Error -> null
            }
        } else {
            null
        }
        val isBookClub = shelf?.isBookClub ?: false
        val clubCode = shelf?.clubCode
        val clubCreatorId = shelf?.clubCreatorId

        // Get who added this book to the shelf (for club remove permissions)
        val addedByUserId = if (shelfId != null && isBookClub) {
            when (val result = bookshelfRepository.getAddedByUserId(shelfId, bookId)) {
                is Result.Success -> result.data
                is Result.Error -> null
            }
        } else {
            null
        }

        // Get the shelf status Flow and combine with book data
        val shelfStatusFlow = if (shelfId != null) {
            bookshelfRepository.isBookOnShelf(bookId, shelfId)
        } else {
            flow { emit(false) }
        }
        val isInLibraryFlow = bookRepository.getAllPersonalBooks()
            .map { books -> books.any { it.id == bookId } }
        return combine(
            shelfStatusFlow,
            // Convert single book fetch to Flow behavior by getting book once.
            // DB-first: a persisted book always wins, so personal metadata is
            // preserved. Falls back to the preview cache when no row exists,
            // which is the only legitimate cache consumer in the codebase —
            // the compose is explicit so future readers don't re-route the
            // cache through the repository's `getBookById`.
            flow {
                val bookResult = bookRepository.getBookById(bookId)
                val book = when (bookResult) {
                    is Result.Success -> bookResult.data ?: bookRepository.peekPreview(bookId)
                    is Result.Error -> null // Handle error gracefully in UI
                }
                emit(book)
            },
            isInLibraryFlow,
        ) { isOnShelf, book, isInLibrary ->
            BookDetailsWithShelfStatus(
                book = book,
                isOnShelf = isOnShelf,
                isInLibrary = isInLibrary,
                isBookClub = isBookClub,
                clubCode = clubCode,
                clubCreatorId = clubCreatorId,
                addedByUserId = addedByUserId,
            )
        }
    }
}
