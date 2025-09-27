package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class CreateShelfUseCaseImpl(
    private val repository: BookcaseRepository,
    private val idGenerator: IdGenerator
) : CreateShelfUseCase {

    override suspend fun execute(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>
    ): Result<Bookshelf, DataError.Local> {
        return ErrorMapper.safeCall {
            val nextPosition = existingShelves.maxOfOrNull { it.position }?.plus(1) ?: 0
            val newShelf = Bookshelf(
                id = idGenerator.generateId(),
                name = name,
                books = emptyList(),
                shelfStyle = style,
                position = nextPosition
            )

            repository.addShelf(newShelf)
            newShelf
        }
    }
}