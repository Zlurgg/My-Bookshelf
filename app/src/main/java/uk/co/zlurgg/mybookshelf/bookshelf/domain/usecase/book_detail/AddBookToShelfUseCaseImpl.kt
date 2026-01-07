package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

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
    private val bookClubRepository: BookClubRepository,
    private val syncSchedulerService: SyncSchedulerService
) : AddBookToShelfUseCase {

    override suspend fun execute(book: Book, shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // Check shelf book limit before adding
            val shelf = bookcaseRepository.getShelfById(shelfId)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            if (shelf.books.size >= MAX_BOOKS_PER_SHELF) {
                Timber.tag(TAG).w("Shelf %s has reached maximum of %d books", shelfId, MAX_BOOKS_PER_SHELF)
                return Result.Error(DataError.Local.MAX_BOOKS_REACHED)
            }

            // Check if book already exists to preserve personal metadata
            val existingBook = bookRepository.getBookById(book.id)

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
                book.copy(spineColor = BookColorGenerator.generateSpineColor())
            }

            // Persist the book (with preserved metadata if it existed)
            bookRepository.upsertBook(bookToUpsert)

            // Then create the shelf association
            bookshelfRepository.addBookToShelf(shelfId, book.id)

            // If this is a book club shelf, also sync to Firestore club collection
            if (shelf.isBookClub && !shelf.clubCode.isNullOrEmpty()) {
                Timber.tag(TAG).d("Syncing book %s to book club %s", book.id, shelf.clubCode)
                val syncResult = bookClubRepository.syncBookToClub(shelf.clubCode, bookToUpsert)
                if (syncResult is Result.Error) {
                    Timber.tag(TAG).w("Failed to sync book to club: %s", syncResult.error)
                    // Don't fail the whole operation - local add succeeded
                }
            }

            // Trigger sync after successful book addition
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: AddBookToShelf")
            syncSchedulerService.triggerImmediateSync()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                    ?: DataError.Local.UNKNOWN
            )
        }
    }

    companion object {
        private const val TAG = "AddBookToShelf"
        const val MAX_BOOKS_PER_SHELF = 20
    }
}