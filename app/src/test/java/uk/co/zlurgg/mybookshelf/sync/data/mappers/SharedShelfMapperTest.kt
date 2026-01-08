package uk.co.zlurgg.mybookshelf.sync.data.mappers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

/**
 * Unit tests for SharedShelfDto mapper extension functions.
 *
 * Tests the Map<String, Any?>.toSharedShelfDto() function used to parse
 * Firestore document snapshots into SharedShelfDto objects.
 */
class SharedShelfMapperTest {
    @Test
    fun `toSharedShelfDto from map parses correctly`() {
        // Given
        val createdDate = Date()
        val map =
            mapOf<String, Any?>(
                "owner_id" to "owner-456",
                "shelf_id" to "shelf-xyz",
                "shelf_name" to "Shared Books",
                "subscriber_ids" to listOf("sub-1", "sub-2"),
                "created_at" to createdDate,
                "book_count" to 10,
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
        val partialMap =
            mapOf<String, Any?>(
                "owner_id" to "partial-owner",
                "shelf_name" to "Partial Shelf",
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

    @Test
    fun `toSharedShelfDto from map handles number type variations`() {
        // Given - Firestore can return Long or Int for numbers
        val map =
            // book_count is Long instead of Int (Firestore returns Long)
            mapOf<String, Any?>(
                "owner_id" to "owner-123",
                "shelf_id" to "shelf-abc",
                "shelf_name" to "Test Shelf",
                "book_count" to 42L,
            )

        // When
        val dto = map.toSharedShelfDto("test-code")

        // Then
        assertEquals(42, dto.bookCount)
    }
}
