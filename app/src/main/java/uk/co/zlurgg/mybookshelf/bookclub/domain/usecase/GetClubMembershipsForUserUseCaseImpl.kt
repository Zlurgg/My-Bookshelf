package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GetClubMembershipsForUserUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : GetClubMembershipsForUserUseCase {
    override suspend fun invoke(userId: String): Result<List<String>, DataError.Sync> {
        return bookClubRepository.getRemoteClubMemberships(userId)
    }
}
