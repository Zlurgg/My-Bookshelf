package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class SignOutUseCaseTest {

    // Test doubles
    private var mockSignOutResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var signedInStateSet: Boolean? = null

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(idToken: String): Result<UserData, DataError.Local> = Result.Success(
            UserData("test-user-id", "Test User", null)
        )
        override suspend fun signOut(): Result<Unit, DataError.Local> = mockSignOutResult
        override fun getSignedInUser(): UserData? = null
        override suspend fun deleteAccount(): Result<Unit, DataError.Local> = Result.Error(DataError.Local.AUTH_FAILED)
        override suspend fun reauthenticate(
            idToken: String,
        ): Result<Unit, DataError.Local> = Result.Error(DataError.Local.AUTH_FAILED)
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Result<Boolean, DataError.Local> =
            Result.Success(true)
        override suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local> {
            signedInStateSet = isSignedIn
            return Result.Success(Unit)
        }
    }

    private lateinit var useCase: SignOutUseCase

    @Before
    fun setup() {
        signedInStateSet = null
        mockSignOutResult = Result.Success(Unit)
        useCase = SignOutUseCaseImpl(
            mockAuthService,
            mockAuthStateRepository,
        )
    }

    @Test
    fun `execute returns success and clears signed in state when sign out succeeds`() = runTest {
        val result = useCase()

        assertTrue("Should be success", result is Result.Success)
        assertEquals(false, signedInStateSet)
    }

    @Test
    fun `execute returns error and does not modify state when sign out fails`() = runTest {
        mockSignOutResult = Result.Error(DataError.Local.AUTH_FAILED)

        val result = useCase()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_FAILED, (result as Result.Error).error)
        assertEquals(null, signedInStateSet) // State should not be modified on failure
    }

    @Test
    fun `execute returns success with Unit data`() = runTest {
        val result = useCase()

        assertTrue("Should be success", result is Result.Success)
        assertEquals(Unit, (result as Result.Success).data)
    }

    @Test
    fun `execute sets signed in state to false on success`() = runTest {
        useCase()

        assertEquals(false, signedInStateSet)
    }

    @Test
    fun `execute preserves error type from auth service`() = runTest {
        mockSignOutResult = Result.Error(DataError.Local.AUTH_NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_NETWORK_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `execute does not clear club memberships or delete club shelves`() = runTest {
        // Sign-out should only handle auth concerns — club data persists for guest access.
        // Club cleanup is handled by DeleteAccountUseCaseImpl only.
        val result = useCase()

        assertTrue("Should be success", result is Result.Success)
        // If this test compiles and passes, it confirms SignOutUseCaseImpl
        // no longer depends on ClubOperations or BookcaseRepository.
    }
}
