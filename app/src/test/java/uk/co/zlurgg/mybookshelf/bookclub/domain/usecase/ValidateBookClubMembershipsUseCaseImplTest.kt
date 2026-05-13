package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockAuthService
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

class ValidateBookClubMembershipsUseCaseImplTest {

    private val mockAuthService = MockAuthService()
    private val mockBookClubRepository = MockBookClubRepository()
    private val mockBookcaseRepository = MockBookcaseRepository()
    private val useCase = ValidateBookClubMembershipsUseCaseImpl(
        mockAuthService,
        mockBookClubRepository,
        mockBookcaseRepository,
    )

    @After
    fun tearDown() {
        mockAuthService.reset()
        mockBookClubRepository.reset()
        mockBookcaseRepository.reset()
    }

    // ========== User Not Signed In Tests ==========

    @Test
    fun `returns empty result when user is not signed in`() = runTest {
        // Given
        mockAuthService.configureSignedOut()

        // When
        val result = useCase()

        // Then
        assertTrue("Should return success", result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(emptyList<String>(), data.deletedClubNames)
        assertEquals(emptyMap<String, Int>(), data.memberCounts)
    }

    // ========== No Memberships Tests ==========

    @Test
    fun `returns empty result when user has no book club memberships`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockBookClubRepository.myBookClubs = emptyList()

        // When
        val result = useCase()

        // Then
        assertTrue("Should return success", result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(emptyList<String>(), data.deletedClubNames)
        assertEquals(emptyMap<String, Int>(), data.memberCounts)
    }

    // ========== Club Still Exists Tests ==========

    @Test
    fun `returns empty deletions and member counts when all clubs still exist`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        val currentTime = System.currentTimeMillis()
        mockBookClubRepository.myBookClubs = listOf(
            BookClubMembership(
                clubCode = "CLUB1234",
                localShelfId = "local-shelf-1",
                joinedAt = currentTime,
            )
        )
        mockBookClubRepository.getBookClubResult = Result.Success(
            BookClub(
                code = "CLUB1234",
                name = "Test Club",
                style = ShelfStyle.DarkWood,
                createdAt = currentTime,
                createdBy = "creator-user-id",
                createdByName = "Creator Name",
                bookCount = 5,
                memberCount = 2
            )
        )

        // When
        val result = useCase()

        // Then
        assertTrue("Should return success", result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(emptyList<String>(), data.deletedClubNames)
        assertEquals(mapOf("CLUB1234" to 2), data.memberCounts)
    }

    // ========== Club Deleted Tests ==========

    @Test
    fun `returns shelf name when club was deleted`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        val currentTime = System.currentTimeMillis()
        val testShelf = TestShelfBuilder()
            .withId("local-shelf-1")
            .withName("Science Fiction Club")
            .withIsBookClub(true)
            .withClubCode("CLUB1234")
            .build()
        mockBookcaseRepository.shelfByIdToReturn = testShelf
        mockBookClubRepository.myBookClubs = listOf(
            BookClubMembership(
                clubCode = "CLUB1234",
                localShelfId = "local-shelf-1",
                joinedAt = currentTime,
            )
        )
        mockBookClubRepository.getBookClubResult = Result.Success(null) // Club deleted

        // When
        val result = useCase()

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals(listOf("Science Fiction Club"), (result as Result.Success).data.deletedClubNames)
        assertTrue("Should call convertClubToPersonalShelf", mockBookClubRepository.convertClubToPersonalShelfCalled)
        assertEquals("CLUB1234", mockBookClubRepository.lastConvertCode)
    }

    @Test
    fun `converts multiple deleted clubs to personal shelves`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        val currentTime = System.currentTimeMillis()

        // Two memberships
        mockBookClubRepository.myBookClubs = listOf(
            BookClubMembership(
                clubCode = "CLUB1",
                localShelfId = "local-shelf-1",
                joinedAt = currentTime,
            ),
            BookClubMembership(
                clubCode = "CLUB2",
                localShelfId = "local-shelf-2",
                joinedAt = currentTime,
            )
        )

        // Both clubs deleted
        mockBookClubRepository.getBookClubResult = Result.Success(null)

        // Configure shelf names
        val shelf1 = TestShelfBuilder()
            .withId("local-shelf-1")
            .withName("Club One")
            .build()
        val shelf2 = TestShelfBuilder()
            .withId("local-shelf-2")
            .withName("Club Two")
            .build()

        // Return different shelves based on ID
        mockBookcaseRepository.shelfById = mapOf(
            "local-shelf-1" to shelf1,
            "local-shelf-2" to shelf2
        )

        // When
        val result = useCase()

        // Then
        assertTrue("Should return success", result is Result.Success)
        val convertedNames = (result as Result.Success).data.deletedClubNames
        assertEquals(2, convertedNames.size)
        assertTrue("Should include Club One", convertedNames.contains("Club One"))
        assertTrue("Should include Club Two", convertedNames.contains("Club Two"))
    }

    @Test
    fun `uses Unknown Club for shelf with null name`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        val currentTime = System.currentTimeMillis()
        mockBookcaseRepository.shelfByIdToReturn = null // Shelf not found
        mockBookClubRepository.myBookClubs = listOf(
            BookClubMembership(
                clubCode = "CLUB1234",
                localShelfId = "non-existent-shelf",
                joinedAt = currentTime,
            )
        )
        mockBookClubRepository.getBookClubResult = Result.Success(null) // Club deleted

        // When
        val result = useCase()

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals(listOf("Unknown Club"), (result as Result.Success).data.deletedClubNames)
    }

    // ========== Network Error Tests ==========

    @Test
    fun `does not convert when network error occurs`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        val currentTime = System.currentTimeMillis()
        mockBookClubRepository.myBookClubs = listOf(
            BookClubMembership(
                clubCode = "CLUB1234",
                localShelfId = "local-shelf-1",
                joinedAt = currentTime,
            )
        )
        mockBookClubRepository.getBookClubResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        // When
        val result = useCase()

        // Then
        assertTrue("Should return success even on network error", result is Result.Success)
        assertEquals(emptyList<String>(), (result as Result.Success).data.deletedClubNames)
        assertTrue(
            "Should not call convertClubToPersonalShelf on network error",
            !mockBookClubRepository.convertClubToPersonalShelfCalled
        )
    }
}
