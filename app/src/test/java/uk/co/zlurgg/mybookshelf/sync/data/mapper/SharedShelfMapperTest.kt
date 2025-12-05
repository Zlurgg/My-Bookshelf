package uk.co.zlurgg.mybookshelf.sync.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import java.util.Date

/**
 * Unit tests for SharedShelfMapper extension functions.
 */
class SharedShelfMapperTest {

    @Test
    fun `toSharedShelfDto maps all fields correctly`() {
        // Given
        val entity = createTestBookshelfEntity()
        val ownerId = "user-123"
        val bookCount = 5

        // When
        val dto = entity.toSharedShelfDto(ownerId, bookCount)

        // Then
        assertEquals("ABC123", dto.shareCode)
        assertEquals("user-123", dto.ownerId)
        assertEquals("shelf-1", dto.shelfId)
        assertEquals("Test Shelf", dto.shelfName)
        assertEquals(emptyList<String>(), dto.subscriberIds)
        assertNotNull(dto.createdAt)
        assertEquals(5, dto.bookCount)
    }

    @Test
    fun `toSharedShelfDto throws when shareCode is null`() {
        // Given
        val entity = BookshelfEntity(
            id = "shelf-no-share",
            name = "Not Shared",
            shelfMaterial = "DARK_WOOD",
            position = 0,
            isShared = false,
            shareCode = null
        )

        // When/Then
        assertThrows(IllegalArgumentException::class.java) {
            entity.toSharedShelfDto("user-123", 0)
        }
    }

    @Test
    fun `toSharedShelfDto with zero bookCount`() {
        // Given
        val entity = createTestBookshelfEntity()

        // When
        val dto = entity.toSharedShelfDto("user-123", 0)

        // Then
        assertEquals(0, dto.bookCount)
    }

    @Test
    fun `toSharedShelfDto from map parses correctly`() {
        // Given
        val createdDate = Date()
        val map = mapOf<String, Any?>(
            "owner_id" to "owner-456",
            "shelf_id" to "shelf-xyz",
            "shelf_name" to "Shared Books",
            "subscriber_ids" to listOf("sub-1", "sub-2"),
            "created_at" to createdDate,
            "book_count" to 10
        )

        // When
        val dto = map.toSharedShelfDto("share-code-123")

        // Then
        assertEquals("share-code-123", dto.shareCode)
        assertEquals("owner-456", dto.ownerId)
        assertEquals("shelf-xyz", dto.shelfId)
        assertEquals("Shared Books", dto.shelfName)
        assertEquals(listOf("sub-1", "sub-2"), dto.subscriberIds)
        assertEquals(createdDate, dto.createdAt)
        assertEquals(10, dto.bookCount)
    }

    @Test
    fun `toSharedShelfDto from map handles missing fields with defaults`() {
        // Given
        val emptyMap = emptyMap<String, Any?>()

        // When
        val dto = emptyMap.toSharedShelfDto("empty-share-code")

        // Then
        assertEquals("empty-share-code", dto.shareCode)
        assertEquals("", dto.ownerId)
        assertEquals("", dto.shelfId)
        assertEquals("", dto.shelfName)
        assertEquals(emptyList<String>(), dto.subscriberIds)
        assertEquals(null, dto.createdAt)
        assertEquals(0, dto.bookCount)
    }

    @Test
    fun `toSharedShelfDto from map handles partial data`() {
        // Given
        val partialMap = mapOf<String, Any?>(
            "owner_id" to "partial-owner",
            "shelf_name" to "Partial Shelf"
            // missing other fields
        )

        // When
        val dto = partialMap.toSharedShelfDto("partial-code")

        // Then
        assertEquals("partial-code", dto.shareCode)
        assertEquals("partial-owner", dto.ownerId)
        assertEquals("", dto.shelfId) // default
        assertEquals("Partial Shelf", dto.shelfName)
        assertEquals(emptyList<String>(), dto.subscriberIds) // default
        assertEquals(0, dto.bookCount) // default
    }

    // Helper functions

    private fun createTestBookshelfEntity() = BookshelfEntity(
        id = "shelf-1",
        name = "Test Shelf",
        shelfMaterial = "DARK_WOOD",
        position = 0,
        isTidyMode = false,
        ownerId = "owner-1",
        lastModifiedAt = 0L,
        syncStatus = "SYNCED",
        cloudId = null,
        version = 1L,
        isShared = true,
        shareCode = "ABC123"
    )
}
