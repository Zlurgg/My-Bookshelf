package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DeleteShelfUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FakeDeleteShelfUseCase : DeleteShelfUseCase {

    override suspend fun execute(shelfId: String): Result<Unit, DataError.Local> {
        return Result.Success(Unit)
    }

    override suspend fun restore(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return Result.Success(Unit)
    }
}