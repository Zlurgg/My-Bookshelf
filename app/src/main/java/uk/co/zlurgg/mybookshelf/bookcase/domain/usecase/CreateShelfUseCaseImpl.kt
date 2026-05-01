package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.GetOrCreateTutorialBookUseCase
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator

// Depends on welcome/ for tutorial book creation when a tutorial shelf is made.
// This cross-feature dependency is intentional and injected via Koin.
class CreateShelfUseCaseImpl(
    private val repository: BookcaseRepository,
    private val idGenerator: IdGenerator,
    private val getOrCreateTutorialBook: GetOrCreateTutorialBookUseCase,
) : CreateShelfUseCase {

    override suspend operator fun invoke(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>
    ): Result<Bookshelf, DataError.Local> {
        val nextPosition = existingShelves.maxOfOrNull { it.position }?.plus(1) ?: 0
        val newShelf = Bookshelf(
            id = idGenerator.generateId(),
            name = name,
            books = emptyList(),
            shelfStyle = style,
            position = nextPosition
        )

        when (val addResult = repository.addShelf(newShelf)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return addResult
        }

        // If this is the tutorial shelf, ensure the tutorial book is added
        if (name == BookshelfConstants.TUTORIAL_SHELF_NAME) {
            getOrCreateTutorialBook(newShelf.id)
        }

        return Result.Success(newShelf)
    }
}
