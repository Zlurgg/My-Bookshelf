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
     * Performs sign-in using a pre-fetched Google ID token.
     * Credential fetching is a UI concern handled at the presentation layer.
     */
    suspend fun signIn(idToken: String): Result<UserData, DataError.Local>

    suspend fun signOut(): Result<Unit, DataError.Local>

    fun getSignedInUser(): UserData?
}
