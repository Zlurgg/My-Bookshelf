package uk.co.zlurgg.mybookshelf.account.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockAuthService
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncRepository

class DeleteAccountUseCaseTest {

    private var mockCurrentUserId: String? = "test-user-id"
    private var syncCancelled = false

    // Club tracking
    private var clubsCreatedByUser: Result<List<String>, DataError.Sync> = Result.Success(emptyList())
    private var clubMemberships: Result<List<String>, DataError.Sync> = Result.Success(emptyList())
    private var deletedClubs = mutableListOf<String>()
    private var removedFromClubs = mutableListOf<String>()
    private var deleteClubResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    private var removeUserResult: Result<Unit, DataError.Sync> = Result.Success(Unit)

    private val mockAuthService = MockAuthService()
    private val mockSyncRepository = MockSyncRepository()
    private val mockBookcaseRepository = MockBookcaseRepository()

    // Auth state tracking
    private var authStateSignedIn: Boolean? = null
    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Result<Boolean, DataError.Local> =
            Result.Success(true)
        override suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local> {
            authStateSignedIn = isSignedIn
            return Result.Success(Unit)
        }
    }

    private val mockCurrentUserProvider = object : CurrentUserProvider {
        override fun getCurrentUserId(): String? = mockCurrentUserId
    }

    private val mockSyncScheduler = object : uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService {
        override fun schedulePeriodicSync() = Unit
        override fun triggerImmediateSync() = Unit
        override fun cancelAllSync() {
            syncCancelled = true
        }
    }

    @Suppress("TooManyFunctions")
    private val mockClubOperations = object : ClubOperations {
        override suspend fun createBookClub(name: String, shelfStyle: String, sourceShelfId: String?) =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun lookupBookClub(codeOrUrl: String) =
            ClubOperations.LookupResult.NotFound(DataError.Sync.CLUB_NOT_FOUND)
        override suspend fun joinBookClub() = Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun joinBookClub(code: String) = Result.Error(DataError.Sync.UNKNOWN)
        override fun clearLookupState() = Unit
        override suspend fun syncBooksFromClub(clubCode: String, localShelfId: String) =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun leaveBookClub(shelfId: String) = Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun validateMemberships() = emptyList<String>()
        override suspend fun deleteBookClub(clubCode: String): Result<Unit, DataError.Sync> {
            deletedClubs.add(clubCode)
            return deleteClubResult
        }
        override suspend fun syncBookToClub(clubCode: String, book: Book) =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun removeBookFromClub(clubCode: String, bookId: String) =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun updateClubStyle(clubCode: String, styleName: String) =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun clearAllMemberships() = Result.Success(Unit)
        override suspend fun renameBookClub(clubCode: String, newName: String): Result<Unit, DataError> =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun getClubsCreatedByUser(userId: String) = clubsCreatedByUser
        override suspend fun getClubMembershipsForUser(userId: String) = clubMemberships
        override suspend fun removeUserFromClub(clubCode: String, userId: String): Result<Unit, DataError.Sync> {
            removedFromClubs.add(clubCode)
            return removeUserResult
        }
    }

    private lateinit var useCase: DeleteAccountUseCase

    @Before
    fun setup() {
        mockCurrentUserId = "test-user-id"
        syncCancelled = false
        clubsCreatedByUser = Result.Success(emptyList())
        clubMemberships = Result.Success(emptyList())
        deletedClubs = mutableListOf()
        removedFromClubs = mutableListOf()
        deleteClubResult = Result.Success(Unit)
        removeUserResult = Result.Success(Unit)
        mockAuthService.reset()
        mockAuthService.deleteAccountResult = Result.Success(Unit)
        mockSyncRepository.reset()
        mockSyncRepository.deleteAllRemoteDataResult = Result.Success(Unit)
        mockBookcaseRepository.reset()
        authStateSignedIn = null

        useCase = DeleteAccountUseCaseImpl(
            currentUserProvider = mockCurrentUserProvider,
            syncScheduler = mockSyncScheduler,
            syncRepository = mockSyncRepository,
            clubOperations = mockClubOperations,
            authService = mockAuthService,
            bookcaseRepository = mockBookcaseRepository,
            authStateRepository = mockAuthStateRepository,
        )
    }

    // ==================== Full Success Path ====================

    @Test
    fun `invoke - full success - returns success`() = runTest {
        val result = useCase()

        assertTrue("Should succeed", result is Result.Success)
        assertTrue("Auth should be deleted", mockAuthService.deleteAccountCalled)
        assertTrue("Sync should be cancelled", syncCancelled)
        assertTrue("Remote data should be deleted", mockSyncRepository.deleteAllRemoteDataCalled)
    }

    // ==================== Not Signed In ====================

    @Test
    fun `invoke - not signed in - returns AUTH_FAILED immediately`() = runTest {
        mockCurrentUserId = null

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Local.AUTH_FAILED, (result as Result.Error).error)
        assertFalse("Auth must NOT be deleted", mockAuthService.deleteAccountCalled)
    }

    // ==================== Club Query Failures ====================

    @Test
    fun `invoke - club query fails - remote data NOT deleted`() = runTest {
        clubsCreatedByUser = Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Remote data must NOT be deleted", mockSyncRepository.deleteAllRemoteDataCalled)
        assertFalse("Auth must NOT be deleted", mockAuthService.deleteAccountCalled)
    }

    @Test
    fun `invoke - club delete fails - remote data NOT deleted`() = runTest {
        clubsCreatedByUser = Result.Success(listOf("club-1"))
        deleteClubResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Remote data must NOT be deleted", mockSyncRepository.deleteAllRemoteDataCalled)
    }

    // ==================== Firestore Failure ====================

    @Test
    fun `invoke - firestore deletion fails - auth NOT deleted`() = runTest {
        mockSyncRepository.deleteAllRemoteDataResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Auth must NOT be deleted", mockAuthService.deleteAccountCalled)
    }

    // ==================== REQUIRES_RECENT_LOGIN ====================

    @Test
    fun `invoke - auth requires recent login - returns REQUIRES_RECENT_LOGIN`() = runTest {
        mockAuthService.deleteAccountResult = Result.Error(DataError.Local.REQUIRES_RECENT_LOGIN)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Local.REQUIRES_RECENT_LOGIN, (result as Result.Error).error)
    }

    // ==================== Club Cleanup Behavior ====================

    @Test
    fun `invoke - deletes clubs created by user`() = runTest {
        clubsCreatedByUser = Result.Success(listOf("club-a", "club-b"))

        useCase()

        assertEquals(listOf("club-a", "club-b"), deletedClubs)
    }

    @Test
    fun `invoke - removes user from member clubs excluding created clubs`() = runTest {
        clubsCreatedByUser = Result.Success(listOf("club-a"))
        clubMemberships = Result.Success(listOf("club-a", "club-x", "club-y"))

        useCase()

        assertEquals("Created clubs deleted", listOf("club-a"), deletedClubs)
        assertEquals("Only non-created memberships removed", listOf("club-x", "club-y"), removedFromClubs)
    }

    // ==================== retryAfterReAuth ====================

    @Test
    fun `retryAfterReAuth - success - returns success`() = runTest {
        val result = useCase.retryAfterReAuth("fresh-token")

        assertTrue("Should succeed", result is Result.Success)
        assertTrue("Should re-authenticate", mockAuthService.reauthenticateCalled)
        assertTrue("Auth should be deleted", mockAuthService.deleteAccountCalled)
    }

    @Test
    fun `retryAfterReAuth - reauth fails - returns error, auth NOT deleted`() = runTest {
        mockAuthService.reauthenticateResult = Result.Error(DataError.Local.AUTH_FAILED)

        val result = useCase.retryAfterReAuth("bad-token")

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Auth must NOT be deleted", mockAuthService.deleteAccountCalled)
    }

    // ==================== Local Data Revert ====================

    @Test
    fun `invoke - success - reverts local data to guest`() = runTest {
        useCase()

        assertTrue("Should revert local data", mockBookcaseRepository.revertUserDataToGuestCalled)
        assertEquals("test-user-id", mockBookcaseRepository.lastRevertedUserId)
    }

    @Test
    fun `invoke - success - sets auth state to false`() = runTest {
        useCase()

        assertEquals(false, authStateSignedIn)
    }

    @Test
    fun `invoke - success - clears sync data`() = runTest {
        useCase()

        assertEquals("test-user-id", mockSyncRepository.clearedSyncDataForUserId)
    }

    @Test
    fun `invoke - auth fails - does NOT revert local data`() = runTest {
        mockAuthService.deleteAccountResult = Result.Error(DataError.Local.AUTH_FAILED)

        useCase()

        assertFalse("Should NOT revert local data", mockBookcaseRepository.revertUserDataToGuestCalled)
    }

    @Test
    fun `invoke - REQUIRES_RECENT_LOGIN - does NOT revert local data`() = runTest {
        mockAuthService.deleteAccountResult = Result.Error(DataError.Local.REQUIRES_RECENT_LOGIN)

        useCase()

        assertFalse("Should NOT revert local data", mockBookcaseRepository.revertUserDataToGuestCalled)
    }

    @Test
    fun `retryAfterReAuth - success - reverts local data to guest`() = runTest {
        useCase.retryAfterReAuth("fresh-token")

        assertTrue("Should revert local data", mockBookcaseRepository.revertUserDataToGuestCalled)
        assertEquals("test-user-id", mockBookcaseRepository.lastRevertedUserId)
    }

    @Test
    fun `retryAfterReAuth - success - sets auth state to false`() = runTest {
        useCase.retryAfterReAuth("fresh-token")

        assertEquals(false, authStateSignedIn)
    }

    @Test
    fun `invoke - revert fails - still clears sync data and auth state`() = runTest {
        mockBookcaseRepository.revertErrorToReturn = DataError.Local.UNKNOWN

        useCase()

        assertTrue("Revert should be attempted", mockBookcaseRepository.revertUserDataToGuestCalled)
        assertEquals("Sync data should still be cleared", "test-user-id", mockSyncRepository.clearedSyncDataForUserId)
        assertEquals("Auth state should still be set", false, authStateSignedIn)
    }
}
