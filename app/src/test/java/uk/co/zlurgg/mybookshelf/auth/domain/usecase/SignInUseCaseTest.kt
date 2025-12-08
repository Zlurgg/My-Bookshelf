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
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class SignInUseCaseTest {

    // Test doubles
    private var mockSignInResult: Result<UserData, DataError.Local> = Result.Success(
        UserData("test-user-id", "Test User", null)
    )
    private var signedInStateSet: Boolean? = null
    private var periodicSyncScheduled = false
    private var immediateSyncTriggered = false

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(): Result<UserData, DataError.Local> = mockSignInResult
        override suspend fun signOut(): Result<Unit, DataError.Local> = Result.Success(Unit)
        override fun getSignedInUser(): UserData? = null
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Boolean = false
        override suspend fun setSignedInState(isSignedIn: Boolean) {
            signedInStateSet = isSignedIn
        }
    }

    private val mockSyncScheduler = object : SyncSchedulerService {
        override fun schedulePeriodicSync() {
            periodicSyncScheduled = true
        }
        override fun triggerImmediateSync() {
            immediateSyncTriggered = true
        }
        override fun cancelAllSync() {}
    }

    private lateinit var useCase: SignInUseCase

    @Before
    fun setup() {
        signedInStateSet = null
        periodicSyncScheduled = false
        immediateSyncTriggered = false
        mockSignInResult = Result.Success(UserData("test-user-id", "Test User", null))
        useCase = SignInUseCase(mockAuthService, mockAuthStateRepository, mockSyncScheduler)
    }

    @Test
    fun `execute returns success and saves signed in state when sign in succeeds`() = runTest {
        val result = useCase.execute()

        assertTrue("Should be success", result is Result.Success)
        assertEquals("test-user-id", (result as Result.Success).data.userId)
        assertEquals(true, signedInStateSet)
    }

    @Test
    fun `execute returns user data on success`() = runTest {
        val expectedUser = UserData("user-123", "John Doe", "https://example.com/photo.jpg")
        mockSignInResult = Result.Success(expectedUser)

        val result = useCase.execute()

        assertTrue("Should be success", result is Result.Success)
        val userData = (result as Result.Success).data
        assertEquals("user-123", userData.userId)
        assertEquals("John Doe", userData.username)
        assertEquals("https://example.com/photo.jpg", userData.profilePictureUrl)
    }

    @Test
    fun `execute returns error and does not save state when sign in fails`() = runTest {
        mockSignInResult = Result.Error(DataError.Local.AUTH_FAILED)

        val result = useCase.execute()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_FAILED, (result as Result.Error).error)
        assertEquals(null, signedInStateSet) // State should not be set on failure
    }

    @Test
    fun `execute returns cancelled error when user cancels`() = runTest {
        mockSignInResult = Result.Error(DataError.Local.AUTH_CANCELLED)

        val result = useCase.execute()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_CANCELLED, (result as Result.Error).error)
    }

    @Test
    fun `execute returns no credential error when no account available`() = runTest {
        mockSignInResult = Result.Error(DataError.Local.AUTH_NO_CREDENTIAL)

        val result = useCase.execute()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_NO_CREDENTIAL, (result as Result.Error).error)
    }

    @Test
    fun `execute returns network error on network failure`() = runTest {
        mockSignInResult = Result.Error(DataError.Local.AUTH_NETWORK_ERROR)

        val result = useCase.execute()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_NETWORK_ERROR, (result as Result.Error).error)
    }

    // ==================== Sync Scheduling Tests ====================

    @Test
    fun `execute schedules periodic sync on successful sign in`() = runTest {
        useCase.execute()

        assertTrue("Periodic sync should be scheduled", periodicSyncScheduled)
    }

    @Test
    fun `execute triggers immediate sync on successful sign in`() = runTest {
        useCase.execute()

        assertTrue("Immediate sync should be triggered", immediateSyncTriggered)
    }

    @Test
    fun `execute does not schedule sync when sign in fails`() = runTest {
        mockSignInResult = Result.Error(DataError.Local.AUTH_FAILED)

        useCase.execute()

        assertEquals(false, periodicSyncScheduled)
        assertEquals(false, immediateSyncTriggered)
    }

    // Note: Migration tests removed - migration is now handled separately
    // by the ViewModel via MigrateLocalDataUseCase after showing a dialog
}
