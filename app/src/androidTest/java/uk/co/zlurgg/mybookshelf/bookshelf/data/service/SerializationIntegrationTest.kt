package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

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

    private val testTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1000L
    }

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-id-${counter++}"
    }

    @Before
    fun setup() {
        mapper = BookshelfExportMapper(testTimeProvider, testIdGenerator)
        serializer = JsonBookshelfSerializer(mapper)
    }

    @Test
    fun serializeDeserializeRoundTrip() = runTest {
        // Given - Bookshelf with books
        val book = Book(
            id = "book-1",
            title = "Test Book",
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
            readingStatus = ReadingStatus.WANT_TO_READ,
            personalRating = null,
            personalNotes = null,
            dateAdded = null,
            purchaseDate = null,
            isbn = null,
            publisher = null,
            publishDate = null,
            internetArchiveId = null
        )

        val shelf = Bookshelf(
            id = "shelf-1",
            name = "Fiction",
            books = listOf(book),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )

        // When - Serialize
        val serializeResult = serializer.serialize(shelf)
        assertTrue("Serialize should succeed", serializeResult is Result.Success)
        val jsonString = (serializeResult as Result.Success).data

        // Then - JSON should be valid and contain expected data
        assertTrue("Should contain shelf name", jsonString.contains("Fiction"))
        assertTrue("Should contain book title", jsonString.contains("Test Book"))

        // And - Deserialize should reconstruct data
        val deserializeResult = serializer.deserialize(jsonString)
        assertTrue("Deserialize should succeed", deserializeResult is Result.Success)

        val exportData = (deserializeResult as Result.Success).data
        assertEquals("Fiction", exportData.bookshelf.name)
        assertEquals(ShelfStyle.DarkWood, exportData.bookshelf.shelfStyle)
        assertEquals(1, exportData.bookshelf.books.size)
        assertEquals("book-1", exportData.bookshelf.books[0].id)
        assertEquals("Test Book", exportData.bookshelf.books[0].title)
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
    fun serializeMultipleBooksPreservesAllData() = runTest {
        // Given - Shelf with multiple books
        val book1 = createTestBook("book-1", "Book One", listOf("Author A"))
        val book2 = createTestBook("book-2", "Book Two", listOf("Author B", "Author C"))
        val book3 = createTestBook("book-3", "Book Three", listOf("Author D"))

        val shelf = Bookshelf(
            id = "shelf-1",
            name = "Multi-Book Shelf",
            books = listOf(book1, book2, book3),
            shelfStyle = ShelfStyle.WhiteMetal,
            position = 0
        )

        // When - Serialize and deserialize
        val jsonString = (serializer.serialize(shelf) as Result.Success).data
        val exportData = (serializer.deserialize(jsonString) as Result.Success).data

        // Then - All books preserved
        assertEquals(3, exportData.bookshelf.books.size)
        assertEquals("Book One", exportData.bookshelf.books[0].title)
        assertEquals("Book Two", exportData.bookshelf.books[1].title)
        assertEquals("Book Three", exportData.bookshelf.books[2].title)

        // Multi-author preserved
        assertEquals(2, exportData.bookshelf.books[1].authors.size)
        assertEquals("Author B", exportData.bookshelf.books[1].authors[0])
        assertEquals("Author C", exportData.bookshelf.books[1].authors[1])
    }

    @Test
    fun serializePreservesBookMetadata() = runTest {
        // Given - Book with rich metadata
        val book = Book(
            id = "book-1",
            title = "Metadata Book",
            authors = listOf("Author A", "Author B"),
            imageUrl = "https://example.com/cover.jpg",
            description = "Long description with special characters: <>&\"",
            languages = listOf("en", "fr", "de"),
            firstPublishYear = "1999",
            averageRating = 4.7,
            ratingCount = 9999,
            numPages = 456,
            numEditions = 12,
            purchased = true,
            spineColor = 0xFF123456.toInt(),
            readingStatus = ReadingStatus.WANT_TO_READ,
            personalRating = null,
            personalNotes = null,
            dateAdded = null,
            purchaseDate = null,
            isbn = null,
            publisher = null,
            publishDate = null,
            internetArchiveId = null
        )

        val shelf = Bookshelf(
            id = "shelf-1",
            name = "Metadata Shelf",
            books = listOf(book),
            shelfStyle = ShelfStyle.DarkGreyMetal,
            position = 0
        )

        // When - Serialize and deserialize
        val jsonString = (serializer.serialize(shelf) as Result.Success).data
        val exportData = (serializer.deserialize(jsonString) as Result.Success).data

        // Then - All metadata preserved
        val exportedBook = exportData.bookshelf.books[0]
        assertEquals("Metadata Book", exportedBook.title)
        assertEquals(2, exportedBook.authors.size)
        assertEquals("Long description with special characters: <>&\"", exportedBook.description)
        assertEquals(3, exportedBook.languages.size)
        assertEquals("1999", exportedBook.firstPublishYear)
        assertEquals(4.7, exportedBook.averageRating!!, 0.01)
        assertEquals(9999, exportedBook.ratingCount)
        assertEquals(456, exportedBook.numPages)
        assertEquals(12, exportedBook.numEditions)
        assertEquals(true, exportedBook.purchased)
    }

    @Test
    fun serializeHandlesSpecialCharacters() = runTest {
        // Given - Shelf with special characters in name and description
        val book = Book(
            id = "book-1",
            title = "Book with \"quotes\" and <tags>",
            authors = listOf("Author & Co."),
            imageUrl = "https://example.com/cover.jpg",
            description = "Description with \n newlines \t tabs and unicode: 日本語",
            languages = listOf("en"),
            firstPublishYear = "2024",
            averageRating = null,
            ratingCount = null,
            numPages = null,
            numEditions = 0,
            purchased = false,
            spineColor = 0,
            readingStatus = ReadingStatus.WANT_TO_READ,
            personalRating = null,
            personalNotes = null,
            dateAdded = null,
            purchaseDate = null,
            isbn = null,
            publisher = null,
            publishDate = null,
            internetArchiveId = null
        )

        val shelf = Bookshelf(
            id = "shelf-1",
            name = "Special & \"Characters\" <Shelf>",
            books = listOf(book),
            shelfStyle = ShelfStyle.GreyMetal,
            position = 0
        )

        // When - Serialize and deserialize
        val jsonString = (serializer.serialize(shelf) as Result.Success).data
        val exportData = (serializer.deserialize(jsonString) as Result.Success).data

        // Then - Special characters preserved correctly
        assertEquals("Special & \"Characters\" <Shelf>", exportData.bookshelf.name)
        assertEquals("Book with \"quotes\" and <tags>", exportData.bookshelf.books[0].title)
        assertEquals("Author & Co.", exportData.bookshelf.books[0].authors[0])
        assertTrue(exportData.bookshelf.books[0].description!!.contains("日本語"))
    }

    @Test
    fun personalMetadataNotInExportedJson() = runTest {
        // Given - Book with personal metadata populated
        val book = Book(
            id = "book-1",
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

        // But SHOULD contain enhanced metadata
        assertTrue("JSON should contain isbn", jsonString.contains("isbn"))
        assertTrue("JSON should contain 978-1234567890", jsonString.contains("978-1234567890"))
        assertTrue("JSON should contain publisher", jsonString.contains("publisher"))
        assertTrue("JSON should contain Test Publisher", jsonString.contains("Test Publisher"))
        assertTrue("JSON should contain internetArchiveId", jsonString.contains("internetArchiveId"))
    }

    @Test
    fun deserializedBookExcludesPersonalMetadata() = runTest {
        // Given - Book with both personal metadata and enhanced metadata
        val book = Book(
            id = "book-1",
            title = "Full Metadata Book",
            authors = listOf("Test Author"),
            imageUrl = "https://example.com/cover.jpg",
            description = "Test description",
            languages = listOf("en"),
            firstPublishYear = "2024",
            averageRating = 4.5,
            ratingCount = 100,
            numPages = 300,
            numEditions = 5,
            purchased = true,
            spineColor = 0xFF8B4513.toInt(),
            // Personal metadata (should NOT survive round-trip)
            readingStatus = ReadingStatus.READ,
            personalRating = 5.0f,
            personalNotes = "Excellent book, highly recommend!",
            dateAdded = 1111111111L,
            purchaseDate = 2222222222L,
            // Enhanced metadata (SHOULD survive round-trip)
            isbn = "978-9876543210",
            publisher = "Privacy Publisher",
            publishDate = "2024-02-20",
            internetArchiveId = "privacy-archive-id"
        )

        val shelf = Bookshelf(
            id = "shelf-1",
            name = "Full Metadata Shelf",
            books = listOf(book),
            shelfStyle = ShelfStyle.WhiteMetal,
            position = 0
        )

        // When - Serialize and deserialize
        val jsonString = (serializer.serialize(shelf) as Result.Success).data
        val exportData = (serializer.deserialize(jsonString) as Result.Success).data

        // Then - Deserialized ExportedBook should not have personal metadata fields
        // When we convert ExportedBook -> Book via mapper, personal metadata gets defaults
        val exportedBookshelf = exportData.bookshelf
        val reconvertedBook = uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookMapper.fromExportedBook(exportedBookshelf.books[0])

        // Personal metadata should be reset to defaults (privacy protection)
        assertNull("Personal rating should be null", reconvertedBook.personalRating)
        assertNull("Personal notes should be null", reconvertedBook.personalNotes)
        assertNull("Date added should be null", reconvertedBook.dateAdded)
        assertNull("Purchase date should be null", reconvertedBook.purchaseDate)
        assertEquals("Reading status should be default", ReadingStatus.WANT_TO_READ, reconvertedBook.readingStatus)

        // Enhanced metadata SHOULD be preserved
        assertEquals("ISBN should be preserved", "978-9876543210", reconvertedBook.isbn)
        assertEquals("Publisher should be preserved", "Privacy Publisher", reconvertedBook.publisher)
        assertEquals("Publish date should be preserved", "2024-02-20", reconvertedBook.publishDate)
        assertEquals("Internet Archive ID should be preserved", "privacy-archive-id", reconvertedBook.internetArchiveId)

        // Regular book metadata SHOULD be preserved
        assertEquals("Title should be preserved", "Full Metadata Book", reconvertedBook.title)
        assertEquals("Authors should be preserved", "Test Author", reconvertedBook.authors[0])
        assertEquals("Purchased flag should be preserved", true, reconvertedBook.purchased)
    }

    private fun createTestBook(
        id: String,
        title: String,
        authors: List<String> = listOf("Test Author")
    ): Book {
        return Book(
            id = id,
            title = title,
            authors = authors,
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
            // New fields with default values
            readingStatus = ReadingStatus.WANT_TO_READ,
            personalRating = null,
            personalNotes = null,
            dateAdded = null,
            purchaseDate = null,
            isbn = null,
            publisher = null,
            publishDate = null,
            internetArchiveId = null
        )
    }
}
