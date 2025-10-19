package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookDetailConstants
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * UseCase to get existing tutorial book or create it if it doesn't exist.
 * Creates a welcome book with app usage instructions and adds it to the tutorial shelf.
 */
class GetOrCreateTutorialBookUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val timeProvider: TimeProvider
) : GetOrCreateTutorialBookUseCase {

    override suspend fun execute(tutorialShelfId: String): Result<String, DataError.Local> {
        return ErrorMapper.safeCall {
            // Check if tutorial book already exists
            val existingBook = bookRepository.getBookById(BookDetailConstants.TUTORIAL_BOOK_ID)

            val bookId = if (existingBook != null) {
                // Tutorial book exists, ensure it's on the tutorial shelf
                val isOnShelf = bookshelfRepository
                    .isBookOnShelf(existingBook.id, tutorialShelfId)
                    .first()

                if (!isOnShelf) {
                    bookshelfRepository.addBookToShelf(tutorialShelfId, existingBook.id)
                }

                existingBook.id
            } else {
                // Create new tutorial book
                val tutorialBook = Book(
                    id = BookDetailConstants.TUTORIAL_BOOK_ID,
                    title = BookDetailConstants.TUTORIAL_BOOK_TITLE,
                    authors = listOf(BookDetailConstants.TUTORIAL_BOOK_AUTHOR),
                    imageUrl = "", // No cover image for tutorial book
                    description = BookDetailConstants.TUTORIAL_BOOK_DESCRIPTION,
                    languages = listOf("en"),
                    firstPublishYear = null,
                    averageRating = 5.0,
                    ratingCount = 1,
                    numPages = null,
                    numEditions = 1,
                    purchased = false,
                    spineColor = BookColorGenerator.generateSpineColor(),
                    readingStatus = ReadingStatus.READ,
                    personalRating = 5f,
                    personalNotes = "Welcome to My Bookshelf! Read this book to learn how to use the app.",
                    dateAdded = timeProvider.currentTimeMillis(),
                    purchaseDate = null,
                    isbn = null,
                    publisher = BookDetailConstants.TUTORIAL_BOOK_PUBLISHER,
                    publishDate = null,
                    internetArchiveId = null
                )

                // Persist the tutorial book
                bookRepository.upsertBook(tutorialBook)

                // Add it to the tutorial shelf
                bookshelfRepository.addBookToShelf(tutorialShelfId, tutorialBook.id)

                tutorialBook.id
            }

            bookId
        }
    }
}
