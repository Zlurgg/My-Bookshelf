package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class RemoveUserFromClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : RemoveUserFromClubUseCase {
    override suspend fun invoke(code: String, userId: String): Result<Unit, DataError.Sync> {
        return bookClubRepository.removeUserFromClub(code, userId)
    }
}
