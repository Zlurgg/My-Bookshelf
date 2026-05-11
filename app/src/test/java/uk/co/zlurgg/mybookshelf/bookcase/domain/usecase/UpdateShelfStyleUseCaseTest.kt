package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.StubClubOperations

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

    private val mockClubOperations = StubClubOperations()

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
