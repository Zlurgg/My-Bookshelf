package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ClearUserDataUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncRepository

class SignOutUseCaseTest {

    // Test doubles
    private var mockSignOutResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var signedInStateSet: Boolean? = null
    private var syncCancelled = false
    private var mockCurrentUserId: String? = "test-user-id"
    private var clearedUserId: String? = null
    private var mockClearResult: Result<Int, DataError.Local> = Result.Success(5)
    private val mockSyncRepository = MockSyncRepository()

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(): Result<UserData, DataError.Local> = Result.Success(
            UserData("test-user-id", "Test User", null)
        )
        override suspend fun signOut(): Result<Unit, DataError.Local> = mockSignOutResult
        override fun getSignedInUser(): UserData? = null
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Result<Boolean, DataError.Local> =
            Result.Success(true)
        override suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local> {
            signedInStateSet = isSignedIn
            return Result.Success(Unit)
        }
    }

    private val mockSyncScheduler = object : SyncSchedulerService {
        override fun schedulePeriodicSync() = Unit
        override fun triggerImmediateSync() = Unit
        override fun cancelAllSync() {
            syncCancelled = true
        }
    }

    private val mockClearUserDataUseCase = object : ClearUserDataUseCase {
        override suspend operator fun invoke(userId: String): Result<Int, DataError.Local> {
            clearedUserId = userId
            return mockClearResult
        }
    }

    private val mockCurrentUserProvider = object : CurrentUserProvider {
        override fun getCurrentUserId(): String? = mockCurrentUserId
    }

    private lateinit var useCase: SignOutUseCase

    @Before
    fun setup() {
        signedInStateSet = null
        syncCancelled = false
        clearedUserId = null
        mockSyncRepository.reset()
        mockSignOutResult = Result.Success(Unit)
        mockCurrentUserId = "test-user-id"
        mockClearResult = Result.Success(5)
        useCase = SignOutUseCaseImpl(
            mockAuthService,
            mockAuthStateRepository,
            mockSyncScheduler,
            mockClearUserDataUseCase,
            mockCurrentUserProvider,
            mockSyncRepository
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

    // ==================== Sync Cancellation Tests ====================

    @Test
    fun `execute cancels sync before signing out`() = runTest {
        useCase()

        assertTrue("Sync should be cancelled", syncCancelled)
    }

    @Test
    fun `execute cancels sync even when sign out fails`() = runTest {
        mockSignOutResult = Result.Error(DataError.Local.AUTH_FAILED)

        useCase()

        assertTrue("Sync should be cancelled even on sign-out failure", syncCancelled)
    }

    // ==================== Data Clearing Tests ====================

    @Test
    fun `execute clears user data before signing out`() = runTest {
        useCase()

        assertEquals("test-user-id", clearedUserId)
    }

    @Test
    fun `execute skips data clearing when no user is signed in`() = runTest {
        mockCurrentUserId = null

        useCase()

        assertNull("Should not clear data when no user signed in", clearedUserId)
    }

    @Test
    fun `execute succeeds even when clearing data fails`() = runTest {
        mockClearResult = Result.Error(DataError.Local.DATABASE_ERROR)

        val result = useCase()

        assertTrue("Sign-out should succeed even if data clearing fails", result is Result.Success)
        assertEquals("test-user-id", clearedUserId) // Still attempted clearing
    }

    @Test
    fun `execute clears data for correct user id`() = runTest {
        mockCurrentUserId = "specific-user-123"

        useCase()

        assertEquals("specific-user-123", clearedUserId)
    }

    // ==================== Sync Metadata Clearing Tests ====================

    @Test
    fun `execute clears sync metadata for user`() = runTest {
        useCase()

        assertEquals("test-user-id", mockSyncRepository.clearedSyncDataForUserId)
    }

    @Test
    fun `execute skips sync metadata clearing when no user is signed in`() = runTest {
        mockCurrentUserId = null

        useCase()

        assertNull("Should not clear sync metadata when no user signed in", mockSyncRepository.clearedSyncDataForUserId)
    }

    @Test
    fun `execute clears sync metadata for correct user id`() = runTest {
        mockCurrentUserId = "specific-user-456"

        useCase()

        assertEquals("specific-user-456", mockSyncRepository.clearedSyncDataForUserId)
    }
}
