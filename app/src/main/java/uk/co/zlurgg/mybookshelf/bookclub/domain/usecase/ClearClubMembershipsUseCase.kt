package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface ClearClubMembershipsUseCase {
    suspend operator fun invoke(): Result<Unit, DataError.Local>
}
