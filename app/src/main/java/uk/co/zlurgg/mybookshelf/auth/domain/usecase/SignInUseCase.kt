package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface SignInUseCase {
    suspend operator fun invoke(): Result<UserData, DataError.Local>
}
