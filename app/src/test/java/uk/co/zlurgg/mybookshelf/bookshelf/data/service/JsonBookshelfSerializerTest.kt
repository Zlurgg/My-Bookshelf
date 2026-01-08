package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder

/**
 * Test for JsonBookshelfSerializer - Focused on JSON serialization logic.
 * Tests business logic: JSON encoding/decoding with proper error handling.
 * Mocks: IdGenerator, RemoteBookDataSource
 */
class JsonBookshelfSerializerTest {
    private val mockIdGenerator = SimpleMockIdGenerator()
    private val mockRemoteDataSource = SimpleMockRemoteBookDataSource()
    private val exportMapper = BookshelfExportMapper(mockIdGenerator, mockRemoteDataSource)
    private val serializer = JsonBookshelfSerializer(exportMapper)

    @Test
    fun `serialize converts shelf to valid JSON string`() {
        // Given
        val shelf =
            TestShelfBuilder()
                .withName("Fiction")
                .withStyle(ShelfStyle.DarkWood)
                .build()

        // When
        val result = serializer.serialize(shelf)

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val jsonString = (result as Result.Success).data
        assertTrue("Should contain shelf name", jsonString.contains("Fiction"))
        assertTrue("Should contain shelf style", jsonString.contains("DarkWood"))
    }

    @Test
    fun `deserialize converts valid JSON to ExportData`() {
        // Given
        val validJson =
            """
            {
                "bookshelf": {
                    "name": "Fiction",
                    "shelfStyle": "DarkWood",
                    "bookIds": []
                }
            }
            """.trimIndent()

        // When
        val result = serializer.deserialize(validJson)

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val exportData = (result as Result.Success).data
        assertEquals("Should have correct name", "Fiction", exportData.bookshelf.name)
        assertEquals("Should have correct style", ShelfStyle.DarkWood, exportData.bookshelf.shelfStyle)
    }

    @Test
    fun `deserialize handles invalid JSON`() {
        // Given
        val invalidJson = "{ invalid json }"

        // When
        val result = serializer.deserialize(invalidJson)

        // Then
        assertTrue("Should fail", result is Result.Error)
        assertEquals(
            "Should return serialization error",
            DataError.Local.SERIALIZATION_ERROR,
            (result as Result.Error).error,
        )
    }

    @Test
    fun `serialize and deserialize round trip preserves data`() {
        // Given
        val originalShelf =
            TestShelfBuilder()
                .withName("Science Fiction")
                .withStyle(ShelfStyle.GreyMetal)
                .build()

        // When
        val serializeResult = serializer.serialize(originalShelf)
        assertTrue("Serialize should succeed", serializeResult is Result.Success)
        val jsonString = (serializeResult as Result.Success).data

        val deserializeResult = serializer.deserialize(jsonString)
        assertTrue("Deserialize should succeed", deserializeResult is Result.Success)
        val exportData = (deserializeResult as Result.Success).data

        // Then
        assertEquals("Should preserve name", "Science Fiction", exportData.bookshelf.name)
        assertEquals("Should preserve style", ShelfStyle.GreyMetal, exportData.bookshelf.shelfStyle)
    }

    @Test
    fun `deserialize handles malformed structure`() {
        // Given - valid JSON but wrong structure
        val wrongStructure =
            """
            {
                "wrongField": "value"
            }
            """.trimIndent()

        // When
        val result = serializer.deserialize(wrongStructure)

        // Then
        assertTrue("Should fail", result is Result.Error)
    }

    @Test
    fun `serialize handles shelf with books`() {
        // Given
        val shelf =
            TestShelfBuilder()
                .withName("Fantasy")
                .withStyle(ShelfStyle.DarkWood)
                .withBooks(
                    listOf(
                        createExportedBook("book-1", "The Hobbit"),
                        createExportedBook("book-2", "Lord of the Rings"),
                    ),
                )
                .build()

        // When
        val result = serializer.serialize(shelf)

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val jsonString = (result as Result.Success).data
        assertTrue("Should contain book IDs", jsonString.contains("book-1"))
        assertTrue("Should contain book IDs", jsonString.contains("book-2"))
        assertTrue("Should contain bookIds field", jsonString.contains("\"bookIds\""))
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
            sort: String?,
        ): Result<SearchResponseDto, DataError.Remote> {
            return Result.Success(SearchResponseDto(numFound = 0, results = emptyList()))
        }

        override suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote> {
            return Result.Success(BookWorkDto(description = null))
        }
    }

    private fun createExportedBook(
        id: String,
        title: String,
    ) = uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book(
        id = id,
        title = title,
        authors = emptyList(),
        imageUrl = "",
        description = null,
        languages = emptyList(),
        firstPublishYear = null,
        averageRating = null,
        ratingCount = null,
        numPages = null,
        numEditions = 0,
        purchased = false,
        spineColor = 0xFF8B4513.toInt(),
    )
}
