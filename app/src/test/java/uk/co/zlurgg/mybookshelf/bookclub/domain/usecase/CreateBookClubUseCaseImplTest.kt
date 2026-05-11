package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository

class CreateBookClubUseCaseImplTest {

    private val mockRepository = MockBookClubRepository()
    private val useCase = CreateBookClubUseCaseImpl(mockRepository)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    // ========== Successful Creation Tests ==========

    @Test
    fun `execute - when repository succeeds - returns club code`() = runTest {
        // Given
        mockRepository.createBookClubResult = Result.Success("CLUB1234")

        // When
        val result = useCase("My Club", "DarkWood")

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("CLUB1234", (result as Result.Success).data)
    }

    @Test
    fun `execute - calls repository with correct params`() = runTest {
        // Given
        mockRepository.createBookClubResult = Result.Success("TEST5678")

        // When
        useCase("Fiction Club", "LightWood", "shelf-123")

        // Then
        assertTrue("Should call createBookClub on repository", mockRepository.createBookClubCalled)
        assertEquals("Fiction Club", mockRepository.lastCreateName)
        assertEquals("LightWood", mockRepository.lastCreateShelfStyle)
        assertEquals("shelf-123", mockRepository.lastCreateSourceShelfId)
    }

    @Test
    fun `execute - direct creation passes null sourceShelfId`() = runTest {
        // Given
        mockRepository.createBookClubResult = Result.Success("DIRECT1")

        // When
        useCase("Direct Club", "DarkWood")

        // Then
        assertTrue("Should call createBookClub on repository", mockRepository.createBookClubCalled)
        assertEquals("Direct Club", mockRepository.lastCreateName)
        assertEquals(null, mockRepository.lastCreateSourceShelfId)
    }

    // ========== Error Propagation Tests ==========

    @Test
    fun `execute - when repository returns NOT_SIGNED_IN - propagates error`() = runTest {
        // Given
        mockRepository.createBookClubResult = Result.Error(DataError.Sync.NOT_SIGNED_IN)

        // When
        val result = useCase("Club", "DarkWood")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NOT_SIGNED_IN, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns NETWORK_ERROR - propagates error`() = runTest {
        // Given
        mockRepository.createBookClubResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        // When
        val result = useCase("Club", "DarkWood")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns QUOTA_EXCEEDED - propagates error`() = runTest {
        // Given
        mockRepository.createBookClubResult = Result.Error(DataError.Sync.QUOTA_EXCEEDED)

        // When
        val result = useCase("Club", "DarkWood")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.QUOTA_EXCEEDED, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns PERMISSION_DENIED - propagates error`() = runTest {
        // Given
        mockRepository.createBookClubResult = Result.Error(DataError.Sync.PERMISSION_DENIED)

        // When
        val result = useCase("Club", "DarkWood")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.PERMISSION_DENIED, (result as Result.Error).error)
    }

    // ========== MAX_BOOK_CLUBS Limit Tests ==========

    @Test
    fun `execute - when at max book clubs - returns MAX_BOOK_CLUBS_REACHED`() = runTest {
        // Given — fill up to max
        mockRepository.myBookClubs = (1..5).map {
            uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubMembership(
                clubCode = "CLUB$it",
                localShelfId = "shelf-$it",
                joinedAt = 1000L,
            )
        }
        mockRepository.createBookClubResult = Result.Success("SHOULDNT_REACH")

        // When
        val result = useCase("New Club", "DarkWood")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.MAX_BOOK_CLUBS_REACHED, (result as Result.Error).error)
    }

    @Test
    fun `execute - direct creation also checks max limit`() = runTest {
        // Given — fill up to max, no sourceShelfId
        mockRepository.myBookClubs = (1..5).map {
            uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubMembership(
                clubCode = "CLUB$it",
                localShelfId = "shelf-$it",
                joinedAt = 1000L,
            )
        }

        // When
        val result = useCase("Direct Club", "LightWood")

        // Then
        assertTrue("Should return error for direct path too", result is Result.Error)
        assertEquals(DataError.Sync.MAX_BOOK_CLUBS_REACHED, (result as Result.Error).error)
    }
}
