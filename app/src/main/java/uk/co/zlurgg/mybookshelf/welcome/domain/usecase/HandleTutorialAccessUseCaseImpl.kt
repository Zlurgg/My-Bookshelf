package uk.co.zlurgg.mybookshelf.welcome.domain.usecase

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase to handle tutorial access from the help (?) button.
 * Orchestrates checking for existing tutorial, creating if needed,
 * and determining navigation behavior.
 *
 * If tutorial doesn't exist: Creates shelf and book silently, returns DoNotNavigate
 * If tutorial exists: Returns NavigateToBook with shelf and book IDs
 */
class HandleTutorialAccessUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val getOrCreateTutorialShelf: GetOrCreateTutorialShelfUseCase,
    private val getOrCreateTutorialBook: GetOrCreateTutorialBookUseCase
) : HandleTutorialAccessUseCase {

    @Suppress("TooGenericExceptionThrown") // Intentional: throws within safeCall which converts to Result.Error
    override suspend operator fun invoke(): Result<TutorialAccessResult, DataError.Local> {
        return ErrorMapper.safeCall {
            // Check if tutorial shelf already exists
            val existingShelves = bookcaseRepository.getAllShelves().first()
            val tutorialShelf = existingShelves.find { it.name == BookshelfConstants.TUTORIAL_SHELF_NAME }

            if (tutorialShelf != null) {
                // Tutorial shelf exists - navigate to tutorial book
                val tutorialBook = tutorialShelf.books.firstOrNull()

                val bookId = tutorialBook?.id ?: // Book exists, use ID if it doesn't exist, create it
                    when (val result = getOrCreateTutorialBook(tutorialShelf.id)) {
                        is Result.Success -> result.data
                        is Result.Error -> throw Exception("Failed to create tutorial book: ${result.error}")
                    }

                TutorialAccessResult.NavigateToBook(
                    shelfId = tutorialShelf.id,
                    bookId = bookId
                )
            } else {
                // Tutorial doesn't exist, create silently without navigating
                when (val result = getOrCreateTutorialShelf()) {
                    is Result.Success -> TutorialAccessResult.DoNotNavigate
                    is Result.Error -> throw Exception("Failed to create tutorial shelf: ${result.error}")
                }
            }
        }
    }
}
