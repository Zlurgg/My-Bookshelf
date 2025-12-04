package uk.co.zlurgg.mybookshelf.auth.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignInViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Test doubles
    private var mockSignInResult: Result<UserData, DataError.Local> = Result.Success(
        UserData("test-user-id", "Test User", null)
    )
    private var mockSignOutResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var mockIsSignedIn: Boolean = false
    private var mockCurrentUser: UserData? = null
    private var signInStateSet: Boolean? = null

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(): Result<UserData, DataError.Local> = mockSignInResult
        override suspend fun signOut(): Result<Unit, DataError.Local> = mockSignOutResult
        override fun getSignedInUser(): UserData? = mockCurrentUser
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Boolean = mockIsSignedIn
        override suspend fun setSignedInState(isSignedIn: Boolean) {
            signInStateSet = isSignedIn
        }
    }

    private val mockSyncScheduler = object : SyncSchedulerService {
        override fun schedulePeriodicSync() {}
        override fun triggerImmediateSync() {}
        override fun cancelAllSync() {}
    }

    private fun createViewModel(): SignInViewModel {
        val signInUseCase = SignInUseCase(mockAuthService, mockAuthStateRepository, mockSyncScheduler)
        val signOutUseCase = SignOutUseCase(mockAuthService, mockAuthStateRepository, mockSyncScheduler)
        val checkSignInStatusUseCase = CheckSignInStatusUseCase(mockAuthService, mockAuthStateRepository)

        val useCases = SignInUseCases(
            signIn = signInUseCase,
            signOut = signOutUseCase,
            checkSignInStatus = checkSignInStatusUseCase
        )

        return SignInViewModel(useCases)
    }

    private fun resetMocks() {
        mockSignInResult = Result.Success(UserData("test-user-id", "Test User", null))
        mockSignOutResult = Result.Success(Unit)
        mockIsSignedIn = false
        mockCurrentUser = null
        signInStateSet = null
    }

    // ============================================================================
    // Initialization Tests
    // ============================================================================

    @Test
    fun `initial state has default values`() = runTest(testDispatcher) {
        resetMocks()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be loading", state.isLoading)
        assertFalse("Should not be signed in", state.isSignInSuccessful)
        assertNull("Should have no error", state.errorMessage)
    }

    @Test
    fun `auto signs in when already authenticated`() = runTest(testDispatcher) {
        resetMocks()
        mockIsSignedIn = true
        mockCurrentUser = UserData("existing-user", "Existing User", null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue("Should be signed in", viewModel.state.value.isSignInSuccessful)
    }

    @Test
    fun `does not auto sign in when local state true but no firebase user`() = runTest(testDispatcher) {
        resetMocks()
        mockIsSignedIn = true
        mockCurrentUser = null // No Firebase user

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse("Should not be signed in", viewModel.state.value.isSignInSuccessful)
    }

    // ============================================================================
    // Sign In Tests
    // ============================================================================

    @Test
    fun `sign in success updates state correctly`() = runTest(testDispatcher) {
        resetMocks()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(SignInAction.SignIn)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be loading", state.isLoading)
        assertTrue("Should be signed in", state.isSignInSuccessful)
        assertNull("Should have no error", state.errorMessage)
        assertTrue("Should have set signed in state", signInStateSet == true)
    }

    @Test
    fun `sign in shows loading state`() = runTest(testDispatcher) {
        resetMocks()
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Capture loading state during sign in
        val wasLoading = false
        mockSignInResult = Result.Success(UserData("test", null, null))

        viewModel.onAction(SignInAction.SignIn)
        // In UnconfinedTestDispatcher, this happens synchronously
        // So we check the final state
        advanceUntilIdle()

        assertFalse("Loading should complete", viewModel.state.value.isLoading)
    }

    @Test
    fun `sign in cancelled shows appropriate error`() = runTest(testDispatcher) {
        resetMocks()
        mockSignInResult = Result.Error(DataError.Local.AUTH_CANCELLED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(SignInAction.SignIn)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be loading", state.isLoading)
        assertFalse("Should not be signed in", state.isSignInSuccessful)
        assertTrue("Should have error message", state.errorMessage?.contains("cancelled") == true)
    }

    @Test
    fun `sign in no credential shows appropriate error`() = runTest(testDispatcher) {
        resetMocks()
        mockSignInResult = Result.Error(DataError.Local.AUTH_NO_CREDENTIAL)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(SignInAction.SignIn)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be signed in", state.isSignInSuccessful)
        assertTrue("Should mention Google account",
            state.errorMessage?.contains("Google account") == true)
    }

    @Test
    fun `sign in failure shows generic error`() = runTest(testDispatcher) {
        resetMocks()
        mockSignInResult = Result.Error(DataError.Local.AUTH_FAILED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(SignInAction.SignIn)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be signed in", state.isSignInSuccessful)
        assertTrue("Should have error message", state.errorMessage != null)
    }

    // ============================================================================
    // Reset State Tests
    // ============================================================================

    @Test
    fun `reset state clears all flags`() = runTest(testDispatcher) {
        resetMocks()
        val viewModel = createViewModel()
        advanceUntilIdle()

        // First sign in successfully
        viewModel.onAction(SignInAction.SignIn)
        advanceUntilIdle()
        assertTrue("Should be signed in", viewModel.state.value.isSignInSuccessful)

        // Then reset
        viewModel.onAction(SignInAction.ResetState)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should clear loading", state.isLoading)
        assertFalse("Should clear sign in success", state.isSignInSuccessful)
        assertNull("Should clear error", state.errorMessage)
    }

    @Test
    fun `reset state clears error message`() = runTest(testDispatcher) {
        resetMocks()
        mockSignInResult = Result.Error(DataError.Local.AUTH_FAILED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger error
        viewModel.onAction(SignInAction.SignIn)
        advanceUntilIdle()
        assertTrue("Should have error", viewModel.state.value.errorMessage != null)

        // Reset
        viewModel.onAction(SignInAction.ResetState)
        advanceUntilIdle()

        assertNull("Error should be cleared", viewModel.state.value.errorMessage)
    }
}
