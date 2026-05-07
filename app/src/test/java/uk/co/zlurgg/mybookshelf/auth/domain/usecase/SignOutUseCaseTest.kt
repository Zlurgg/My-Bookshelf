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
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

class SignOutUseCaseTest {

    // Test doubles
    private var mockSignOutResult: Result<Unit, DataError.Local> = Result.Success(Unit)
    private var signedInStateSet: Boolean? = null
    private var mockCurrentUserId: String? = "test-user-id"
    private var clearAllMembershipsCalled = false
    private val mockBookcaseRepository = MockBookcaseRepository()

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(idToken: String): Result<UserData, DataError.Local> = Result.Success(
            UserData("test-user-id", "Test User", null)
        )
        override suspend fun signOut(): Result<Unit, DataError.Local> = mockSignOutResult
        override fun getSignedInUser(): UserData? = null
        override suspend fun deleteAccount(): Result<Unit, DataError.Local> = Result.Error(DataError.Local.AUTH_FAILED)
        override suspend fun reauthenticate(
            idToken: String,
        ): Result<Unit, DataError.Local> = Result.Error(DataError.Local.AUTH_FAILED)
    }

    private val mockAuthStateRepository = object : AuthStateRepository {
        override suspend fun isSignedIn(): Result<Boolean, DataError.Local> =
            Result.Success(true)
        override suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local> {
            signedInStateSet = isSignedIn
            return Result.Success(Unit)
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
        override suspend fun deleteBookClub(clubCode: String) = Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun syncBookToClub(clubCode: String, book: Book) =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun removeBookFromClub(clubCode: String, bookId: String) =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun updateClubStyle(clubCode: String, styleName: String) =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun clearAllMemberships(): Result<Unit, DataError.Local> {
            clearAllMembershipsCalled = true
            return Result.Success(Unit)
        }
        override suspend fun renameBookClub(clubCode: String, newName: String): Result<Unit, DataError> =
            Result.Error(DataError.Sync.UNKNOWN)
        override suspend fun getClubsCreatedByUser(userId: String) = Result.Success(emptyList<String>())
        override suspend fun getClubMembershipsForUser(userId: String) = Result.Success(emptyList<String>())
        override suspend fun removeUserFromClub(clubCode: String, userId: String) =
            Result.Error(DataError.Sync.UNKNOWN)
    }

    private val mockCurrentUserProvider = object : CurrentUserProvider {
        override fun getCurrentUserId(): String? = mockCurrentUserId
    }

    private lateinit var useCase: SignOutUseCase

    @Before
    fun setup() {
        signedInStateSet = null
        clearAllMembershipsCalled = false
        mockBookcaseRepository.reset()
        mockSignOutResult = Result.Success(Unit)
        mockCurrentUserId = "test-user-id"
        useCase = SignOutUseCaseImpl(
            mockAuthService,
            mockAuthStateRepository,
            mockCurrentUserProvider,
            mockClubOperations,
            mockBookcaseRepository,
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

    // ==================== Club Cleanup Tests ====================

    @Test
    fun `execute clears all memberships on success`() = runTest {
        useCase()

        assertTrue("Memberships should be cleared", clearAllMembershipsCalled)
    }

    @Test
    fun `execute deletes club shelves for user on success`() = runTest {
        useCase()

        assertTrue("Club shelves should be deleted", mockBookcaseRepository.deleteClubShelvesCalled)
        assertEquals("test-user-id", mockBookcaseRepository.lastDeleteClubShelvesUserId)
    }

    @Test
    fun `execute skips club shelf cleanup when no user is signed in`() = runTest {
        mockCurrentUserId = null

        useCase()

        assertNull("Should not delete club shelves when no user signed in", mockBookcaseRepository.lastDeleteClubShelvesUserId)
    }

    @Test
    fun `execute deletes club shelves for correct user id`() = runTest {
        mockCurrentUserId = "specific-user-456"

        useCase()

        assertEquals("specific-user-456", mockBookcaseRepository.lastDeleteClubShelvesUserId)
    }
}
