package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.CreateShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FakeCreateShelfUseCase : CreateShelfUseCase {

    override suspend fun execute(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>
    ): Result<Bookshelf, DataError.Local> {
        return Result.Success(
            Bookshelf(
                id = "fake-shelf-id",
                name = name,
                books = emptyList(),
                shelfStyle = style,
                position = 0
            )
        )
    }
}