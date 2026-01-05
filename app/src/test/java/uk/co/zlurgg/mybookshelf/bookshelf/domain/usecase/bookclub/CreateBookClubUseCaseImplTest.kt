package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

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
        val shelfId = "test-shelf-123"
        mockRepository.createBookClubResult = Result.Success("CLUB1234")

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("CLUB1234", (result as Result.Success).data)
    }

    @Test
    fun `execute - calls repository with correct shelf ID`() = runTest {
        // Given
        val shelfId = "my-fiction-shelf"
        mockRepository.createBookClubResult = Result.Success("TEST5678")

        // When
        useCase.execute(shelfId)

        // Then
        assertTrue("Should call createBookClub on repository", mockRepository.createBookClubCalled)
        assertEquals("my-fiction-shelf", mockRepository.lastCreateShelfId)
    }

    // ========== Error Propagation Tests ==========

    @Test
    fun `execute - when repository returns NOT_SIGNED_IN - propagates error`() = runTest {
        // Given
        val shelfId = "test-shelf"
        mockRepository.createBookClubResult = Result.Error(DataError.Sync.NOT_SIGNED_IN)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NOT_SIGNED_IN, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns NETWORK_ERROR - propagates error`() = runTest {
        // Given
        val shelfId = "test-shelf"
        mockRepository.createBookClubResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns QUOTA_EXCEEDED - propagates error`() = runTest {
        // Given
        val shelfId = "test-shelf"
        mockRepository.createBookClubResult = Result.Error(DataError.Sync.QUOTA_EXCEEDED)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.QUOTA_EXCEEDED, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns PERMISSION_DENIED - propagates error`() = runTest {
        // Given
        val shelfId = "test-shelf"
        mockRepository.createBookClubResult = Result.Error(DataError.Sync.PERMISSION_DENIED)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.PERMISSION_DENIED, (result as Result.Error).error)
    }

    // ========== Edge Cases ==========

    @Test
    fun `execute - with empty shelf ID - delegates to repository`() = runTest {
        // Given
        val emptyShelfId = ""
        mockRepository.createBookClubResult = Result.Success("CODE1234")

        // When
        val result = useCase.execute(emptyShelfId)

        // Then
        assertTrue("Should call repository", mockRepository.createBookClubCalled)
        assertEquals("", mockRepository.lastCreateShelfId)
        assertTrue("Should return success from repository", result is Result.Success)
    }
}
