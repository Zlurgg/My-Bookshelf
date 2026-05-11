package uk.co.zlurgg.mybookshelf.account.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface DeleteAccountUseCase {
    suspend operator fun invoke(): Result<Unit, DataError>

    /**
     * Retries account deletion after re-authentication.
     * Called after [invoke] returned [DataError.Local.REQUIRES_RECENT_LOGIN].
     * Re-attempts user document deletion defensively (may already be deleted
     * from initial invoke), then deletes Firebase Auth and cleans up locally.
     *
     * @param idToken Fresh Google ID token from re-authentication
     */
    suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError>
}
