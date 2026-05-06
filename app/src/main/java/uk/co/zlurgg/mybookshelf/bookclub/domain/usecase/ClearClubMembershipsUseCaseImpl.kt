package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ClearClubMembershipsUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : ClearClubMembershipsUseCase {
    override suspend fun invoke(): Result<Unit, DataError.Local> {
        return bookClubRepository.clearAllMemberships()
    }
}
