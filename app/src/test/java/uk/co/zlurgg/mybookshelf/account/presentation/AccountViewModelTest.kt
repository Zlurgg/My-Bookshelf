package uk.co.zlurgg.mybookshelf.account.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.account.domain.usecase.DeleteAccountUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetCurrentUserIdUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetSignedInUserUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Configurable results
    private var mockSignOutResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var mockDeleteResult: Result<Unit, DataError> = Result.Success(Unit)
    private var mockRetryResult: Result<Unit, DataError> = Result.Success(Unit)
    private var mockSignedInUser: UserData? = UserData(
        userId = "user-1",
        username = "Test User",
        profilePictureUrl = "https://photo.url",
        email = "test@example.com",
    )

    private val mockSignIn = object : SignInUseCase {
        override suspend fun invoke(idToken: String): Result<UserData, DataError.Local> =
            Result.Error(DataError.Local.AUTH_FAILED)
    }

    private val mockSignOut = object : SignOutUseCase {
        override suspend fun invoke(): Result<Unit, DataError.Local> = mockSignOutResult
    }

    private val mockCheckSignIn = object : CheckSignInStatusUseCase {
        override suspend fun invoke(): Boolean = true
    }

    private val mockGetCurrentUserId = object : GetCurrentUserIdUseCase {
        override fun invoke(): String? = "user-1"
    }

    private val mockGetSignedInUser = object : GetSignedInUserUseCase {
        override fun invoke(): UserData? = mockSignedInUser
    }

    private val mockDeleteAccount = object : DeleteAccountUseCase {
        override suspend fun invoke(): Result<Unit, DataError> = mockDeleteResult
        override suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError> = mockRetryResult
    }

    private fun createViewModel(): AccountViewModel {
        return AccountViewModel(
            authUseCases = AuthUseCases(
                signIn = mockSignIn,
                signOut = mockSignOut,
                checkSignInStatus = mockCheckSignIn,
                getCurrentUserId = mockGetCurrentUserId,
                getSignedInUser = mockGetSignedInUser,
            ),
            deleteAccountUseCase = mockDeleteAccount,
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSignOutResult = Result.Success(Unit)
        mockDeleteResult = Result.Success(Unit)
        mockRetryResult = Result.Success(Unit)
        mockSignedInUser = UserData(
            userId = "user-1",
            username = "Test User",
            profilePictureUrl = "https://photo.url",
            email = "test@example.com",
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Init ====================

    @Test
    fun `init - loads user data into state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.getCurrentState()

        assertEquals("Test User", state?.userName)
        assertEquals("test@example.com", state?.userEmail)
        assertEquals("https://photo.url", state?.profilePictureUrl)
        assertTrue("Should be signed in", state?.isSignedIn == true)
        helper.cleanup()
    }

    @Test
    fun `init - no user - state remains empty`() = runTest(testDispatcher) {
        mockSignedInUser = null
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.getCurrentState()

        assertNull(state?.userName)
        assertFalse("Should not be signed in", state?.isSignedIn == true)
        helper.cleanup()
    }

    // ==================== Sign Out ====================

    @Test
    fun `sign out - success - sets navigateToSignIn`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.ConfirmSignOut)
        }

        assertTrue("Should navigate to sign in", state?.navigateToSignIn == true)
        helper.cleanup()
    }

    @Test
    fun `sign out - failure - sets errorMessage`() = runTest(testDispatcher) {
        mockSignOutResult = Result.Error(DataError.Local.AUTH_FAILED)
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.ConfirmSignOut)
        }

        assertTrue("Should have error", state?.errorMessage != null)
        assertFalse("Should NOT navigate", state?.navigateToSignIn == true)
        helper.cleanup()
    }

    // ==================== Delete Account ====================

    @Test
    fun `delete - success - sets navigateToSignIn`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.ConfirmDeleteAccount)
        }

        assertTrue("Should navigate to sign in", state?.navigateToSignIn == true)
        assertFalse("Should not be deleting", state?.isDeleting == true)
        helper.cleanup()
    }

    @Test
    fun `delete - requires recent login - sets requestReAuth`() = runTest(testDispatcher) {
        mockDeleteResult = Result.Error(DataError.Local.REQUIRES_RECENT_LOGIN)
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.ConfirmDeleteAccount)
        }

        assertTrue("Should request re-auth", state?.requestReAuth == true)
        assertFalse("Should not be deleting", state?.isDeleting == true)
        assertFalse("Should NOT navigate", state?.navigateToSignIn == true)
        helper.cleanup()
    }

    @Test
    fun `delete - other failure - sets errorMessage`() = runTest(testDispatcher) {
        mockDeleteResult = Result.Error(DataError.Sync.NETWORK_ERROR)
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.ConfirmDeleteAccount)
        }

        assertTrue("Should have error", state?.errorMessage != null)
        assertFalse("Should not be deleting", state?.isDeleting == true)
        helper.cleanup()
    }

    @Test
    fun `delete - while already deleting - no-op`() = runTest(testDispatcher) {
        // Make delete hang by never completing
        var deleteCallCount = 0
        val hangingDeleteAccount = object : DeleteAccountUseCase {
            override suspend fun invoke(): Result<Unit, DataError> {
                deleteCallCount++
                if (deleteCallCount == 1) {
                    // First call: simulate a slow operation
                    kotlinx.coroutines.delay(Long.MAX_VALUE)
                }
                return mockDeleteResult
            }
            override suspend fun retryAfterReAuth(idToken: String) = mockRetryResult
        }

        val viewModel = AccountViewModel(
            authUseCases = AuthUseCases(
                signIn = mockSignIn,
                signOut = mockSignOut,
                checkSignInStatus = mockCheckSignIn,
                getCurrentUserId = mockGetCurrentUserId,
                getSignedInUser = mockGetSignedInUser,
            ),
            deleteAccountUseCase = hangingDeleteAccount,
        )
        val helper = viewModel.state.testHelper(this)

        // Start first delete
        viewModel.onAction(AccountAction.ConfirmDeleteAccount)
        testScheduler.advanceTimeBy(100)

        // Try second delete while first is still running
        viewModel.onAction(AccountAction.ConfirmDeleteAccount)
        testScheduler.advanceTimeBy(100)

        assertEquals("Delete should only be called once", 1, deleteCallCount)
        helper.cleanup()
    }

    // ==================== Re-auth Retry ====================

    @Test
    fun `reauth retry - success - navigates to sign in`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.OnReAuthCompleted("fresh-token"))
        }

        assertTrue("Should navigate", state?.navigateToSignIn == true)
        assertFalse("Should not be deleting", state?.isDeleting == true)
        helper.cleanup()
    }

    @Test
    fun `reauth retry - failure - sets errorMessage`() = runTest(testDispatcher) {
        mockRetryResult = Result.Error(DataError.Local.AUTH_FAILED)
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.OnReAuthCompleted("bad-token"))
        }

        assertTrue("Should have error", state?.errorMessage != null)
        assertFalse("Should not be deleting", state?.isDeleting == true)
        helper.cleanup()
    }

    @Test
    fun `OnReAuthFailed - sets errorMessage`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.OnReAuthFailed)
        }

        assertTrue("Should have error", state?.errorMessage != null)
        helper.cleanup()
    }

    // ==================== State Reset ====================

    @Test
    fun `dismiss error - clears errorMessage`() = runTest(testDispatcher) {
        mockDeleteResult = Result.Error(DataError.Sync.NETWORK_ERROR)
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        helper.executeAndGetState {
            viewModel.onAction(AccountAction.ConfirmDeleteAccount)
        }
        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.DismissError)
        }

        assertNull("Error should be cleared", state?.errorMessage)
        helper.cleanup()
    }

    @Test
    fun `reset navigation - clears navigateToSignIn`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        helper.executeAndGetState {
            viewModel.onAction(AccountAction.ConfirmSignOut)
        }
        val state = helper.executeAndGetState {
            viewModel.onAction(AccountAction.ResetNavigation)
        }

        assertFalse("Should be cleared", state?.navigateToSignIn == true)
        helper.cleanup()
    }
}
