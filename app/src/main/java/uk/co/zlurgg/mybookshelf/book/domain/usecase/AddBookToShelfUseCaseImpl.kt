package uk.co.zlurgg.mybookshelf.book.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of AddBookToShelfUseCase that orchestrates book persistence and shelf association.
 * Follows Clean Architecture by coordinating between domain repositories.
 * Generates spine color when book is first added to any shelf for optimal performance.
 * Also syncs to Firestore if the shelf is a book club.
 */
class AddBookToShelfUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val clubOperations: ClubOperations,
    private val timeProvider: TimeProvider,
    private val currentUserProvider: CurrentUserProvider,
) : AddBookToShelfUseCase {

    override suspend operator fun invoke(book: Book, shelfId: String): Result<Unit, DataError.Local> {
        // Check shelf book limit before adding
        val shelf = when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data ?: return Result.Error(DataError.Local.NOT_FOUND)
            is Result.Error -> return getResult
        }

        if (shelf.books.size >= BookshelfConstants.MAX_BOOKS_PER_SHELF) {
            Timber.tag(TAG).w(
                "Shelf %s has reached maximum of %d books",
                shelfId,
                BookshelfConstants.MAX_BOOKS_PER_SHELF
            )
            return Result.Error(DataError.Local.MAX_BOOKS_REACHED)
        }

        // Check if book already exists to preserve personal metadata
        val existingBook = when (val getResult = bookRepository.getBookById(book.id)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        val bookToUpsert = if (existingBook != null) {
            // Book exists - preserve ALL existing data including spine color
            book.copy(
                spineColor = existingBook.spineColor,
                readingStatus = existingBook.readingStatus,
                personalRating = existingBook.personalRating,
                personalNotes = existingBook.personalNotes,
                dateAdded = existingBook.dateAdded,
                purchaseDate = existingBook.purchaseDate,
                purchased = existingBook.purchased
            )
        } else {
            // New book - generate spine color now (not during search)
            book.copy(
                spineColor = BookColorGenerator.generateSpineColor(),
                dateAdded = book.dateAdded ?: timeProvider.currentTimeMillis()
            )
        }

        // Persist the book (with preserved metadata if it existed)
        when (val upsertResult = bookRepository.upsertBook(bookToUpsert)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return upsertResult
        }

        // Then create the shelf association
        val addedByUserId = if (shelf.isBookClub) currentUserProvider.getCurrentUserId() else null
        when (val addResult = bookshelfRepository.addBookToShelf(shelfId, book.id, addedByUserId)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return addResult
        }

        // If this is a book club shelf, also sync to Firestore club collection
        if (shelf.isBookClub && !shelf.clubCode.isNullOrEmpty()) {
            Timber.tag(TAG).d("Syncing book %s to book club %s", book.id, shelf.clubCode)
            val syncResult = clubOperations.syncBookToClub(shelf.clubCode, bookToUpsert)
            if (syncResult is Result.Error) {
                Timber.tag(TAG).w("Failed to sync book to club: %s", syncResult.error)
                // Don't fail the whole operation - local add succeeded
            }
        }

        return Result.Success(Unit)
    }

    companion object {
        private const val TAG = "AddBookToShelf"
    }
}
