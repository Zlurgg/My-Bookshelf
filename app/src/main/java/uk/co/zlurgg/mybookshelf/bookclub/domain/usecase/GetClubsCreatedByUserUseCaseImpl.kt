package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GetClubsCreatedByUserUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : GetClubsCreatedByUserUseCase {
    override suspend fun invoke(userId: String): Result<List<String>, DataError.Sync> {
        return bookClubRepository.getClubsCreatedByUser(userId)
    }
}
