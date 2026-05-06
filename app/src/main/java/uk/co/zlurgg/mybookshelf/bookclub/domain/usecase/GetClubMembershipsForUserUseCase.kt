package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface GetClubMembershipsForUserUseCase {
    suspend operator fun invoke(userId: String): Result<List<String>, DataError.Sync>
}
