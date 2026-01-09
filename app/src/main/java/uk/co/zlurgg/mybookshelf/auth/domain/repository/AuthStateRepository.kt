package uk.co.zlurgg.mybookshelf.auth.domain.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface AuthStateRepository {
    suspend fun isSignedIn(): Result<Boolean, DataError.Local>
    suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local>
}
