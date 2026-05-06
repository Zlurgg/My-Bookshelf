package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface DeleteBookClubUseCase {
    suspend operator fun invoke(code: String): Result<Unit, DataError.Sync>
}
