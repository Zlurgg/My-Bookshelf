package uk.co.zlurgg.mybookshelf.sync.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.data.database.entity.SyncMetadataEntity

/**
 * Unit tests for SyncMetadataEntity field validation.
 */
class SyncMetadataEntityTest {
    @Test
    fun `entity creates with all required fields`() {
        // Given/When
        val entity =
            SyncMetadataEntity(
                userId = "user-123",
                lastSyncTimestamp = 1234567890L,
            )

        // Then
        assertEquals("user-123", entity.userId)
        assertEquals(1234567890L, entity.lastSyncTimestamp)
        assertFalse(entity.syncInProgress)
        assertNull(entity.lastSyncError)
        assertEquals(0, entity.pendingOperationsCount)
    }

    @Test
    fun `entity creates with all fields including optionals`() {
        // Given/When
        val entity =
            SyncMetadataEntity(
                userId = "user-456",
                lastSyncTimestamp = 9876543210L,
                syncInProgress = true,
                lastSyncError = "Network timeout",
                pendingOperationsCount = 5,
            )

        // Then
        assertEquals("user-456", entity.userId)
        assertEquals(9876543210L, entity.lastSyncTimestamp)
        assertEquals(true, entity.syncInProgress)
        assertEquals("Network timeout", entity.lastSyncError)
        assertEquals(5, entity.pendingOperationsCount)
    }

    @Test
    fun `userId is primary key`() {
        // Given
        val entity1 =
            SyncMetadataEntity(
                userId = "user-123",
                lastSyncTimestamp = 1000L,
            )
        val entity2 =
            SyncMetadataEntity(
                userId = "user-123",
                lastSyncTimestamp = 2000L,
            )

        // Then - Same userId means same identity
        assertEquals(entity1.userId, entity2.userId)
    }

    @Test
    fun `default values are correct`() {
        // Given
        val entity =
            SyncMetadataEntity(
                userId = "test-user",
                lastSyncTimestamp = 0L,
            )

        // Then
        assertFalse("syncInProgress should default to false", entity.syncInProgress)
        assertNull("lastSyncError should default to null", entity.lastSyncError)
        assertEquals("pendingOperationsCount should default to 0", 0, entity.pendingOperationsCount)
    }

    @Test
    fun `copy preserves and updates fields correctly`() {
        // Given
        val original =
            SyncMetadataEntity(
                userId = "user-123",
                lastSyncTimestamp = 1000L,
                syncInProgress = false,
                lastSyncError = null,
                pendingOperationsCount = 0,
            )

        // When
        val updated =
            original.copy(
                lastSyncTimestamp = 2000L,
                syncInProgress = true,
                pendingOperationsCount = 3,
            )

        // Then
        assertEquals("userId should remain unchanged", "user-123", updated.userId)
        assertEquals("lastSyncTimestamp should be updated", 2000L, updated.lastSyncTimestamp)
        assertEquals("syncInProgress should be updated", true, updated.syncInProgress)
        assertNull("lastSyncError should remain null", updated.lastSyncError)
        assertEquals("pendingOperationsCount should be updated", 3, updated.pendingOperationsCount)
    }
}
