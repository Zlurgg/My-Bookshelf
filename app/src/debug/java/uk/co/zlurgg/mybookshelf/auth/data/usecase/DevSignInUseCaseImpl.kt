package uk.co.zlurgg.mybookshelf.auth.data.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.data.service.DevAuthService
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.DevSignInUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of [DevSignInUseCase] for debug builds.
 * Signs in using Firebase Auth Emulator and sets local auth state.
 */
class DevSignInUseCaseImpl(
    private val devAuthService: DevAuthService,
    private val authStateRepository: AuthStateRepository
) : DevSignInUseCase {

    companion object {
        private const val TAG = "DevSignIn"
    }

    override suspend operator fun invoke(userNumber: Int): Result<UserData, DataError.Local> {
        Timber.tag(TAG).d("Dev sign-in starting for user %d", userNumber)

        return when (val result = devAuthService.signInAsTestUser(userNumber)) {
            is Result.Success -> {
                // Set local auth state (required for sign-in checks)
                authStateRepository.setSignedInState(true)
                Timber.tag(TAG).d("Dev sign-in successful: %s", result.data.userId)
                result
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Dev sign-in failed: %s", result.error)
                result
            }
        }
    }
}
