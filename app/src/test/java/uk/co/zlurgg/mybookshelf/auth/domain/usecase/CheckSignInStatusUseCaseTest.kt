package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class CheckSignInStatusUseCaseTest {

    // Test doubles
    private var mockIsSignedIn: Boolean = false
    private var mockCurrentUser: UserData? = null

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(): Result<UserData, DataError.Local> = Result.Success(
            UserData("test-user-id", "Test User", null)
        )
        override suspend fun signOut(): Result<Unit, DataError.Local> = Result.Success(Unit)
        override fun getSignedInUser(): UserData? = mockCurrentUser
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Result<Boolean, DataError.Local> =
            Result.Success(mockIsSignedIn)
        override suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local> =
            Result.Success(Unit)
    }

    private lateinit var useCase: CheckSignInStatusUseCase

    @Before
    fun setup() {
        mockIsSignedIn = false
        mockCurrentUser = null
        useCase = CheckSignInStatusUseCaseImpl(mockAuthService, mockAuthStateRepository)
    }

    @Test
    fun `execute returns true when local state is true and firebase user exists`() = runTest {
        mockIsSignedIn = true
        mockCurrentUser = UserData("user-123", "Test User", null)

        val result = useCase()

        assertTrue("Should be signed in when both conditions are true", result)
    }

    @Test
    fun `execute returns false when local state is true but firebase user is null`() = runTest {
        mockIsSignedIn = true
        mockCurrentUser = null

        val result = useCase()

        assertFalse("Should not be signed in when firebase user is null", result)
    }

    @Test
    fun `execute returns false when local state is false but firebase user exists`() = runTest {
        mockIsSignedIn = false
        mockCurrentUser = UserData("user-123", "Test User", null)

        val result = useCase()

        assertFalse("Should not be signed in when local state is false", result)
    }

    @Test
    fun `execute returns false when both local state is false and firebase user is null`() = runTest {
        mockIsSignedIn = false
        mockCurrentUser = null

        val result = useCase()

        assertFalse("Should not be signed in when both conditions are false", result)
    }

    @Test
    fun `execute checks both local state and firebase user`() = runTest {
        // Start with both false
        mockIsSignedIn = false
        mockCurrentUser = null
        assertFalse(useCase())

        // Only local true
        mockIsSignedIn = true
        mockCurrentUser = null
        assertFalse(useCase())

        // Only firebase true
        mockIsSignedIn = false
        mockCurrentUser = UserData("user-123", "Test User", null)
        assertFalse(useCase())

        // Both true
        mockIsSignedIn = true
        mockCurrentUser = UserData("user-123", "Test User", null)
        assertTrue(useCase())
    }
}
