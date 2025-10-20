package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchedBookDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookIdentifier
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.ExportedBookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder

/**
 * Test for BookshelfExportMapper - Focused on data transformation logic.
 * Tests business logic: Export data creation with ID generation.
 * Mocks: IdGenerator, RemoteBookDataSource
 */
class BookshelfExportMapperTest {

    private val mockIdGenerator = SimpleMockIdGenerator()
    private val mockRemoteDataSource = SimpleMockRemoteBookDataSource()
    private val mapper = BookshelfExportMapper(mockIdGenerator, mockRemoteDataSource)

    @Test
    fun `toExportData creates export data with correct structure`() {
        // Given
        val shelf = TestShelfBuilder()
            .withId("original-id")
            .withName("Fiction")
            .withStyle(ShelfStyle.DarkWood)
            .build()

        // When
        val exportData = mapper.toExportData(shelf)

        // Then
        assertEquals("Should have correct shelf name", "Fiction", exportData.bookshelf.name)
        assertEquals("Should preserve shelf style", ShelfStyle.DarkWood, exportData.bookshelf.shelfStyle)
    }

    @Test
    fun `fromExportData uses default name when no custom name provided`() = runTest {
        // Given
        val exportData = createExportData("Original Name")
        mockIdGenerator.nextId = "new-id"

        // When
        val result = mapper.fromExportData(exportData)

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val shelf = (result as Result.Success).data
        assertEquals("Should use default name", "Original Name", shelf.name)
        assertEquals("Should generate new ID", "new-id", shelf.id)
    }

    @Test
    fun `fromExportData uses custom name when provided`() = runTest {
        // Given
        val exportData = createExportData("Original Name")
        mockIdGenerator.nextId = "new-id"

        // When
        val result = mapper.fromExportData(exportData, customName = "Custom Name")

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val shelf = (result as Result.Success).data
        assertEquals("Should use custom name", "Custom Name", shelf.name)
        assertEquals("Should generate new ID", "new-id", shelf.id)
    }

    @Test
    fun `fromExportData generates new ID for imported shelf`() = runTest {
        // Given
        val exportData = createExportData("Test")
        mockIdGenerator.nextId = "generated-id-1"

        // When
        val result1 = mapper.fromExportData(exportData)
        mockIdGenerator.nextId = "generated-id-2"
        val result2 = mapper.fromExportData(exportData)

        // Then
        assertTrue("Should succeed", result1 is Result.Success)
        assertTrue("Should succeed", result2 is Result.Success)
        val shelf1 = (result1 as Result.Success).data
        val shelf2 = (result2 as Result.Success).data
        assertNotEquals("Should generate different IDs", shelf1.id, shelf2.id)
        assertEquals("First import should have first ID", "generated-id-1", shelf1.id)
        assertEquals("Second import should have second ID", "generated-id-2", shelf2.id)
    }

    // Simplified mocks
    private class SimpleMockIdGenerator : IdGenerator {
        var nextId = "test-id"
        override fun generateId(): String = nextId
    }

    private class SimpleMockRemoteBookDataSource : RemoteBookDataSource {
        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            sort: String?
        ): Result<SearchResponseDto, DataError.Remote> {
            // Return empty results for tests (no books to import)
            return Result.Success(SearchResponseDto(results = emptyList()))
        }

        override suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote> {
            return Result.Success(BookWorkDto(description = null))
        }
    }

    private fun createExportData(name: String): BookshelfExportData {
        return BookshelfExportData(
            bookshelf = ExportedBookshelf(
                name = name,
                shelfStyle = ShelfStyle.DarkWood,
                bookIds = emptyList()
            )
        )
    }
}
