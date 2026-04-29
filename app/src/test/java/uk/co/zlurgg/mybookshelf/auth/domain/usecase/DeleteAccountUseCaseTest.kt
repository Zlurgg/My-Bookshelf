package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.ClearUserDataUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubBookDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubCommentDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMemberDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMetadataDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubReviewDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.BookClubRemoteDataSource
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncRepository

class DeleteAccountUseCaseTest {

    // Configurable test doubles
    private var mockCurrentUserId: String? = "test-user-id"
    private var mockDeleteAccountResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var mockReauthResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var mockClearResult: Result<Int, DataError.Local> = Result.Success(5)
    private var signedInStateSet: Boolean? = null
    private var syncCancelled = false
    private var deleteAccountCalled = false
    private var reauthenticateCalled = false

    // Club tracking
    private var clubsCreatedByUser: Result<List<String>, DataError.Sync> =
        Result.Success(emptyList())
    private var clubMemberships: Result<List<String>, DataError.Sync> =
        Result.Success(emptyList())
    private var deletedClubs = mutableListOf<String>()
    private var removedFromClubs = mutableListOf<String>()
    private var clearAllMembershipsCalled = false

    private val mockSyncRepository = MockSyncRepository()

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(
            idToken: String,
        ): Result<UserData, DataError.Local> =
            Result.Success(UserData("test", "Test", null))

        override suspend fun signOut(): Result<Unit, DataError.Local> =
            Result.Success(Unit)

        override fun getSignedInUser(): UserData? = null

        override suspend fun deleteAccount(): Result<Unit, DataError.Local> {
            deleteAccountCalled = true
            return mockDeleteAccountResult
        }

        override suspend fun reauthenticate(
            idToken: String,
        ): Result<Unit, DataError.Local> {
            reauthenticateCalled = true
            return mockReauthResult
        }
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn() = Result.Success(true)
        override suspend fun setSignedInState(
            isSignedIn: Boolean,
        ): Result<Unit, DataError.Local> {
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

    private val mockClearUserData = object : ClearUserDataUseCase {
        override suspend fun invoke(
            userId: String,
        ): Result<Int, DataError.Local> = mockClearResult
    }

    private val mockCurrentUserProvider = object : CurrentUserProvider {
        override fun getCurrentUserId(): String? = mockCurrentUserId
    }

    @Suppress("TooManyFunctions")
    private val mockClubOperations = object : ClubOperations {
        override suspend fun createBookClub(
            shelfId: String,
            shelfName: String,
        ) = Result.Error(DataError.Sync.UNKNOWN)

        override suspend fun lookupBookClub(codeOrUrl: String) =
            ClubOperations.LookupResult.NotFound(DataError.Sync.CLUB_NOT_FOUND)

        override suspend fun joinBookClub() =
            Result.Error(DataError.Sync.UNKNOWN)

        override suspend fun joinBookClub(code: String) =
            Result.Error(DataError.Sync.UNKNOWN)

        override fun clearLookupState() = Unit
        override fun generateInviteLink(
            clubCode: String,
            shelfName: String,
        ) = ""

        override suspend fun syncBooksFromClub(
            clubCode: String,
            localShelfId: String,
        ) = Result.Error(DataError.Sync.UNKNOWN)

        override suspend fun leaveBookClub(shelfId: String) =
            Result.Error(DataError.Sync.UNKNOWN)

        override suspend fun validateMemberships() = emptyList<String>()

        override suspend fun deleteBookClub(
            clubCode: String,
        ): Result<Unit, DataError.Sync> {
            deletedClubs.add(clubCode)
            return Result.Success(Unit)
        }

        override suspend fun syncBookToClub(
            clubCode: String,
            book: Book,
        ) = Result.Error(DataError.Sync.UNKNOWN)

        override suspend fun removeBookFromClub(
            clubCode: String,
            bookId: String,
        ) = Result.Error(DataError.Sync.UNKNOWN)

        override suspend fun updateClubStyle(
            clubCode: String,
            styleName: String,
        ) = Result.Error(DataError.Sync.UNKNOWN)

        override suspend fun clearAllMemberships(): Result<Unit, DataError.Local> {
            clearAllMembershipsCalled = true
            return Result.Success(Unit)
        }

        override suspend fun renameBookClub(
            clubCode: String,
            newName: String,
        ): Result<Unit, DataError> = Result.Error(DataError.Sync.UNKNOWN)
    }

    private val mockBookClubRemoteDataSource =
        StubBookClubRemoteDataSource(
            onGetClubsCreatedByUser = { clubsCreatedByUser },
            onGetClubMembershipsForUser = { clubMemberships },
            onRemoveUserFromClub = { clubCode, _ ->
                removedFromClubs.add(clubCode)
                Result.Success(Unit)
            },
        )

    private lateinit var useCase: DeleteAccountUseCase

    @Before
    fun setup() {
        mockCurrentUserId = "test-user-id"
        mockDeleteAccountResult = Result.Success(Unit)
        mockReauthResult = Result.Success(Unit)
        mockClearResult = Result.Success(5)
        signedInStateSet = null
        syncCancelled = false
        deleteAccountCalled = false
        reauthenticateCalled = false
        clubsCreatedByUser = Result.Success(emptyList())
        clubMemberships = Result.Success(emptyList())
        deletedClubs = mutableListOf()
        removedFromClubs = mutableListOf()
        clearAllMembershipsCalled = false
        mockSyncRepository.reset()
        mockSyncRepository.deleteAllRemoteDataResult = Result.Success(Unit)

        useCase = DeleteAccountUseCaseImpl(
            authService = mockAuthService,
            authStateRepository = mockAuthStateRepository,
            currentUserProvider = mockCurrentUserProvider,
            syncScheduler = mockSyncScheduler,
            syncRepository = mockSyncRepository,
            clearUserData = mockClearUserData,
            clubOperations = mockClubOperations,
            bookClubRemoteDataSource = mockBookClubRemoteDataSource,
        )
    }

    // ==================== Full Success Path ====================

    @Test
    fun `invoke - full success path - returns success`() = runTest {
        val result = useCase()

        assertTrue("Should succeed", result is Result.Success)
        assertTrue("Auth should be deleted", deleteAccountCalled)
        assertTrue("Sync should be cancelled", syncCancelled)
        assertTrue("Remote data should be deleted", mockSyncRepository.deleteAllRemoteDataCalled)
        assertEquals(false, signedInStateSet)
    }

    // ==================== Firestore Failure ====================

    @Test
    fun `invoke - firestore deletion fails - auth NOT deleted`() = runTest {
        mockSyncRepository.deleteAllRemoteDataResult =
            Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Auth must NOT be deleted", deleteAccountCalled)
    }

    // ==================== Club Cleanup Failures ====================

    @Test
    fun `invoke - club query fails - remote data NOT deleted`() = runTest {
        clubsCreatedByUser = Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Remote data must NOT be deleted", mockSyncRepository.deleteAllRemoteDataCalled)
        assertFalse("Auth must NOT be deleted", deleteAccountCalled)
    }

    @Test
    fun `invoke - club delete fails - remote data NOT deleted`() = runTest {
        clubsCreatedByUser = Result.Success(listOf("club-1"))
        // Override club operations to fail on delete
        val failingClubOps = object : ClubOperations by mockClubOperations {
            override suspend fun deleteBookClub(
                clubCode: String,
            ): Result<Unit, DataError.Sync> =
                Result.Error(DataError.Sync.NETWORK_ERROR)
        }

        val failingUseCase = DeleteAccountUseCaseImpl(
            authService = mockAuthService,
            authStateRepository = mockAuthStateRepository,
            currentUserProvider = mockCurrentUserProvider,
            syncScheduler = mockSyncScheduler,
            syncRepository = mockSyncRepository,
            clearUserData = mockClearUserData,
            clubOperations = failingClubOps,
            bookClubRemoteDataSource = mockBookClubRemoteDataSource,
        )

        val result = failingUseCase()

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Remote data must NOT be deleted", mockSyncRepository.deleteAllRemoteDataCalled)
    }

    // ==================== REQUIRES_RECENT_LOGIN ====================

    @Test
    fun `invoke - auth requires recent login - returns that error`() = runTest {
        mockDeleteAccountResult = Result.Error(DataError.Local.REQUIRES_RECENT_LOGIN)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            DataError.Local.REQUIRES_RECENT_LOGIN,
            (result as Result.Error).error
        )
    }

    // ==================== retryAfterReAuth ====================

    @Test
    fun `retryAfterReAuth - success path - returns success`() = runTest {
        mockSyncRepository.hasRemoteDataResult = false

        val result = useCase.retryAfterReAuth("fresh-token")

        assertTrue("Should succeed", result is Result.Success)
        assertTrue("Should re-authenticate", reauthenticateCalled)
        assertTrue("Auth should be deleted", deleteAccountCalled)
        assertEquals(false, signedInStateSet)
    }

    @Test
    fun `retryAfterReAuth - reauth fails - returns error`() = runTest {
        mockSyncRepository.hasRemoteDataResult = false
        mockReauthResult = Result.Error(DataError.Local.AUTH_FAILED)

        val result = useCase.retryAfterReAuth("bad-token")

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Auth must NOT be deleted", deleteAccountCalled)
    }

    @Test
    fun `retryAfterReAuth - remote data still exists - falls back to invoke`() = runTest {
        mockSyncRepository.hasRemoteDataResult = true

        val result = useCase.retryAfterReAuth("token")

        // Falls back to full invoke which deletes remote data
        assertTrue("Should succeed via fallback", result is Result.Success)
        assertTrue("Remote data should be deleted", mockSyncRepository.deleteAllRemoteDataCalled)
    }

    // ==================== Not Signed In ====================

    @Test
    fun `invoke - not signed in - returns error immediately`() = runTest {
        mockCurrentUserId = null

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            DataError.Local.AUTH_FAILED,
            (result as Result.Error).error
        )
        assertFalse("Auth must NOT be deleted", deleteAccountCalled)
    }

    // ==================== Club Cleanup Behavior ====================

    @Test
    fun `invoke - deletes clubs created by user`() = runTest {
        clubsCreatedByUser = Result.Success(listOf("club-a", "club-b"))

        useCase()

        assertEquals(listOf("club-a", "club-b"), deletedClubs)
    }

    @Test
    fun `invoke - removes user from member clubs`() = runTest {
        clubMemberships = Result.Success(listOf("club-x", "club-y"))

        useCase()

        assertEquals(listOf("club-x", "club-y"), removedFromClubs)
        assertTrue("Should clear local memberships", clearAllMembershipsCalled)
    }

    // ==================== Sync Wait ====================

    @Test
    fun `invoke - sync idle - proceeds without timeout`() = runTest {
        // Default mock returns Idle state
        val result = useCase()
        assertTrue("Should succeed", result is Result.Success)
    }
}

/**
 * Stub that only requires the 3 methods used by DeleteAccountUseCase.
 * All other methods throw — they should never be called.
 */
@Suppress("TooManyFunctions")
private class StubBookClubRemoteDataSource(
    private val onGetClubsCreatedByUser: suspend (String) -> Result<List<String>, DataError.Sync>,
    private val onGetClubMembershipsForUser: suspend (String) -> Result<List<String>, DataError.Sync>,
    private val onRemoveUserFromClub: suspend (String, String) -> Result<Unit, DataError.Sync>,
) : BookClubRemoteDataSource {
    override suspend fun getClubsCreatedByUser(userId: String) = onGetClubsCreatedByUser(userId)
    override suspend fun getClubMembershipsForUser(userId: String) = onGetClubMembershipsForUser(userId)
    override suspend fun removeUserFromClub(clubCode: String, userId: String) =
        onRemoveUserFromClub(clubCode, userId)

    // Unused stubs — never called by DeleteAccountUseCase
    override suspend fun createBookClub(code: String, metadata: BookClubMetadataDto) = TODO()
    override suspend fun getBookClubMetadata(code: String) = TODO()
    override suspend fun addBookClubMember(code: String, member: BookClubMemberDto) = TODO()
    override suspend fun removeBookClubMember(code: String, userId: String) = TODO()
    override suspend fun getBookClubMembers(code: String) = TODO()
    override suspend fun isMember(code: String, userId: String) = TODO()
    override suspend fun addBookToClub(code: String, book: BookClubBookDto) = TODO()
    override suspend fun removeBookFromClub(code: String, bookId: String) = TODO()
    override suspend fun getClubBooks(code: String) = TODO()
    override suspend fun updateBookClubCounts(code: String, bookCount: Int, memberCount: Int) = TODO()
    override suspend fun updateBookClubName(code: String, name: String, lastModifiedAt: Long) = TODO()
    override suspend fun updateBookClubStyle(code: String, style: String, lastModifiedAt: Long) = TODO()
    override suspend fun deleteBookClub(code: String) = TODO()
    override suspend fun addClubMembership(userId: String, clubCode: String) = TODO()
    override suspend fun removeClubMembership(userId: String, clubCode: String) = TODO()
    override suspend fun getBookReviews(clubCode: String, bookId: String) = TODO()
    override suspend fun upsertBookReview(clubCode: String, bookId: String, review: BookClubReviewDto) = TODO()
    override suspend fun deleteBookReview(clubCode: String, bookId: String, userId: String) = TODO()
    override suspend fun getBookComments(clubCode: String, bookId: String) = TODO()
    override suspend fun addBookComment(clubCode: String, bookId: String, comment: BookClubCommentDto) = TODO()
    override suspend fun editBookComment(clubCode: String, bookId: String, commentId: String, newText: String) = TODO()
    override suspend fun deleteBookComment(clubCode: String, bookId: String, commentId: String) = TODO()
}
