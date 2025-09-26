package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FakeShareBookshelfUseCase : ShareBookshelfUseCase {

    override suspend fun execute(shelfId: String): Result<Unit, DataError.Local> {
        return Result.Success(Unit)
    }
}