package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.onboarding

import uk.co.zlurgg.mybookshelf.bookshelf.data.service.OnboardingService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class InitializeOnboardingUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val onboardingService: OnboardingService,
    private val idGenerator: IdGenerator,
    private val getOrCreateTutorialBook: GetOrCreateTutorialBookUseCase
) : InitializeOnboardingUseCase {

    override suspend fun execute(): Result<Unit, DataError> {
        return ErrorMapper.safeCall {
            // Check if this is first launch
            if (!onboardingService.isFirstLaunch()) {
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

            // Mark onboarding complete
            onboardingService.markFirstLaunchComplete()
        }
    }
}