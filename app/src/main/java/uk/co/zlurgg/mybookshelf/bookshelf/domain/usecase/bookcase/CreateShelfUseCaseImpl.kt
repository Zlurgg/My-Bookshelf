package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.Result

class CreateShelfUseCaseImpl(
    private val repository: BookcaseRepository,
    private val idGenerator: IdGenerator
) : CreateShelfUseCase {

    override suspend fun execute(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>
    ): Result<Bookshelf, DataError.Local> {
        return try {
            val nextPosition = existingShelves.maxOfOrNull { it.position }?.plus(1) ?: 0
            val newShelf = Bookshelf(
                id = idGenerator.generateId(),
                name = name,
                books = emptyList(),
                shelfStyle = style,
                position = nextPosition
            )

            repository.addShelf(newShelf)
            Result.Success(newShelf)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}