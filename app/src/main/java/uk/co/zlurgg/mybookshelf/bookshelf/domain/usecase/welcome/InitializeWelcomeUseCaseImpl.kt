package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome

import uk.co.zlurgg.mybookshelf.bookshelf.data.service.WelcomeService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialBookUseCase

class InitializeWelcomeUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val welcomeService: WelcomeService,
    private val getOrCreateTutorialBook: GetOrCreateTutorialBookUseCase
) : InitializeWelcomeUseCase {

    override suspend fun execute(): Result<Unit, DataError> {
        return ErrorMapper.safeCall {
            // Check if this is first launch
            if (!welcomeService.isFirstLaunch()) {
                return@safeCall
            }

            // Check if tutorial shelf already exists (from previous install or help icon)
            val existingShelf = bookcaseRepository.getShelfById(SystemOwnerIds.TUTORIAL_SHELF_ID)

            if (existingShelf == null) {
                // Create tutorial shelf with fixed ID and system owner
                val randomStyle = ShelfStyle.entries.random()
                val tutorialShelf = Bookshelf(
                    id = SystemOwnerIds.TUTORIAL_SHELF_ID,
                    name = BookshelfConstants.TUTORIAL_SHELF_NAME,
                    shelfStyle = randomStyle,
                    position = 0,
                    books = emptyList()
                )

                // Add shelf to repository as system shelf
                bookcaseRepository.addSystemShelf(tutorialShelf)

                // Create and add tutorial book to the shelf
                getOrCreateTutorialBook.execute(tutorialShelf.id)
            }

            // Mark welcome complete
            welcomeService.markFirstLaunchComplete()
        }
    }
}
