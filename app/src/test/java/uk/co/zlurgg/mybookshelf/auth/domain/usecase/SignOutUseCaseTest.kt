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

class SignOutUseCaseTest {

    // Test doubles
    private var mockSignOutResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var signedInStateSet: Boolean? = null
    private var syncCancelled = false

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(): Result<UserData, DataError.Local> = Result.Success(
            UserData("test-user-id", "Test User", null)
        )
        override suspend fun signOut(): Result<Unit, DataError.Local> = mockSignOutResult
        override fun getSignedInUser(): UserData? = null
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Boolean = true
        override suspend fun setSignedInState(isSignedIn: Boolean) {
            signedInStateSet = isSignedIn
        }
    }

    private val mockSyncScheduler = object : SyncSchedulerService {
        override fun schedulePeriodicSync() {}
        override fun triggerImmediateSync() {}
        override fun cancelAllSync() {
            syncCancelled = true
        }
    }

    private lateinit var useCase: SignOutUseCase

    @Before
    fun setup() {
        signedInStateSet = null
        syncCancelled = false
        mockSignOutResult = Result.Success(Unit)
        useCase = SignOutUseCase(mockAuthService, mockAuthStateRepository, mockSyncScheduler)
    }

    @Test
    fun `execute returns success and clears signed in state when sign out succeeds`() = runTest {
        val result = useCase.execute()

        assertTrue("Should be success", result is Result.Success)
        assertEquals(false, signedInStateSet)
    }

    @Test
    fun `execute returns error and does not modify state when sign out fails`() = runTest {
        mockSignOutResult = Result.Error(DataError.Local.AUTH_FAILED)

        val result = useCase.execute()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_FAILED, (result as Result.Error).error)
        assertEquals(null, signedInStateSet) // State should not be modified on failure
    }

    @Test
    fun `execute returns success with Unit data`() = runTest {
        val result = useCase.execute()

        assertTrue("Should be success", result is Result.Success)
        assertEquals(Unit, (result as Result.Success).data)
    }

    @Test
    fun `execute sets signed in state to false on success`() = runTest {
        useCase.execute()

        assertEquals(false, signedInStateSet)
    }

    @Test
    fun `execute preserves error type from auth service`() = runTest {
        mockSignOutResult = Result.Error(DataError.Local.AUTH_NETWORK_ERROR)

        val result = useCase.execute()

        assertTrue("Should be error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_NETWORK_ERROR, (result as Result.Error).error)
    }

    // ==================== Sync Cancellation Tests ====================

    @Test
    fun `execute cancels sync before signing out`() = runTest {
        useCase.execute()

        assertTrue("Sync should be cancelled", syncCancelled)
    }

    @Test
    fun `execute cancels sync even when sign out fails`() = runTest {
        mockSignOutResult = Result.Error(DataError.Local.AUTH_FAILED)

        useCase.execute()

        assertTrue("Sync should be cancelled even on sign-out failure", syncCancelled)
    }
}
