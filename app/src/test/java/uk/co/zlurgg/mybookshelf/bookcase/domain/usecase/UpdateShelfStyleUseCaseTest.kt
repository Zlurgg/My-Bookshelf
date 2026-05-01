package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

class UpdateShelfStyleUseCaseTest {

    private val mockRepository = MockBookcaseRepository()

    private val mockAuthService = object : AuthService {
        override suspend fun signIn(idToken: String): Result<UserData, DataError.Local> =
            Result.Error(DataError.Local.AUTH_FAILED)
        override suspend fun signOut(): Result<Unit, DataError.Local> = Result.Success(Unit)
        override fun getSignedInUser(): UserData? = UserData("creator-id", "Creator", null)
        override suspend fun deleteAccount(): Result<Unit, DataError.Local> =
            Result.Error(DataError.Local.AUTH_FAILED)
        override suspend fun reauthenticate(idToken: String): Result<Unit, DataError.Local> =
            Result.Error(DataError.Local.AUTH_FAILED)
    }

    private val mockClubOperations = object : ClubOperations {
        override suspend fun createBookClub(shelfId: String, shelfName: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun lookupBookClub(codeOrUrl: String) =
            ClubOperations.LookupResult.NotFound(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun joinBookClub() =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun joinBookClub(code: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override fun clearLookupState() = Unit
        override fun generateInviteLink(clubCode: String, shelfName: String) = ""
        override suspend fun syncBooksFromClub(clubCode: String, localShelfId: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun leaveBookClub(shelfId: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun validateMemberships() = emptyList<String>()
        override suspend fun deleteBookClub(clubCode: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun syncBookToClub(clubCode: String, book: Book) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun removeBookFromClub(clubCode: String, bookId: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun updateClubStyle(clubCode: String, styleName: String) =
            Result.Success(Unit)
        override suspend fun clearAllMemberships() =
            Result.Error(DataError.Local.UNKNOWN)
        override suspend fun renameBookClub(clubCode: String, newName: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun getClubsCreatedByUser(userId: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun getClubMembershipsForUser(userId: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
        override suspend fun removeUserFromClub(clubCode: String, userId: String) =
            Result.Error(DataError.Sync.NOT_SIGNED_IN)
    }

    private val useCase = UpdateShelfStyleUseCaseImpl(
        mockRepository,
        mockClubOperations,
        mockAuthService,
    )

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    @Test
    fun `personal shelf - updates style successfully`() = runTest {
        val personalShelf = TestShelfBuilder()
            .withId("personal-shelf")
            .withName("My Books")
            .withStyle(ShelfStyle.DarkWood)
            .withIsBookClub(false)
            .build()
        mockRepository.shelfByIdToReturn = personalShelf

        val result = useCase("personal-shelf", ShelfStyle.SilverMetal)

        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call updateShelf", mockRepository.updateShelfCalled)
        assertEquals("Should update to new style", ShelfStyle.SilverMetal, mockRepository.lastUpdatedShelf?.shelfStyle)
    }

    @Test
    fun `book club shelf - updates style for creator`() = runTest {
        val clubShelf = TestShelfBuilder()
            .withId("club-shelf")
            .withName("Club Books")
            .withStyle(ShelfStyle.DarkWood)
            .withIsBookClub(true)
            .withClubCode("ABC123")
            .withClubCreatorId("creator-id")
            .build()
        mockRepository.shelfByIdToReturn = clubShelf

        val result = useCase("club-shelf", ShelfStyle.SilverMetal)

        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call updateShelf", mockRepository.updateShelfCalled)
        assertEquals("Should update to new style", ShelfStyle.SilverMetal, mockRepository.lastUpdatedShelf?.shelfStyle)
    }
}
