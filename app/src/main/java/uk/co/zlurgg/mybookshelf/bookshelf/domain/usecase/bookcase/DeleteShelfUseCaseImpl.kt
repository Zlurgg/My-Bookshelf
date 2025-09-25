package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.Result

class DeleteShelfUseCaseImpl(
    private val repository: BookcaseRepository
) : DeleteShelfUseCase {

    override suspend fun execute(shelfId: String): Result<Unit, DataError.Local> {
        return try {
            repository.removeShelf(shelfId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }

    override suspend fun restore(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return try {
            repository.addShelf(shelf)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}