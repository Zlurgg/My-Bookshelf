package uk.co.zlurgg.mybookshelf.sync.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictResolution
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictStrategy
import uk.co.zlurgg.mybookshelf.sync.domain.model.EntityType
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncConflict

/**
 * Unit tests for DefaultConflictResolver.
 */
class DefaultConflictResolverTest {
    // ==================== Strategy: LAST_WRITE_WINS ====================

    @Test
    fun `resolve with LAST_WRITE_WINS returns KeepLocal when local is newer`() {
        // Given
        val resolver = DefaultConflictResolver.lastWriteWins()
        val conflict = createConflict(localTimestamp = 2000L, remoteTimestamp = 1000L)

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepLocal, resolution)
    }

    @Test
    fun `resolve with LAST_WRITE_WINS returns KeepRemote when remote is newer`() {
        // Given
        val resolver = DefaultConflictResolver.lastWriteWins()
        val conflict = createConflict(localTimestamp = 1000L, remoteTimestamp = 2000L)

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepRemote, resolution)
    }

    @Test
    fun `resolve with LAST_WRITE_WINS returns KeepLocal when timestamps are equal`() {
        // Given (local wins on tie)
        val resolver = DefaultConflictResolver.lastWriteWins()
        val conflict = createConflict(localTimestamp = 1000L, remoteTimestamp = 1000L)

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepLocal, resolution)
    }

    // ==================== Strategy: LOCAL_WINS ====================

    @Test
    fun `resolve with LOCAL_WINS always returns KeepLocal`() {
        // Given
        val resolver = DefaultConflictResolver.localWins()
        val conflict = createConflict(localTimestamp = 1000L, remoteTimestamp = 2000L)

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepLocal, resolution)
    }

    @Test
    fun `resolve with LOCAL_WINS returns KeepLocal even when remote is newer`() {
        // Given
        val resolver = DefaultConflictResolver.localWins()
        val conflict = createConflict(localTimestamp = 500L, remoteTimestamp = 10000L)

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepLocal, resolution)
    }

    // ==================== Strategy: REMOTE_WINS ====================

    @Test
    fun `resolve with REMOTE_WINS always returns KeepRemote`() {
        // Given
        val resolver = DefaultConflictResolver.remoteWins()
        val conflict = createConflict(localTimestamp = 2000L, remoteTimestamp = 1000L)

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepRemote, resolution)
    }

    @Test
    fun `resolve with REMOTE_WINS returns KeepRemote even when local is newer`() {
        // Given
        val resolver = DefaultConflictResolver.remoteWins()
        val conflict = createConflict(localTimestamp = 10000L, remoteTimestamp = 500L)

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepRemote, resolution)
    }

    // ==================== Strategy: ASK_USER ====================

    @Test
    fun `resolve with ASK_USER returns null`() {
        // Given
        val resolver = DefaultConflictResolver.askUser()
        val conflict = createConflict()

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertNull(resolution)
    }

    @Test
    fun `canAutoResolve returns false for ASK_USER`() {
        // Given
        val resolver = DefaultConflictResolver.askUser()
        val conflict = createConflict()

        // When
        val canResolve = resolver.canAutoResolve(conflict)

        // Then
        assertFalse(canResolve)
    }

    // ==================== canAutoResolve ====================

    @Test
    fun `canAutoResolve returns true for LAST_WRITE_WINS`() {
        // Given
        val resolver = DefaultConflictResolver.lastWriteWins()
        val conflict = createConflict()

        // When
        val canResolve = resolver.canAutoResolve(conflict)

        // Then
        assertTrue(canResolve)
    }

    @Test
    fun `canAutoResolve returns true for LOCAL_WINS`() {
        // Given
        val resolver = DefaultConflictResolver.localWins()
        val conflict = createConflict()

        // When
        val canResolve = resolver.canAutoResolve(conflict)

        // Then
        assertTrue(canResolve)
    }

    @Test
    fun `canAutoResolve returns true for REMOTE_WINS`() {
        // Given
        val resolver = DefaultConflictResolver.remoteWins()
        val conflict = createConflict()

        // When
        val canResolve = resolver.canAutoResolve(conflict)

        // Then
        assertTrue(canResolve)
    }

    // ==================== Strategy property ====================

    @Test
    fun `strategy property returns current strategy`() {
        // Given
        val resolver = DefaultConflictResolver(ConflictStrategy.LOCAL_WINS)

        // When
        val strategy = resolver.strategy

        // Then
        assertEquals(ConflictStrategy.LOCAL_WINS, strategy)
    }

    @Test
    fun `default strategy is LAST_WRITE_WINS`() {
        // Given - new resolver (uses default)
        val resolver = DefaultConflictResolver()

        // When
        val strategy = resolver.strategy

        // Then
        assertEquals(ConflictStrategy.LAST_WRITE_WINS, strategy)
    }

    // ==================== Different Entity Types ====================

    @Test
    fun `resolve works for BOOK entity type`() {
        // Given
        val resolver = DefaultConflictResolver.lastWriteWins()
        val conflict =
            createConflict(
                entityType = EntityType.BOOK,
                localTimestamp = 2000L,
                remoteTimestamp = 1000L,
            )

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepLocal, resolution)
    }

    @Test
    fun `resolve works for BOOKSHELF entity type`() {
        // Given
        val resolver = DefaultConflictResolver.lastWriteWins()
        val conflict =
            createConflict(
                entityType = EntityType.BOOKSHELF,
                localTimestamp = 1000L,
                remoteTimestamp = 2000L,
            )

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepRemote, resolution)
    }

    @Test
    fun `resolve works for CROSS_REF entity type`() {
        // Given
        val resolver = DefaultConflictResolver.localWins()
        val conflict =
            createConflict(
                entityType = EntityType.CROSS_REF,
                localTimestamp = 1000L,
                remoteTimestamp = 2000L,
            )

        // When
        val resolution = resolver.resolve(conflict)

        // Then
        assertEquals(ConflictResolution.KeepLocal, resolution)
    }

    // ==================== Factory Methods ====================

    @Test
    fun `localWins factory creates resolver with LOCAL_WINS strategy`() {
        // Given/When
        val resolver = DefaultConflictResolver.localWins()

        // Then
        assertEquals(ConflictStrategy.LOCAL_WINS, resolver.strategy)
    }

    @Test
    fun `remoteWins factory creates resolver with REMOTE_WINS strategy`() {
        // Given/When
        val resolver = DefaultConflictResolver.remoteWins()

        // Then
        assertEquals(ConflictStrategy.REMOTE_WINS, resolver.strategy)
    }

    @Test
    fun `lastWriteWins factory creates resolver with LAST_WRITE_WINS strategy`() {
        // Given/When
        val resolver = DefaultConflictResolver.lastWriteWins()

        // Then
        assertEquals(ConflictStrategy.LAST_WRITE_WINS, resolver.strategy)
    }

    @Test
    fun `askUser factory creates resolver with ASK_USER strategy`() {
        // Given/When
        val resolver = DefaultConflictResolver.askUser()

        // Then
        assertEquals(ConflictStrategy.ASK_USER, resolver.strategy)
    }

    // ==================== Helper Functions ====================

    private fun createConflict(
        entityId: String = "test-entity-id",
        entityType: EntityType = EntityType.BOOK,
        localTimestamp: Long = 1000L,
        remoteTimestamp: Long = 2000L,
        localVersion: Long = 1L,
        remoteVersion: Long = 2L,
    ) = SyncConflict(
        entityId = entityId,
        entityType = entityType,
        localTimestamp = localTimestamp,
        remoteTimestamp = remoteTimestamp,
        localVersion = localVersion,
        remoteVersion = remoteVersion,
    )
}
