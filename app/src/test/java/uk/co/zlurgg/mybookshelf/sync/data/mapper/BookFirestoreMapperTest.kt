package uk.co.zlurgg.mybookshelf.sync.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto

/**
 * Unit tests for BookFirestoreMapper extension functions.
 */
class BookFirestoreMapperTest {

    @Test
    fun `toFirestoreDto maps all fields correctly`() {
        // Given
        val entity = createTestBookEntity()

        // When
        val dto = entity.toFirestoreDto()

        // Then
        assertEquals("book-1", dto.id)
        assertEquals("Test Book", dto.title)
        assertEquals(listOf("Author 1", "Author 2"), dto.authors)
        assertEquals("https://example.com/cover.jpg", dto.imageUrl)
        assertEquals("A test description", dto.description)
        assertEquals(listOf("en", "fr"), dto.languages)
        assertEquals("2020", dto.firstPublishYear)
        assertEquals(4.5, dto.averageRating)
        assertEquals(100, dto.ratingCount)
        assertEquals(300, dto.numPages)
        assertEquals(5, dto.numEditions)
        assertEquals(true, dto.purchased)
        assertEquals(-16711936, dto.spineColor)
        assertEquals("CURRENTLY_READING", dto.readingStatus)
        assertEquals(4.0f, dto.personalRating)
        assertEquals("Great book!", dto.personalNotes)
        assertEquals(1000L, dto.dateAdded)
        assertEquals(2000L, dto.purchaseDate)
        assertEquals("978-0123456789", dto.isbn)
        assertEquals("Test Publisher", dto.publisher)
        assertEquals("2020-01-01", dto.publishDate)
        assertEquals("ia-12345", dto.internetArchiveId)
        assertEquals(3L, dto.version)
        assertEquals(5000L, dto.lastModifiedAt)
    }

    @Test
    fun `toEntity maps all fields correctly`() {
        // Given
        val dto = createTestBookFirestoreDto()
        val ownerId = "user-123"

        // When
        val entity = dto.toEntity(ownerId)

        // Then
        assertEquals("book-1", entity.id)
        assertEquals("Test Book", entity.title)
        assertEquals(listOf("Author 1", "Author 2"), entity.authors)
        assertEquals("https://example.com/cover.jpg", entity.imageUrl)
        assertEquals("A test description", entity.description)
        assertEquals(listOf("en", "fr"), entity.languages)
        assertEquals("2020", entity.firstPublishYear)
        assertEquals(4.5, entity.ratingsAverage)
        assertEquals(100, entity.ratingsCount)
        assertEquals(300, entity.numPagesMedian)
        assertEquals(5, entity.numEditions)
        assertEquals(true, entity.purchased)
        assertEquals(-16711936, entity.spineColor)
        assertEquals("CURRENTLY_READING", entity.readingStatus)
        assertEquals(4.0f, entity.personalRating)
        assertEquals("Great book!", entity.personalNotes)
        assertEquals(1000L, entity.dateAdded)
        assertEquals(2000L, entity.purchaseDate)
        assertEquals("978-0123456789", entity.isbn)
        assertEquals("Test Publisher", entity.publisher)
        assertEquals("2020-01-01", entity.publishDate)
        assertEquals("ia-12345", entity.internetArchiveId)
        assertEquals("user-123", entity.ownerId)
        assertEquals("SYNCED", entity.syncStatus)
        assertEquals("book-1", entity.cloudId)
        assertEquals(3L, entity.version)
        assertEquals(5000L, entity.lastModifiedAt)
    }

    @Test
    fun `toEntity uses custom cloudId when provided`() {
        // Given
        val dto = createTestBookFirestoreDto()

        // When
        val entity = dto.toEntity(ownerId = "user-123", cloudId = "custom-cloud-id")

        // Then
        assertEquals("custom-cloud-id", entity.cloudId)
    }

    @Test
    fun `roundtrip preserves all data`() {
        // Given
        val original = createTestBookEntity()

        // When
        val dto = original.toFirestoreDto()
        val restored = dto.toEntity(ownerId = "user-123", cloudId = original.cloudId ?: original.id)

        // Then - Compare relevant fields (syncStatus changes to SYNCED)
        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.authors, restored.authors)
        assertEquals(original.imageUrl, restored.imageUrl)
        assertEquals(original.description, restored.description)
        assertEquals(original.languages, restored.languages)
        assertEquals(original.firstPublishYear, restored.firstPublishYear)
        assertEquals(original.ratingsAverage, restored.ratingsAverage)
        assertEquals(original.ratingsCount, restored.ratingsCount)
        assertEquals(original.numPagesMedian, restored.numPagesMedian)
        assertEquals(original.numEditions, restored.numEditions)
        assertEquals(original.purchased, restored.purchased)
        assertEquals(original.spineColor, restored.spineColor)
        assertEquals(original.readingStatus, restored.readingStatus)
        assertEquals(original.personalRating, restored.personalRating)
        assertEquals(original.personalNotes, restored.personalNotes)
        assertEquals(original.version, restored.version)
        assertEquals(original.lastModifiedAt, restored.lastModifiedAt)
    }

    @Test
    fun `toFirestoreDto handles null optional fields`() {
        // Given
        val entity = BookEntity(
            id = "book-minimal",
            title = "Minimal Book",
            description = null,
            imageUrl = "url",
            languages = emptyList(),
            authors = emptyList(),
            firstPublishYear = null,
            ratingsAverage = null,
            ratingsCount = null,
            numPagesMedian = null,
            numEditions = 0,
            purchased = false,
            spineColor = 0
        )

        // When
        val dto = entity.toFirestoreDto()

        // Then
        assertEquals("book-minimal", dto.id)
        assertNull(dto.description)
        assertNull(dto.firstPublishYear)
        assertNull(dto.averageRating)
        assertNull(dto.ratingCount)
        assertNull(dto.numPages)
        assertEquals(emptyList<String>(), dto.authors)
        assertEquals(emptyList<String>(), dto.languages)
    }

    @Test
    fun `toBookFirestoreDto from map parses correctly`() {
        // Given
        val map = mapOf<String, Any?>(
            "title" to "Map Book",
            "authors" to listOf("Map Author"),
            "image_url" to "https://example.com/map.jpg",
            "description" to "From map",
            "languages" to listOf("en"),
            "first_publish_year" to "2021",
            "average_rating" to 4.0,
            "rating_count" to 50,
            "num_pages" to 200,
            "num_editions" to 2,
            "purchased" to true,
            "spine_color" to 12345,
            "reading_status" to "FINISHED",
            "personal_rating" to 5.0f,
            "personal_notes" to "Excellent",
            "version" to 2L,
            "last_modified_at" to 9999L
        )

        // When
        val dto = map.toBookFirestoreDto("map-book-id")

        // Then
        assertEquals("map-book-id", dto.id)
        assertEquals("Map Book", dto.title)
        assertEquals(listOf("Map Author"), dto.authors)
        assertEquals("https://example.com/map.jpg", dto.imageUrl)
        assertEquals("From map", dto.description)
        assertEquals(listOf("en"), dto.languages)
        assertEquals("2021", dto.firstPublishYear)
        assertEquals(4.0, dto.averageRating)
        assertEquals(50, dto.ratingCount)
        assertEquals(200, dto.numPages)
        assertEquals(2, dto.numEditions)
        assertEquals(true, dto.purchased)
        assertEquals(12345, dto.spineColor)
        assertEquals("FINISHED", dto.readingStatus)
        assertEquals(2L, dto.version)
        assertEquals(9999L, dto.lastModifiedAt)
    }

    @Test
    fun `toBookFirestoreDto from map handles missing fields with defaults`() {
        // Given
        val emptyMap = emptyMap<String, Any?>()

        // When
        val dto = emptyMap.toBookFirestoreDto("empty-id")

        // Then
        assertEquals("empty-id", dto.id)
        assertEquals("", dto.title)
        assertEquals(emptyList<String>(), dto.authors)
        assertEquals("", dto.imageUrl)
        assertNull(dto.description)
        assertEquals(emptyList<String>(), dto.languages)
        assertEquals(0, dto.numEditions)
        assertEquals(false, dto.purchased)
        assertEquals(0, dto.spineColor)
        assertEquals("WANT_TO_READ", dto.readingStatus)
        assertEquals(0f, dto.personalRating)
        assertEquals("", dto.personalNotes)
        assertEquals(1L, dto.version)
        assertEquals(0L, dto.lastModifiedAt)
    }

    // Helper functions

    private fun createTestBookEntity() = BookEntity(
        id = "book-1",
        title = "Test Book",
        description = "A test description",
        imageUrl = "https://example.com/cover.jpg",
        languages = listOf("en", "fr"),
        authors = listOf("Author 1", "Author 2"),
        firstPublishYear = "2020",
        ratingsAverage = 4.5,
        ratingsCount = 100,
        numPagesMedian = 300,
        numEditions = 5,
        purchased = true,
        spineColor = -16711936,
        readingStatus = "CURRENTLY_READING",
        personalRating = 4.0f,
        personalNotes = "Great book!",
        dateAdded = 1000L,
        purchaseDate = 2000L,
        isbn = "978-0123456789",
        publisher = "Test Publisher",
        publishDate = "2020-01-01",
        internetArchiveId = "ia-12345",
        ownerId = "owner-1",
        lastModifiedAt = 5000L,
        syncStatus = "PENDING",
        cloudId = "cloud-1",
        version = 3L
    )

    private fun createTestBookFirestoreDto() = BookFirestoreDto(
        id = "book-1",
        title = "Test Book",
        authors = listOf("Author 1", "Author 2"),
        imageUrl = "https://example.com/cover.jpg",
        description = "A test description",
        languages = listOf("en", "fr"),
        firstPublishYear = "2020",
        averageRating = 4.5,
        ratingCount = 100,
        numPages = 300,
        numEditions = 5,
        purchased = true,
        spineColor = -16711936,
        readingStatus = "CURRENTLY_READING",
        personalRating = 4.0f,
        personalNotes = "Great book!",
        dateAdded = 1000L,
        purchaseDate = 2000L,
        isbn = "978-0123456789",
        publisher = "Test Publisher",
        publishDate = "2020-01-01",
        internetArchiveId = "ia-12345",
        version = 3L,
        lastModifiedAt = 5000L
    )
}
