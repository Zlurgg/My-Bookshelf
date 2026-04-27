package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository

class GetBookClubPreviewUseCaseImplTest {

    private val mockRepository = MockBookClubRepository()
    private val useCase = GetBookClubPreviewUseCaseImpl(mockRepository)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    // ========== Successful Retrieval Tests ==========

    @Test
    fun `invoke - when club exists - returns book club data`() = runTest {
        // Given
        val expectedClub = BookClub(
            code = "TEST1234",
            name = "Science Fiction Club",
            style = ShelfStyle.DarkWood,
            createdAt = System.currentTimeMillis(),
            createdBy = "creator-id",
            createdByName = "John Doe",
            bookCount = 15,
            memberCount = 5
        )
        mockRepository.configureBookClub(expectedClub)

        // When
        val result = useCase("TEST1234")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val club = (result as Result.Success).data
        assertEquals(expectedClub.code, club?.code)
        assertEquals(expectedClub.name, club?.name)
        assertEquals(expectedClub.bookCount, club?.bookCount)
        assertEquals(expectedClub.memberCount, club?.memberCount)
    }

    @Test
    fun `invoke - calls repository with correct code`() = runTest {
        // Given
        val code = "MYCLUB99"
        mockRepository.configureBookClubNotFound()

        // When
        useCase(code)

        // Then
        assertTrue("Should call getBookClub on repository", mockRepository.getBookClubCalled)
        assertEquals("MYCLUB99", mockRepository.lastGetBookClubCode)
    }

    @Test
    fun `invoke - when club not found - returns null in success`() = runTest {
        // Given
        mockRepository.configureBookClubNotFound()

        // When
        val result = useCase("INVALID1")

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertNull("Should return null for non-existent club", (result as Result.Success).data)
    }

    // ========== Error Propagation Tests ==========

    @Test
    fun `invoke - when repository returns NETWORK_ERROR - propagates error`() = runTest {
        // Given
        mockRepository.getBookClubResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        // When
        val result = useCase("TEST1234")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `invoke - when repository returns PERMISSION_DENIED - propagates error`() = runTest {
        // Given
        mockRepository.getBookClubResult = Result.Error(DataError.Sync.PERMISSION_DENIED)

        // When
        val result = useCase("TEST1234")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.PERMISSION_DENIED, (result as Result.Error).error)
    }

    @Test
    fun `invoke - when repository returns NOT_SIGNED_IN - propagates error`() = runTest {
        // Given
        mockRepository.getBookClubResult = Result.Error(DataError.Sync.NOT_SIGNED_IN)

        // When
        val result = useCase("TEST1234")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NOT_SIGNED_IN, (result as Result.Error).error)
    }

    // ========== Data Preservation Tests ==========

    @Test
    fun `invoke - preserves all book club fields from repository`() = runTest {
        // Given
        val createdAt = 1704067200000L // Jan 1, 2024
        val club = BookClub(
            code = "FULL1234",
            name = "Complete Book Club",
            style = ShelfStyle.SilverMetal,
            createdAt = createdAt,
            createdBy = "user-abc-123",
            createdByName = "Jane Smith",
            bookCount = 42,
            memberCount = 10
        )
        mockRepository.configureBookClub(club)

        // When
        val result = useCase("FULL1234")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val returnedClub = (result as Result.Success).data!!
        assertEquals("FULL1234", returnedClub.code)
        assertEquals("Complete Book Club", returnedClub.name)
        assertEquals(ShelfStyle.SilverMetal, returnedClub.style)
        assertEquals(createdAt, returnedClub.createdAt)
        assertEquals("user-abc-123", returnedClub.createdBy)
        assertEquals("Jane Smith", returnedClub.createdByName)
        assertEquals(42, returnedClub.bookCount)
        assertEquals(10, returnedClub.memberCount)
    }
}
