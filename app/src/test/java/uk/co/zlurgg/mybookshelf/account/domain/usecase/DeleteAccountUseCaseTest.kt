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

class DeleteAccountUseCaseTest {

    private var mockCurrentUserId: String? = "test-user-id"

    // Club tracking
    private var clubsCreatedByUser: Result<List<String>, DataError.Sync> = Result.Success(emptyList())
    private var clubMemberships: Result<List<String>, DataError.Sync> = Result.Success(emptyList())
    private var deletedClubs = mutableListOf<String>()
    private var removedFromClubs = mutableListOf<String>()
    private var deleteClubResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    private var removeUserResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    private var deleteUserDocumentResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    private var deleteUserDocumentCalledWithUserId: String? = null

    private val mockAuthService = MockAuthService()
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
        override suspend fun deleteUserDocument(userId: String): Result<Unit, DataError.Sync> {
            deleteUserDocumentCalledWithUserId = userId
            return deleteUserDocumentResult
        }
    }

    private lateinit var useCase: DeleteAccountUseCase

    @Before
    fun setup() {
        mockCurrentUserId = "test-user-id"
        clubsCreatedByUser = Result.Success(emptyList())
        clubMemberships = Result.Success(emptyList())
        deletedClubs = mutableListOf()
        removedFromClubs = mutableListOf()
        deleteClubResult = Result.Success(Unit)
        removeUserResult = Result.Success(Unit)
        deleteUserDocumentResult = Result.Success(Unit)
        deleteUserDocumentCalledWithUserId = null
        mockAuthService.reset()
        mockAuthService.deleteAccountResult = Result.Success(Unit)
        mockBookcaseRepository.reset()
        authStateSignedIn = null

        useCase = DeleteAccountUseCaseImpl(
            currentUserProvider = mockCurrentUserProvider,
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
    fun `invoke - club query fails - auth NOT deleted`() = runTest {
        clubsCreatedByUser = Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Auth must NOT be deleted", mockAuthService.deleteAccountCalled)
    }

    @Test
    fun `invoke - club delete fails - auth NOT deleted`() = runTest {
        clubsCreatedByUser = Result.Success(listOf("club-1"))
        deleteClubResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
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

    // ==================== Local Data Cleanup ====================

    @Test
    fun `invoke - success - deletes club shelves`() = runTest {
        useCase()

        assertTrue("Should delete club shelves", mockBookcaseRepository.deleteClubShelvesCalled)
        assertEquals("test-user-id", mockBookcaseRepository.lastDeleteClubShelvesUserId)
    }

    @Test
    fun `invoke - success - sets auth state to false`() = runTest {
        useCase()

        assertEquals(false, authStateSignedIn)
    }

    @Test
    fun `invoke - auth fails - does NOT delete club shelves`() = runTest {
        mockAuthService.deleteAccountResult = Result.Error(DataError.Local.AUTH_FAILED)

        useCase()

        assertFalse("Should NOT delete club shelves", mockBookcaseRepository.deleteClubShelvesCalled)
    }

    @Test
    fun `invoke - REQUIRES_RECENT_LOGIN - does NOT delete club shelves`() = runTest {
        mockAuthService.deleteAccountResult = Result.Error(DataError.Local.REQUIRES_RECENT_LOGIN)

        useCase()

        assertFalse("Should NOT delete club shelves", mockBookcaseRepository.deleteClubShelvesCalled)
    }

    @Test
    fun `retryAfterReAuth - success - deletes club shelves`() = runTest {
        useCase.retryAfterReAuth("fresh-token")

        assertTrue("Should delete club shelves", mockBookcaseRepository.deleteClubShelvesCalled)
        assertEquals("test-user-id", mockBookcaseRepository.lastDeleteClubShelvesUserId)
    }

    @Test
    fun `retryAfterReAuth - success - sets auth state to false`() = runTest {
        useCase.retryAfterReAuth("fresh-token")

        assertEquals(false, authStateSignedIn)
    }

    // ==================== deleteUserDocument ====================

    @Test
    fun `invoke - deleteUserDocument fails - returns error, auth NOT deleted`() = runTest {
        deleteUserDocumentResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase()

        assertTrue("Should return error", result is Result.Error)
        assertFalse("Auth must NOT be deleted", mockAuthService.deleteAccountCalled)
        assertFalse("Should NOT delete club shelves", mockBookcaseRepository.deleteClubShelvesCalled)
    }

    @Test
    fun `retryAfterReAuth - calls deleteUserDocument before auth deletion`() = runTest {
        useCase.retryAfterReAuth("fresh-token")

        assertEquals("test-user-id", deleteUserDocumentCalledWithUserId)
        assertTrue("Auth should be deleted", mockAuthService.deleteAccountCalled)
    }

    @Test
    fun `retryAfterReAuth - deleteUserDocument fails - returns error, auth NOT deleted`() = runTest {
        deleteUserDocumentResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        val result = useCase.retryAfterReAuth("fresh-token")

        assertTrue("Should return error", result is Result.Error)
        assertTrue("Should re-authenticate", mockAuthService.reauthenticateCalled)
        assertFalse("Auth must NOT be deleted", mockAuthService.deleteAccountCalled)
    }

    @Test
    fun `invoke - retry after partial club cleanup succeeds`() = runTest {
        // First attempt: clubs exist, but deleteUserDocument fails
        clubsCreatedByUser = Result.Success(listOf("club-a"))
        deleteUserDocumentResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        val firstResult = useCase()
        assertTrue("First attempt should fail", firstResult is Result.Error)
        assertEquals(listOf("club-a"), deletedClubs)

        // Second attempt: club-a already deleted from Firestore, re-query returns empty
        deletedClubs.clear()
        clubsCreatedByUser = Result.Success(emptyList())
        deleteUserDocumentResult = Result.Success(Unit)
        mockAuthService.reset()
        mockAuthService.deleteAccountResult = Result.Success(Unit)

        val secondResult = useCase()
        assertTrue("Second attempt should succeed", secondResult is Result.Success)
        assertTrue("Empty — clubs already deleted", deletedClubs.isEmpty())
        assertTrue("Auth should be deleted", mockAuthService.deleteAccountCalled)
    }
}
