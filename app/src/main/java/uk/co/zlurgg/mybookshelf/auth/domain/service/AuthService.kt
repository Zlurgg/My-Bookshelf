package uk.co.zlurgg.mybookshelf.auth.domain.service

import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Service interface for authentication operations.
 * Implementation handles platform-specific sign-in flows.
 */
interface AuthService {
    /**
     * Performs sign-in. Implementation requires Activity context
     * which is provided at the data layer.
     */
    suspend fun signIn(): Result<UserData, DataError.Local>

    suspend fun signOut(): Result<Unit, DataError.Local>

    fun getSignedInUser(): UserData?
}
