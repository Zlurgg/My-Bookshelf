package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.ExportedBookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder

/**
 * Test for BookshelfExportMapper - Focused on data transformation logic.
 * Tests business logic: Export data creation with timestamp/ID generation.
 * Mocks: TimeProvider, IdGenerator
 */
class BookshelfExportMapperTest {

    private val mockTimeProvider = SimpleMockTimeProvider()
    private val mockIdGenerator = SimpleMockIdGenerator()
    private val mapper = BookshelfExportMapper(mockTimeProvider, mockIdGenerator)

    @Test
    fun `toExportData creates export data with correct structure`() {
        // Given
        val shelf = TestShelfBuilder()
            .withId("original-id")
            .withName("Fiction")
            .withStyle(ShelfStyle.DarkWood)
            .build()
        mockTimeProvider.currentTime = 1704067200000L // 2024-01-01 00:00:00 UTC

        // When
        val exportData = mapper.toExportData(shelf)

        // Then
        assertEquals("Should have format version 1", 1, exportData.formatVersion)
        assertTrue("Should have timestamp", exportData.exportedAt.isNotEmpty())
        assertEquals("Should have correct shelf name", "Fiction", exportData.bookshelf.name)
        assertEquals("Should preserve shelf style", ShelfStyle.DarkWood, exportData.bookshelf.shelfStyle)
    }

    @Test
    fun `fromExportData uses default name when no custom name provided`() {
        // Given
        val exportData = createExportData("Original Name")
        mockIdGenerator.nextId = "new-id"

        // When
        val shelf = mapper.fromExportData(exportData)

        // Then
        assertEquals("Should use default name", "Original Name", shelf.name)
        assertEquals("Should generate new ID", "new-id", shelf.id)
    }

    @Test
    fun `fromExportData uses custom name when provided`() {
        // Given
        val exportData = createExportData("Original Name")
        mockIdGenerator.nextId = "new-id"

        // When
        val shelf = mapper.fromExportData(exportData, customName = "Custom Name")

        // Then
        assertEquals("Should use custom name", "Custom Name", shelf.name)
        assertEquals("Should generate new ID", "new-id", shelf.id)
    }

    @Test
    fun `fromExportData generates new ID for imported shelf`() {
        // Given
        val exportData = createExportData("Test")
        mockIdGenerator.nextId = "generated-id-1"

        // When
        val shelf1 = mapper.fromExportData(exportData)
        mockIdGenerator.nextId = "generated-id-2"
        val shelf2 = mapper.fromExportData(exportData)

        // Then
        assertNotEquals("Should generate different IDs", shelf1.id, shelf2.id)
        assertEquals("First import should have first ID", "generated-id-1", shelf1.id)
        assertEquals("Second import should have second ID", "generated-id-2", shelf2.id)
    }

    // Simplified mocks
    private class SimpleMockTimeProvider : TimeProvider {
        var currentTime = 0L
        override fun currentTimeMillis(): Long = currentTime
    }

    private class SimpleMockIdGenerator : IdGenerator {
        var nextId = "test-id"
        override fun generateId(): String = nextId
    }

    private fun createExportData(name: String): BookshelfExportData {
        return BookshelfExportData(
            formatVersion = 1,
            exportedAt = "2024-01-01T00:00:00",
            bookshelf = ExportedBookshelf(
                name = name,
                shelfStyle = ShelfStyle.DarkWood,
                books = emptyList()
            )
        )
    }
}
