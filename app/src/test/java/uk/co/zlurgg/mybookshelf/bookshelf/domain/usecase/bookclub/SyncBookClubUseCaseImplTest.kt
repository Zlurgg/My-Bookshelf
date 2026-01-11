package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository

class SyncBookClubUseCaseImplTest {

    private val mockRepository = MockBookClubRepository()
    private val useCase = SyncBookClubUseCaseImpl(mockRepository)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    // ========== Successful Sync Tests ==========

    @Test
    fun `execute - when sync succeeds with no changes - returns zero counts`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Success(SyncResult(booksAdded = 0, booksRemoved = 0))

        // When
        val result = useCase("CLUB1234", "local-shelf-id")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val syncResult = (result as Result.Success).data
        assertEquals(0, syncResult.booksAdded)
        assertEquals(0, syncResult.booksRemoved)
    }

    @Test
    fun `execute - when sync adds books - returns correct added count`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Success(SyncResult(booksAdded = 5, booksRemoved = 0))

        // When
        val result = useCase("CLUB1234", "local-shelf-id")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val syncResult = (result as Result.Success).data
        assertEquals(5, syncResult.booksAdded)
        assertEquals(0, syncResult.booksRemoved)
    }

    @Test
    fun `execute - when sync removes books - returns correct removed count`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Success(SyncResult(booksAdded = 0, booksRemoved = 3))

        // When
        val result = useCase("CLUB1234", "local-shelf-id")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val syncResult = (result as Result.Success).data
        assertEquals(0, syncResult.booksAdded)
        assertEquals(3, syncResult.booksRemoved)
    }

    @Test
    fun `execute - when sync adds and removes books - returns both counts`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Success(SyncResult(booksAdded = 7, booksRemoved = 2))

        // When
        val result = useCase("CLUB1234", "local-shelf-id")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val syncResult = (result as Result.Success).data
        assertEquals(7, syncResult.booksAdded)
        assertEquals(2, syncResult.booksRemoved)
    }

    @Test
    fun `execute - calls repository with correct parameters`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Success(SyncResult(0, 0))

        // When
        useCase("MYCLUB99", "shelf-abc-123")

        // Then
        assertTrue("Should call syncBooksFromClub on repository", mockRepository.syncBooksFromClubCalled)
        assertEquals("MYCLUB99", mockRepository.lastSyncFromClubCode)
        assertEquals("shelf-abc-123", mockRepository.lastSyncFromClubShelfId)
    }

    // ========== Error Propagation Tests ==========

    @Test
    fun `execute - when repository returns NETWORK_ERROR - propagates error`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Error(DataError.Sync.NETWORK_ERROR)

        // When
        val result = useCase("CLUB1234", "local-shelf-id")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns NOT_SIGNED_IN - propagates error`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Error(DataError.Sync.NOT_SIGNED_IN)

        // When
        val result = useCase("CLUB1234", "local-shelf-id")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NOT_SIGNED_IN, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns CLUB_NOT_FOUND - propagates error`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Error(DataError.Sync.CLUB_NOT_FOUND)

        // When
        val result = useCase("INVALID1", "local-shelf-id")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.CLUB_NOT_FOUND, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns PERMISSION_DENIED - propagates error`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Error(DataError.Sync.PERMISSION_DENIED)

        // When
        val result = useCase("CLUB1234", "local-shelf-id")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.PERMISSION_DENIED, (result as Result.Error).error)
    }

    @Test
    fun `execute - when repository returns NOT_MEMBER - propagates error`() = runTest {
        // Given
        mockRepository.syncBooksFromClubResult = Result.Error(DataError.Sync.NOT_MEMBER)

        // When
        val result = useCase("CLUB1234", "local-shelf-id")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NOT_MEMBER, (result as Result.Error).error)
    }
}
