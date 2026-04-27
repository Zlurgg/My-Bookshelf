package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.book.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.book.domain.util.BookDetailConstants
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

    override suspend operator fun invoke(tutorialShelfId: String): Result<String, DataError.Local> {
        val existingBook = when (val getResult = bookRepository.getBookById(BookDetailConstants.TUTORIAL_BOOK_ID)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        return if (existingBook != null) {
            ensureBookOnShelf(existingBook.id, tutorialShelfId)
        } else {
            createAndAddTutorialBook(tutorialShelfId)
        }
    }

    private suspend fun ensureBookOnShelf(bookId: String, shelfId: String): Result<String, DataError.Local> {
        val isOnShelf = bookshelfRepository.isBookOnShelf(bookId, shelfId).first()
        if (!isOnShelf) {
            when (val addResult = bookshelfRepository.addBookToShelf(shelfId, bookId)) {
                is Result.Success -> { /* continue */ }
                is Result.Error -> return addResult
            }
        }
        return Result.Success(bookId)
    }

    private suspend fun createAndAddTutorialBook(shelfId: String): Result<String, DataError.Local> {
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

        when (val upsertResult = bookRepository.upsertSystemBook(tutorialBook)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return upsertResult
        }

        when (val addResult = bookshelfRepository.addBookToShelf(shelfId, tutorialBook.id)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return addResult
        }

        return Result.Success(tutorialBook.id)
    }
}
