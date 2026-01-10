package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Development-only use case for signing in with test users via Firebase Auth Emulator.
 * Only available in debug builds.
 */
interface DevSignInUseCase {
    /**
     * Signs in as a test user for development/testing purposes.
     *
     * @param userNumber Which test user to sign in as (1=Alice, 2=Bob, 3=Charlie)
     * @return Result containing UserData on success or DataError on failure
     */
    suspend fun execute(userNumber: Int = 1): Result<UserData, DataError.Local>
}
