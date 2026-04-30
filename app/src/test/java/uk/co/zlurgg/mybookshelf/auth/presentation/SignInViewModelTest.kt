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
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetCurrentUserIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetSignedInUserUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.ResumeSessionUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.ClearUserDataUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.ShouldShowWelcomeUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.GuestDataInfo
import uk.co.zlurgg.mybookshelf.sync.domain.model.MigrationResult
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.HasGuestDataUseCase
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.MigrateLocalDataUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncRepository

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignInViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Test doubles
    private var mockCredentialResult: Result<String, DataError.Local> = Result.Success("test-id-token")
    private var mockSignInResult: Result<UserData, DataError.Local> = Result.Success(
        UserData("test-user-id", "Test User", null)
    )
    private var mockSignOutResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var mockIsSignedIn: Boolean = false
    private var mockCurrentUser: UserData? = null
    private var signInStateSet: Boolean? = null

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(idToken: String): Result<UserData, DataError.Local> = mockSignInResult
        override suspend fun signOut(): Result<Unit, DataError.Local> = mockSignOutResult
        override fun getSignedInUser(): UserData? = mockCurrentUser
        override suspend fun deleteAccount(): Result<Unit, DataError.Local> =
            Result.Error(DataError.Local.AUTH_FAILED)

        override suspend fun reauthenticate(
            idToken: String,
        ): Result<Unit, DataError.Local> =
            Result.Error(DataError.Local.AUTH_FAILED)
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Result<Boolean, DataError.Local> =
            Result.Success(mockIsSignedIn)
        override suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local> {
            signInStateSet = isSignedIn
            return Result.Success(Unit)
        }
    }

    private val mockSyncScheduler = object : SyncSchedulerService {
        override fun schedulePeriodicSync() = Unit
        override fun triggerImmediateSync() = Unit
        override fun cancelAllSync() = Unit
    }

    private var resumeSessionCallCount = 0
    private val mockResumeSession = object : ResumeSessionUseCase {
        override suspend fun invoke() {
            resumeSessionCallCount++
        }
    }

    private var mockMigrationResult: Result<MigrationResult, DataError.Sync> = Result.Success(
        MigrationResult.NO_MIGRATION_NEEDED
    )
    private val mockMigrateLocalDataUseCase = object : MigrateLocalDataUseCase {
        override suspend operator fun invoke(): Result<MigrationResult, DataError.Sync> = mockMigrationResult
    }

    private var mockShouldShowWelcome = false
    private val mockShouldShowWelcomeUseCase = object : ShouldShowWelcomeUseCase {
        override suspend operator fun invoke(): Boolean = mockShouldShowWelcome
    }

    private var mockGuestDataInfo = GuestDataInfo(bookCount = 0, shelfCount = 0)
    private val mockHasGuestDataUseCase = object : HasGuestDataUseCase {
        override suspend operator fun invoke(): GuestDataInfo = mockGuestDataInfo
    }

    private val mockClearUserDataUseCase = object : ClearUserDataUseCase {
        override suspend operator fun invoke(userId: String): Result<Int, DataError.Local> = Result.Success(0)
    }

    private val mockCurrentUserProvider = object : CurrentUserProvider {
        override fun getCurrentUserId(): String = "test-user-id"
    }

    private val mockSyncRepository = MockSyncRepository()

    private fun createViewModel(): SignInViewModel {
        val signInUseCase = SignInUseCaseImpl(mockAuthService, mockAuthStateRepository)
        val signOutUseCase =
            SignOutUseCaseImpl(
                mockAuthService,
                mockAuthStateRepository,
                mockSyncScheduler,
                mockClearUserDataUseCase,
                mockCurrentUserProvider,
                mockSyncRepository
            )
        val checkSignInStatusUseCase = CheckSignInStatusUseCaseImpl(mockAuthService, mockAuthStateRepository)
        val getCurrentUserIdUseCase = GetCurrentUserIdUseCaseImpl(mockCurrentUserProvider)

        val useCases = AuthUseCases(
            signIn = signInUseCase,
            signOut = signOutUseCase,
            checkSignInStatus = checkSignInStatusUseCase,
            getCurrentUserId = getCurrentUserIdUseCase,
            getSignedInUser = object : GetSignedInUserUseCase {
                override fun invoke(): UserData? = null
            },
        )

        return SignInViewModel(
            useCases,
            mockShouldShowWelcomeUseCase,
            mockHasGuestDataUseCase,
            mockMigrateLocalDataUseCase,
            mockResumeSession,
        )
    }

    private fun signInAction() = SignInAction.SignIn { mockCredentialResult }

    private fun resetMocks() {
        mockCredentialResult = Result.Success("test-id-token")
        mockSignInResult = Result.Success(UserData("test-user-id", "Test User", null))
        mockSignOutResult = Result.Success(Unit)
        mockIsSignedIn = false
        mockCurrentUser = null
        signInStateSet = null
        mockShouldShowWelcome = false
        mockGuestDataInfo = GuestDataInfo(bookCount = 0, shelfCount = 0)
        mockMigrationResult = Result.Success(MigrationResult.NO_MIGRATION_NEEDED)
        resumeSessionCallCount = 0
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

        viewModel.onAction(signInAction())
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
        mockSignInResult = Result.Success(UserData("test", null, null))

        viewModel.onAction(signInAction())
        // In UnconfinedTestDispatcher, this happens synchronously
        // So we check the final state
        advanceUntilIdle()

        assertFalse("Loading should complete", viewModel.state.value.isLoading)
    }

    @Test
    fun `sign in cancelled shows appropriate error`() = runTest(testDispatcher) {
        resetMocks()
        mockCredentialResult = Result.Error(DataError.Local.AUTH_CANCELLED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(signInAction())
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be loading", state.isLoading)
        assertFalse("Should not be signed in", state.isSignInSuccessful)
        assertTrue("Should have error message", state.errorMessage?.contains("cancelled") == true)
    }

    @Test
    fun `sign in no credential shows appropriate error`() = runTest(testDispatcher) {
        resetMocks()
        mockCredentialResult = Result.Error(DataError.Local.AUTH_NO_CREDENTIAL)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(signInAction())
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be signed in", state.isSignInSuccessful)
        assertTrue(
            "Should mention Google account",
            state.errorMessage?.contains("Google account") == true
        )
    }

    @Test
    fun `sign in failure shows generic error`() = runTest(testDispatcher) {
        resetMocks()
        mockSignInResult = Result.Error(DataError.Local.AUTH_FAILED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(signInAction())
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
        viewModel.onAction(signInAction())
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
        mockCredentialResult = Result.Error(DataError.Local.AUTH_FAILED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger error
        viewModel.onAction(signInAction())
        advanceUntilIdle()
        assertTrue("Should have error", viewModel.state.value.errorMessage != null)

        // Reset
        viewModel.onAction(SignInAction.ResetState)
        advanceUntilIdle()

        assertNull("Error should be cleared", viewModel.state.value.errorMessage)
    }

    // ============================================================================
    // ResumeSession Tests
    // ============================================================================

    @Test
    fun `auto sign-in - triggers resumeSession`() = runTest(testDispatcher) {
        resetMocks()
        mockIsSignedIn = true
        mockCurrentUser = UserData("existing-user", "Existing User", null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue("Should be signed in", viewModel.state.value.isSignInSuccessful)
        assertTrue("Should have called resumeSession", resumeSessionCallCount > 0)
    }

    @Test
    fun `not signed in - does not trigger resumeSession`() = runTest(testDispatcher) {
        resetMocks()
        mockIsSignedIn = false

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse("Should not be signed in", viewModel.state.value.isSignInSuccessful)
        assertTrue("Should not have called resumeSession", resumeSessionCallCount == 0)
    }
}
