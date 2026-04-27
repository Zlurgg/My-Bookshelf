package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.book.data.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.book.data.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator

/**
 * Integration test for JSON serialization with real data.
 * Tests JsonBookshelfSerializer with actual JSON encoding/decoding.
 *
 * This is a medium-scope test (Google's 20% integration test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class SerializationIntegrationTest {

    private lateinit var serializer: JsonBookshelfSerializer
    private lateinit var mapper: BookshelfExportMapper

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-id-${counter++}"
    }

    private val mockRemoteDataSource = object : RemoteBookDataSource {
        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            sort: String?
        ): Result<SearchResponseDto, DataError.Remote> {
            return Result.Success(SearchResponseDto(numFound = 0, results = emptyList()))
        }

        override suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote> {
            return Result.Success(BookWorkDto(description = null))
        }
    }

    @Before
    fun setup() {
        mapper = BookshelfExportMapper(testIdGenerator, mockRemoteDataSource)
        serializer = JsonBookshelfSerializer(mapper)
    }

    @Test
    fun serializeEmptyShelfSucceeds() = runTest {
        // Given - Empty shelf
        val shelf = Bookshelf(
            id = "shelf-1",
            name = "Empty Shelf",
            books = emptyList(),
            shelfStyle = ShelfStyle.SilverMetal,
            position = 0
        )

        // When - Serialize
        val result = serializer.serialize(shelf)

        // Then - Should succeed with valid JSON
        assertTrue("Serialize should succeed", result is Result.Success)
        val jsonString = (result as Result.Success).data
        assertTrue("Should contain shelf name", jsonString.contains("Empty Shelf"))
        assertTrue("Should contain shelf style", jsonString.contains("SilverMetal"))
    }

    @Test
    fun deserializeInvalidJsonReturnsError() = runTest {
        // Given - Invalid JSON
        val invalidJson = "{invalid json}"

        // When - Deserialize
        val result = serializer.deserialize(invalidJson)

        // Then - Should return error
        assertTrue("Deserialize should fail", result is Result.Error)
    }

    @Test
    fun personalMetadataNotInExportedJson() = runTest {
        // Given - Book with personal metadata populated
        val book = Book(
            id = "OL123W",
            title = "Privacy Test Book",
            authors = listOf("Test Author"),
            imageUrl = "https://example.com/cover.jpg",
            description = "Test description",
            languages = listOf("en"),
            firstPublishYear = "2024",
            averageRating = 4.5,
            ratingCount = 100,
            numPages = 300,
            numEditions = 5,
            purchased = false,
            spineColor = 0xFF8B4513.toInt(),
            // Personal metadata (should NOT be exported)
            readingStatus = ReadingStatus.CURRENTLY_READING,
            personalRating = 4.5f,
            personalNotes = "Private notes that should never be shared",
            dateAdded = 1234567890L,
            purchaseDate = 9876543210L,
            // Enhanced metadata (should be exported)
            isbn = "978-1234567890",
            publisher = "Test Publisher",
            publishDate = "2024-01-15",
            internetArchiveId = "test-archive-id"
        )

        val shelf = Bookshelf(
            id = "shelf-1",
            name = "Privacy Test Shelf",
            books = listOf(book),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )

        // When - Serialize to JSON
        val result = serializer.serialize(shelf)
        assertTrue("Serialize should succeed", result is Result.Success)
        val jsonString = (result as Result.Success).data

        // Then - JSON should NOT contain personal metadata field names
        assertFalse("JSON should not contain readingStatus", jsonString.contains("readingStatus"))
        assertFalse("JSON should not contain CURRENTLY_READING", jsonString.contains("CURRENTLY_READING"))
        assertFalse("JSON should not contain personalRating", jsonString.contains("personalRating"))
        assertFalse("JSON should not contain personalNotes", jsonString.contains("personalNotes"))
        assertFalse("JSON should not contain private notes text", jsonString.contains("Private notes that should never be shared"))
        assertFalse("JSON should not contain dateAdded", jsonString.contains("dateAdded"))
        assertFalse("JSON should not contain purchaseDate", jsonString.contains("purchaseDate"))

        // JSON should only contain work ID, not full book data
        assertTrue("JSON should contain work ID", jsonString.contains("OL123W"))
        assertTrue("JSON should contain bookIds field", jsonString.contains("bookIds"))

        // Should NOT contain book details (those are fetched on import)
        assertFalse("JSON should not contain full title", jsonString.contains("Privacy Test Book"))
        assertFalse("JSON should not contain author", jsonString.contains("Test Author"))
    }
}
