package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookDetailConstants
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * UseCase to get existing tutorial book or create it if it doesn't exist.
 * Creates a welcome book with app usage instructions and adds it to the tutorial shelf.
 * The tutorial book is created as a system entity (not synced to cloud).
 */
class GetOrCreateTutorialBookUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val timeProvider: TimeProvider
) : GetOrCreateTutorialBookUseCase {

    override suspend fun execute(tutorialShelfId: String): Result<String, DataError.Local> {
        // Check if tutorial book already exists
        val existingBook = when (val getResult = bookRepository.getBookById(BookDetailConstants.TUTORIAL_BOOK_ID)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        val bookId = if (existingBook != null) {
            // Tutorial book exists, ensure it's on the tutorial shelf
            val isOnShelf = bookshelfRepository
                .isBookOnShelf(existingBook.id, tutorialShelfId)
                .first()

            if (!isOnShelf) {
                when (val addResult = bookshelfRepository.addBookToShelf(tutorialShelfId, existingBook.id)) {
                    is Result.Success -> { /* continue */ }
                    is Result.Error -> return addResult
                }
            }

            existingBook.id
        } else {
            // Create new tutorial book
            val tutorialBook = Book(
                id = BookDetailConstants.TUTORIAL_BOOK_ID,
                title = BookDetailConstants.TUTORIAL_BOOK_TITLE,
                authors = listOf(BookDetailConstants.TUTORIAL_BOOK_AUTHOR),
                imageUrl = BookDetailConstants.TUTORIAL_BOOK_IMAGE_URL,
                description = BookDetailConstants.TUTORIAL_BOOK_DESCRIPTION,
                languages = listOf("en"),
                firstPublishYear = null,
                averageRating = BookDetailConstants.TUTORIAL_BOOK_RATING,
                ratingCount = BookDetailConstants.TUTORIAL_BOOK_RATING_COUNT,
                numPages = null,
                numEditions = BookDetailConstants.TUTORIAL_BOOK_EDITION_COUNT,
                purchased = false,
                spineColor = BookColorGenerator.generateSpineColor(),
                readingStatus = ReadingStatus.READ,
                personalRating = BookDetailConstants.TUTORIAL_BOOK_PERSONAL_RATING,
                personalNotes = "",
                dateAdded = timeProvider.currentTimeMillis(),
                purchaseDate = null,
                isbn = null,
                publisher = BookDetailConstants.TUTORIAL_BOOK_PUBLISHER,
                publishDate = null,
                internetArchiveId = null
            )

            // Persist the tutorial book as a system entity (not synced to cloud)
            when (val upsertResult = bookRepository.upsertSystemBook(tutorialBook)) {
                is Result.Success -> { /* continue */ }
                is Result.Error -> return upsertResult
            }

            // Add it to the tutorial shelf
            when (val addResult = bookshelfRepository.addBookToShelf(tutorialShelfId, tutorialBook.id)) {
                is Result.Success -> { /* continue */ }
                is Result.Error -> return addResult
            }

            tutorialBook.id
        }

        return Result.Success(bookId)
    }
}
