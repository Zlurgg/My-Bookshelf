package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GetShelfByIdUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository
) : GetShelfByIdUseCase {

    override suspend fun execute(shelfId: String): Result<Bookshelf?, DataError.Local> {
        return try {
            val shelf = bookcaseRepository.getShelfById(shelfId)
            Result.Success(shelf)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}
