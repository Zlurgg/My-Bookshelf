package uk.co.zlurgg.mybookshelf.account.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface DeleteAccountUseCase {
    suspend operator fun invoke(): Result<Unit, DataError>

    /**
     * Retries account deletion after re-authentication.
     * Must only be called after [invoke] returned [DataError.Local.REQUIRES_RECENT_LOGIN],
     * meaning clubs are cleaned and all remote data is already deleted.
     *
     * @param idToken Fresh Google ID token from re-authentication
     */
    suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError>
}
