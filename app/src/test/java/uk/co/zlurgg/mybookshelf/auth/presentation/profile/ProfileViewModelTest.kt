package uk.co.zlurgg.mybookshelf.auth.presentation.profile

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
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.DeleteAccountUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetCurrentUserIdUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetSignedInUserUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

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
        override suspend fun invoke(
            idToken: String,
        ): Result<UserData, DataError.Local> =
            Result.Error(DataError.Local.AUTH_FAILED)
    }

    private val mockSignOut = object : SignOutUseCase {
        override suspend fun invoke(): Result<Unit, DataError.Local> =
            mockSignOutResult
    }

    private val mockCheckSignIn = object : CheckSignInStatusUseCase {
        override suspend fun invoke(): Boolean = true
    }

    private val mockGetCurrentUserId = object : GetCurrentUserIdUseCase {
        override fun invoke(): String? = "user-1"
    }

    private val mockDeleteAccount = object : DeleteAccountUseCase {
        override suspend fun invoke(): Result<Unit, DataError> =
            mockDeleteResult

        override suspend fun retryAfterReAuth(
            idToken: String,
        ): Result<Unit, DataError> = mockRetryResult
    }

    private val mockGetSignedInUser = object : GetSignedInUserUseCase {
        override fun invoke(): UserData? = mockSignedInUser
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            authUseCases = AuthUseCases(
                signIn = mockSignIn,
                signOut = mockSignOut,
                checkSignInStatus = mockCheckSignIn,
                getCurrentUserId = mockGetCurrentUserId,
                deleteAccount = mockDeleteAccount,
                getSignedInUser = mockGetSignedInUser,
            ),
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
    fun `init - no user signed in - state remains empty`() = runTest(testDispatcher) {
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
            viewModel.onAction(ProfileAction.ConfirmSignOut)
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
            viewModel.onAction(ProfileAction.ConfirmSignOut)
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
            viewModel.onAction(ProfileAction.ConfirmDeleteAccount)
        }

        assertTrue("Should navigate to sign in", state?.navigateToSignIn == true)
        assertFalse("Should not be deleting", state?.isDeleting == true)
        helper.cleanup()
    }

    @Test
    fun `delete - requires recent login - shows reauth dialog`() = runTest(testDispatcher) {
        mockDeleteResult = Result.Error(DataError.Local.REQUIRES_RECENT_LOGIN)
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(ProfileAction.ConfirmDeleteAccount)
        }

        assertTrue("Should show re-auth dialog", state?.showReAuthDialog == true)
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
            viewModel.onAction(ProfileAction.ConfirmDeleteAccount)
        }

        assertTrue("Should have error", state?.errorMessage != null)
        assertFalse("Should not be deleting", state?.isDeleting == true)
        helper.cleanup()
    }

    // ==================== Re-auth Retry ====================

    @Test
    fun `reauth retry - success - navigates to sign in`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        val state = helper.executeAndGetState {
            viewModel.onAction(ProfileAction.OnReAuthCompleted("fresh-token"))
        }

        assertTrue("Should navigate", state?.navigateToSignIn == true)
        assertFalse("Should not be deleting", state?.isDeleting == true)
        helper.cleanup()
    }

    // ==================== Error Dismissal ====================

    @Test
    fun `dismiss error - clears errorMessage`() = runTest(testDispatcher) {
        mockDeleteResult = Result.Error(DataError.Sync.NETWORK_ERROR)
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        // Trigger error first
        helper.executeAndGetState {
            viewModel.onAction(ProfileAction.ConfirmDeleteAccount)
        }

        val state = helper.executeAndGetState {
            viewModel.onAction(ProfileAction.DismissError)
        }

        assertNull("Error should be cleared", state?.errorMessage)
        helper.cleanup()
    }

    // ==================== ResetNavigation ====================

    @Test
    fun `reset navigation - clears navigateToSignIn`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val helper = viewModel.state.testHelper(this)

        // Trigger navigation first
        helper.executeAndGetState {
            viewModel.onAction(ProfileAction.ConfirmSignOut)
        }

        val state = helper.executeAndGetState {
            viewModel.onAction(ProfileAction.ResetNavigation)
        }

        assertFalse("Should be cleared", state?.navigateToSignIn == true)
        helper.cleanup()
    }
}
