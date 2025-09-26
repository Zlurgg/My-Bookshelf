package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FakeGetShelfByIdUseCase : GetShelfByIdUseCase {

    override suspend fun execute(shelfId: String): Result<Bookshelf?, DataError.Local> {
        return Result.Success(
            Bookshelf(
                id = shelfId,
                name = "Test Shelf",
                books = emptyList(),
                shelfStyle = ShelfStyle.DarkWood,
                position = 0
            )
        )
    }
}