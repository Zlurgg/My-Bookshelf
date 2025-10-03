package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookshelfExportService

/**
 * Tests for ShareBookshelfUseCase demonstrating service delegation pattern.
 * Tests business logic:
 * - Successful share delegation
 * - Error handling from export service
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShareBookshelfUseCaseTest {

    private val mockExportService = MockBookshelfExportService()
    private val useCase = ShareBookshelfUseCaseImpl(mockExportService)

    @After
    fun tearDown() {
        mockExportService.reset()
    }

    @Test
    fun `delegates share to export service successfully`() = runTest {
        // Given
        val shelfId = "fiction-shelf"
        mockExportService.shareResult = Result.Success(Unit)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call export service", mockExportService.shareBookshelfCalled)
        assertTrue("Should pass correct shelf ID", mockExportService.lastShareShelfId == shelfId)
    }

    @Test
    fun `returns error when export service fails`() = runTest {
        // Given
        val shelfId = "invalid-shelf"
        mockExportService.shareResult = Result.Error(DataError.Local.UNKNOWN)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertTrue("Should call export service", mockExportService.shareBookshelfCalled)
    }

    @Test
    fun `handles empty shelf ID`() = runTest {
        // Given
        val emptyShelfId = ""
        mockExportService.shareResult = Result.Error(DataError.Local.UNKNOWN)

        // When
        useCase.execute(emptyShelfId)

        // Then
        assertTrue("Should call export service with empty ID", mockExportService.shareBookshelfCalled)
        assertTrue("Should pass empty ID", mockExportService.lastShareShelfId == "")
    }

    @Test
    fun `propagates export service result correctly`() = runTest {
        // Given
        val shelfId = "test-shelf"
        val expectedError = DataError.Local.DISK_FULL
        mockExportService.shareResult = Result.Error(expectedError)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue("Should propagate specific error", error == expectedError)
    }
}
