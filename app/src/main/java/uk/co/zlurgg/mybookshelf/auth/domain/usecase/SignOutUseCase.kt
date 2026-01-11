package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface SignOutUseCase {
    suspend operator fun invoke(): Result<Unit, DataError.Local>
}
