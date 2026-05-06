package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class UpdateClubStyleUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : UpdateClubStyleUseCase {
    override suspend fun invoke(code: String, style: String): Result<Unit, DataError.Sync> {
        return bookClubRepository.updateClubStyle(code, style)
    }
}
