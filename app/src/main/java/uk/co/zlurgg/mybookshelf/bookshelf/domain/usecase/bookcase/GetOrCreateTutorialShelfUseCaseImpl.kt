package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GetOrCreateTutorialShelfUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val idGenerator: IdGenerator
) : GetOrCreateTutorialShelfUseCase {

    override suspend fun execute(): Result<String, DataError.Local> {
        return ErrorMapper.safeCall {
            // Get all shelves to search for tutorial shelf
            val existingShelves = bookcaseRepository.getAllShelves().first()
            val tutorialShelf = existingShelves.find { it.name == BookshelfConstants.TUTORIAL_SHELF_NAME }

            if (tutorialShelf != null) {
                // Tutorial shelf exists, return its ID
                tutorialShelf.id
            } else {
                // Create new tutorial shelf at position 0 with random style
                val randomStyle = ShelfStyle.entries.random()
                val newTutorialShelf = Bookshelf(
                    id = idGenerator.generateId(),
                    name = BookshelfConstants.TUTORIAL_SHELF_NAME,
                    shelfStyle = randomStyle,
                    position = 0,
                    books = emptyList()
                )

                bookcaseRepository.addShelf(newTutorialShelf)
                newTutorialShelf.id
            }
        }
    }
}