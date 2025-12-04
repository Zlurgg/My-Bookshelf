package uk.co.zlurgg.mybookshelf.sync.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfEntity
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto

/**
 * Unit tests for BookshelfFirestoreMapper extension functions.
 */
class BookshelfFirestoreMapperTest {

    @Test
    fun `toFirestoreDto maps all fields correctly`() {
        // Given
        val entity = createTestBookshelfEntity()
        val bookIds = listOf("book-1", "book-2", "book-3")

        // When
        val dto = entity.toFirestoreDto(bookIds)

        // Then
        assertEquals("shelf-1", dto.id)
        assertEquals("Test Shelf", dto.name)
        assertEquals("DARK_WOOD", dto.shelfMaterial)
        assertEquals(2, dto.position)
        assertEquals(true, dto.isTidyMode)
        assertEquals(listOf("book-1", "book-2", "book-3"), dto.bookIds)
        assertEquals(true, dto.isShared)
        assertEquals("ABC123", dto.shareCode)
        assertEquals(5L, dto.version)
        assertEquals(10000L, dto.lastModifiedAt)
    }

    @Test
    fun `toEntity maps all fields correctly`() {
        // Given
        val dto = createTestBookshelfFirestoreDto()
        val ownerId = "user-123"

        // When
        val entity = dto.toEntity(ownerId)

        // Then
        assertEquals("shelf-1", entity.id)
        assertEquals("Test Shelf", entity.name)
        assertEquals("DARK_WOOD", entity.shelfMaterial)
        assertEquals(2, entity.position)
        assertEquals(true, entity.isTidyMode)
        assertEquals("user-123", entity.ownerId)
        assertEquals("SYNCED", entity.syncStatus)
        assertEquals("shelf-1", entity.cloudId)
        assertEquals(5L, entity.version)
        assertEquals(10000L, entity.lastModifiedAt)
        assertEquals(true, entity.isShared)
        assertEquals("ABC123", entity.shareCode)
    }

    @Test
    fun `toEntity uses custom cloudId when provided`() {
        // Given
        val dto = createTestBookshelfFirestoreDto()

        // When
        val entity = dto.toEntity(ownerId = "user-123", cloudId = "custom-cloud-id")

        // Then
        assertEquals("custom-cloud-id", entity.cloudId)
    }

    @Test
    fun `roundtrip preserves all data`() {
        // Given
        val original = createTestBookshelfEntity()
        val bookIds = listOf("book-1", "book-2")

        // When
        val dto = original.toFirestoreDto(bookIds)
        val restored = dto.toEntity(ownerId = "user-123", cloudId = original.cloudId ?: original.id)

        // Then - Compare relevant fields (syncStatus changes to SYNCED)
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.shelfMaterial, restored.shelfMaterial)
        assertEquals(original.position, restored.position)
        assertEquals(original.isTidyMode, restored.isTidyMode)
        assertEquals(original.version, restored.version)
        assertEquals(original.lastModifiedAt, restored.lastModifiedAt)
        assertEquals(original.isShared, restored.isShared)
        assertEquals(original.shareCode, restored.shareCode)
    }

    @Test
    fun `toFirestoreDto with empty bookIds`() {
        // Given
        val entity = createTestBookshelfEntity()

        // When
        val dto = entity.toFirestoreDto(emptyList())

        // Then
        assertEquals(emptyList<String>(), dto.bookIds)
    }

    @Test
    fun `toEntity handles non-shared shelf`() {
        // Given
        val dto = BookshelfFirestoreDto(
            id = "shelf-private",
            name = "Private Shelf",
            shelfMaterial = "LIGHT_WOOD",
            position = 0,
            isTidyMode = false,
            bookIds = listOf("book-1"),
            isShared = false,
            shareCode = null,
            version = 1L,
            lastModifiedAt = 0L
        )

        // When
        val entity = dto.toEntity("user-123")

        // Then
        assertEquals(false, entity.isShared)
        assertNull(entity.shareCode)
    }

    @Test
    fun `toBookshelfFirestoreDto from map parses correctly`() {
        // Given
        val map = mapOf<String, Any?>(
            "name" to "Map Shelf",
            "shelf_material" to "SILVER_METAL",
            "position" to 5,
            "is_tidy_mode" to true,
            "book_ids" to listOf("b1", "b2"),
            "is_shared" to true,
            "share_code" to "XYZ789",
            "version" to 3L,
            "last_modified_at" to 5555L
        )

        // When
        val dto = map.toBookshelfFirestoreDto("map-shelf-id")

        // Then
        assertEquals("map-shelf-id", dto.id)
        assertEquals("Map Shelf", dto.name)
        assertEquals("SILVER_METAL", dto.shelfMaterial)
        assertEquals(5, dto.position)
        assertEquals(true, dto.isTidyMode)
        assertEquals(listOf("b1", "b2"), dto.bookIds)
        assertEquals(true, dto.isShared)
        assertEquals("XYZ789", dto.shareCode)
        assertEquals(3L, dto.version)
        assertEquals(5555L, dto.lastModifiedAt)
    }

    @Test
    fun `toBookshelfFirestoreDto from map handles missing fields with defaults`() {
        // Given
        val emptyMap = emptyMap<String, Any?>()

        // When
        val dto = emptyMap.toBookshelfFirestoreDto("empty-shelf-id")

        // Then
        assertEquals("empty-shelf-id", dto.id)
        assertEquals("", dto.name)
        assertEquals("DARK_WOOD", dto.shelfMaterial)
        assertEquals(0, dto.position)
        assertEquals(false, dto.isTidyMode)
        assertEquals(emptyList<String>(), dto.bookIds)
        assertEquals(false, dto.isShared)
        assertNull(dto.shareCode)
        assertEquals(1L, dto.version)
        assertEquals(0L, dto.lastModifiedAt)
    }

    // Helper functions

    private fun createTestBookshelfEntity() = BookshelfEntity(
        id = "shelf-1",
        name = "Test Shelf",
        shelfMaterial = "DARK_WOOD",
        position = 2,
        isTidyMode = true,
        ownerId = "owner-1",
        lastModifiedAt = 10000L,
        syncStatus = "PENDING",
        cloudId = "cloud-shelf-1",
        version = 5L,
        isShared = true,
        shareCode = "ABC123"
    )

    private fun createTestBookshelfFirestoreDto() = BookshelfFirestoreDto(
        id = "shelf-1",
        name = "Test Shelf",
        shelfMaterial = "DARK_WOOD",
        position = 2,
        isTidyMode = true,
        bookIds = listOf("book-1", "book-2"),
        isShared = true,
        shareCode = "ABC123",
        version = 5L,
        lastModifiedAt = 10000L
    )
}
