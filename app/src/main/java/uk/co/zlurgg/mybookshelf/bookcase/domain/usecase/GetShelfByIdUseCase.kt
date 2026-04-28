package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface GetShelfByIdUseCase {
    suspend operator fun invoke(shelfId: String): Result<Bookshelf?, DataError.Local>
}
