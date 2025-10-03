package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.ExportedBookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

/**
 * Test for BookshelfImportValidatorImpl - Focused on validation logic.
 * Tests business logic: Format validation and name conflict detection.
 * Mocks: MockBookcaseRepository
 */
class BookshelfImportValidatorImplTest {

    private lateinit var mockBookcaseRepository: MockBookcaseRepository
    private lateinit var validator: BookshelfImportValidatorImpl

    @Before
    fun setup() {
        mockBookcaseRepository = MockBookcaseRepository()
        validator = BookshelfImportValidatorImpl(mockBookcaseRepository)
    }

    @Test
    fun `validateFormat accepts valid export data`() {
        // Given
        val validExportData = createExportData(
            formatVersion = 1,
            shelfName = "Fiction"
        )

        // When
        val result = validator.validateFormat(validExportData)

        // Then
        assertTrue("Should succeed", result is Result.Success)
    }

    @Test
    fun `validateFormat rejects unsupported format version`() {
        // Given
        val futureVersion = createExportData(
            formatVersion = 2,
            shelfName = "Fiction"
        )

        // When
        val result = validator.validateFormat(futureVersion)

        // Then
        assertTrue("Should fail", result is Result.Error)
        assertEquals("Should return unsupported format error",
            DataError.Local.UNSUPPORTED_FORMAT_VERSION,
            (result as Result.Error).error)
    }

    @Test
    fun `validateFormat rejects blank shelf name`() {
        // Given
        val blankName = createExportData(
            formatVersion = 1,
            shelfName = ""
        )

        // When
        val result = validator.validateFormat(blankName)

        // Then
        assertTrue("Should fail", result is Result.Error)
        assertEquals("Should return validation error",
            DataError.Local.VALIDATION_ERROR,
            (result as Result.Error).error)
    }

    @Test
    fun `validateFormat rejects whitespace-only shelf name`() {
        // Given
        val whitespaceOnly = createExportData(
            formatVersion = 1,
            shelfName = "   "
        )

        // When
        val result = validator.validateFormat(whitespaceOnly)

        // Then
        assertTrue("Should fail", result is Result.Error)
        assertEquals("Should return validation error",
            DataError.Local.VALIDATION_ERROR,
            (result as Result.Error).error)
    }

    @Test
    fun `checkNameConflict returns null when no conflict exists`() = runTest {
        // Given
        val existingShelf = TestShelfBuilder()
            .withName("Existing Shelf")
            .build()
        mockBookcaseRepository.configureShelves(listOf(existingShelf))

        // When
        val result = validator.checkNameConflict("New Shelf")

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertNull("Should not find conflict", (result as Result.Success).data)
    }

    @Test
    fun `checkNameConflict returns conflicting name when duplicate found`() = runTest {
        // Given
        val existingShelf = TestShelfBuilder()
            .withName("Fiction")
            .build()
        mockBookcaseRepository.configureShelves(listOf(existingShelf))

        // When
        val result = validator.checkNameConflict("Fiction")

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertEquals("Should find conflict",
            "Fiction",
            (result as Result.Success).data)
    }

    @Test
    fun `checkNameConflict handles multiple shelves correctly`() = runTest {
        // Given
        val shelf1 = TestShelfBuilder().withName("Fiction").build()
        val shelf2 = TestShelfBuilder().withName("Non-Fiction").build()
        val shelf3 = TestShelfBuilder().withName("Biography").build()
        mockBookcaseRepository.configureShelves(listOf(shelf1, shelf2, shelf3))

        // When
        val result = validator.checkNameConflict("Non-Fiction")

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertEquals("Should find exact match",
            "Non-Fiction",
            (result as Result.Success).data)
    }

    @Test
    fun `checkNameConflict handles empty bookcase`() = runTest {
        // Given
        mockBookcaseRepository.configureShelves(emptyList())

        // When
        val result = validator.checkNameConflict("Any Name")

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertNull("Should not find conflict in empty bookcase",
            (result as Result.Success).data)
    }

    @Test
    fun `checkNameConflict handles repository exception`() = runTest {
        // Given
        mockBookcaseRepository.shouldThrowException = true

        // When
        val result = validator.checkNameConflict("Fiction")

        // Then
        assertTrue("Should fail", result is Result.Error)
        // Error will always be DataError.Local due to ErrorMapper implementation
    }

    @Test
    fun `validateFormat accepts format version 1`() {
        // Given
        val version1 = createExportData(
            formatVersion = 1,
            shelfName = "Valid Shelf"
        )

        // When
        val result = validator.validateFormat(version1)

        // Then
        assertTrue("Should succeed for version 1", result is Result.Success)
    }

    private fun createExportData(formatVersion: Int, shelfName: String): BookshelfExportData {
        return BookshelfExportData(
            formatVersion = formatVersion,
            exportedAt = "2024-01-01T00:00:00",
            bookshelf = ExportedBookshelf(
                name = shelfName,
                shelfStyle = ShelfStyle.DarkWood,
                books = emptyList()
            )
        )
    }
}
