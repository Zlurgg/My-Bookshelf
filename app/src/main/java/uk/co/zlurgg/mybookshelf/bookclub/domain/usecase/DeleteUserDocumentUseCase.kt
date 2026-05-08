package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface DeleteUserDocumentUseCase {
    suspend operator fun invoke(userId: String): Result<Unit, DataError.Sync>
}
