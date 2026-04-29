package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Reusable mock AuthService for testing.
 * Provides configurable behavior for testing different authentication scenarios.
 */
class MockAuthService : AuthService {

    // Configuration properties
    private var currentUser: UserData? = null
    var signInResult: Result<UserData, DataError.Local> = Result.Error(DataError.Local.AUTH_FAILED)
    var signOutResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    var deleteAccountResult: Result<Unit, DataError.Local> = Result.Error(DataError.Local.AUTH_FAILED)
    var reauthenticateResult: Result<Unit, DataError.Local> = Result.Success(Unit)

    // Tracking properties
    var signInCalled = false
    var signOutCalled = false
    var deleteAccountCalled = false
    var reauthenticateCalled = false

    override suspend fun signIn(idToken: String): Result<UserData, DataError.Local> {
        signInCalled = true
        return signInResult
    }

    override suspend fun signOut(): Result<Unit, DataError.Local> {
        signOutCalled = true
        return signOutResult
    }

    override fun getSignedInUser(): UserData? {
        return currentUser
    }

    override suspend fun deleteAccount(): Result<Unit, DataError.Local> {
        deleteAccountCalled = true
        return deleteAccountResult
    }

    override suspend fun reauthenticate(idToken: String): Result<Unit, DataError.Local> {
        reauthenticateCalled = true
        return reauthenticateResult
    }

    // Helper methods for test setup
    fun reset() {
        currentUser = null
        signInResult = Result.Error(DataError.Local.AUTH_FAILED)
        signOutResult = Result.Success(Unit)
        signInCalled = false
        signOutCalled = false
        deleteAccountCalled = false
        reauthenticateCalled = false
        deleteAccountResult = Result.Error(DataError.Local.AUTH_FAILED)
        reauthenticateResult = Result.Success(Unit)
    }

    fun configureSignedIn(userId: String = "test-user", username: String = "Test User") {
        currentUser = UserData(userId = userId, username = username, profilePictureUrl = null)
    }

    fun configureSignedOut() {
        currentUser = null
    }
}
