package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class DeleteBookClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : DeleteBookClubUseCase {
    override suspend fun invoke(code: String): Result<Unit, DataError.Sync> {
        return bookClubRepository.deleteBookClub(code)
    }
}
