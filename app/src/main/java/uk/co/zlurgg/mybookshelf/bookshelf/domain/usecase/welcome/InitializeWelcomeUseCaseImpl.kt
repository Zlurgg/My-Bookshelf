package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome

import uk.co.zlurgg.mybookshelf.bookshelf.data.service.WelcomeService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.GetOrCreateTutorialBookUseCase

class InitializeWelcomeUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val welcomeService: WelcomeService,
    private val idGenerator: IdGenerator,
    private val getOrCreateTutorialBook: GetOrCreateTutorialBookUseCase
) : InitializeWelcomeUseCase {

    override suspend fun execute(): Result<Unit, DataError> {
        return ErrorMapper.safeCall {
            // Check if this is first launch
            if (!welcomeService.isFirstLaunch()) {
                return@safeCall
            }

            // Business logic: Create tutorial shelf with random style
            val randomStyle = ShelfStyle.entries.random()
            val tutorialShelf = Bookshelf(
                id = idGenerator.generateId(),
                name = BookshelfConstants.TUTORIAL_SHELF_NAME,
                shelfStyle = randomStyle,
                position = 0,
                books = emptyList()
            )

            // Add shelf to repository
            bookcaseRepository.addShelf(tutorialShelf)

            // Create and add tutorial book to the shelf
            getOrCreateTutorialBook.execute(tutorialShelf.id)

            // Mark welcome complete
            welcomeService.markFirstLaunchComplete()
        }
    }
}
