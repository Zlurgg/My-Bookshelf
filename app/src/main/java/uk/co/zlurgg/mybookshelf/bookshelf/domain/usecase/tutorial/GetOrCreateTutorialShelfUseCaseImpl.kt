package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GetOrCreateTutorialShelfUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val getOrCreateTutorialBook: GetOrCreateTutorialBookUseCase
) : GetOrCreateTutorialShelfUseCase {

    override suspend fun execute(): Result<String, DataError.Local> {
        // Check if tutorial shelf already exists (using fixed ID)
        val existingShelf = when (val getResult = bookcaseRepository.getShelfById(SystemOwnerIds.TUTORIAL_SHELF_ID)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        val shelfId = if (existingShelf != null) {
            // Tutorial shelf exists, use its ID
            existingShelf.id
        } else {
            // Create new tutorial shelf with fixed ID and system owner
            val randomStyle = ShelfStyle.entries.random()
            val newTutorialShelf = Bookshelf(
                id = SystemOwnerIds.TUTORIAL_SHELF_ID,
                name = BookshelfConstants.TUTORIAL_SHELF_NAME,
                shelfStyle = randomStyle,
                position = 0,
                books = emptyList()
            )

            when (val addResult = bookcaseRepository.addSystemShelf(newTutorialShelf)) {
                is Result.Success -> { /* continue */ }
                is Result.Error -> return addResult
            }
            newTutorialShelf.id
        }

        // Ensure tutorial book exists in the shelf
        when (val bookResult = getOrCreateTutorialBook.execute(shelfId)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return bookResult
        }

        return Result.Success(shelfId)
    }
}
