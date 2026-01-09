package uk.co.zlurgg.mybookshelf.bookshelf.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.SyncDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.SyncMetadataEntity

/**
 * DAO layer tests for sync-related queries.
 *
 * Tests use in-memory Room database with Robolectric for Android framework.
 * Focus on sync status queries, owner assignment, and sharing functionality.
 */
@RunWith(RobolectricTestRunner::class)
class BookshelfDaoSyncTest {

    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var dao: BookshelfDao
    private lateinit var syncDao: SyncDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MyBookshelfRoomDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.bookshelfDao
        syncDao = database.syncDao
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== Pending Sync Queries ==========

    @Test
    fun `getPendingSyncBooks returns books with non-SYNCED status`() = runTest {
        // Given - Books with different sync statuses
        dao.upsert(createTestBook("book-1", "Book 1", syncStatus = "PENDING"))
        dao.upsert(createTestBook("book-2", "Book 2", syncStatus = "SYNCED"))
        dao.upsert(createTestBook("book-3", "Book 3", syncStatus = "CONFLICT"))
        dao.upsert(createTestBook("book-4", "Book 4", syncStatus = "DELETED"))

        // When
        val pendingBooks = dao.getPendingSyncBooks()

        // Then - Should return 3 books (not SYNCED)
        assertEquals(3, pendingBooks.size)
        assertTrue(pendingBooks.any { it.id == "book-1" })
        assertTrue(pendingBooks.any { it.id == "book-3" })
        assertTrue(pendingBooks.any { it.id == "book-4" })
        assertTrue(pendingBooks.none { it.id == "book-2" })
    }

    @Test
    fun `getPendingSyncShelves returns shelves with non-SYNCED status`() = runTest {
        // Given - Shelves with different sync statuses
        dao.upsertShelf(createTestShelf("shelf-1", "Shelf 1", 0, syncStatus = "PENDING"))
        dao.upsertShelf(createTestShelf("shelf-2", "Shelf 2", 1, syncStatus = "SYNCED"))
        dao.upsertShelf(createTestShelf("shelf-3", "Shelf 3", 2, syncStatus = "CONFLICT"))

        // When
        val pendingShelves = dao.getPendingSyncShelves()

        // Then - Should return 2 shelves (not SYNCED)
        assertEquals(2, pendingShelves.size)
        assertTrue(pendingShelves.any { it.id == "shelf-1" })
        assertTrue(pendingShelves.any { it.id == "shelf-3" })
    }

    @Test
    fun `getPendingSyncCrossRefs returns cross refs with non-SYNCED status`() = runTest {
        // Given - Book and shelf exist
        dao.upsert(createTestBook("book-1", "Book 1"))
        dao.upsertShelf(createTestShelf("shelf-1", "Shelf 1", 0))

        // Cross refs with different statuses
        dao.upsertCrossRef(
            BookshelfBookCrossRef("shelf-1", "book-1", 1000L, syncStatus = "PENDING")
        )

        // When
        val pendingCrossRefs = dao.getPendingSyncCrossRefs()

        // Then
        assertEquals(1, pendingCrossRefs.size)
        assertEquals("PENDING", pendingCrossRefs[0].syncStatus)
    }

    // ========== Sync Status Update Queries ==========

    @Test
    fun `updateBookSyncStatus updates status and timestamp`() = runTest {
        // Given
        dao.upsert(createTestBook("book-1", "Book 1", syncStatus = "PENDING"))

        // When
        dao.updateBookSyncStatus("book-1", "SYNCED", 5000L)

        // Then
        val updated = dao.getBookById("book-1")
        assertEquals("SYNCED", updated?.syncStatus)
        assertEquals(5000L, updated?.lastModifiedAt)
    }

    @Test
    fun `updateShelfSyncStatus updates status and timestamp`() = runTest {
        // Given
        dao.upsertShelf(createTestShelf("shelf-1", "Shelf 1", 0, syncStatus = "PENDING"))

        // When
        dao.updateShelfSyncStatus("shelf-1", "SYNCED", 6000L)

        // Then
        val updated = dao.getShelfById("shelf-1")
        assertEquals("SYNCED", updated?.syncStatus)
        assertEquals(6000L, updated?.lastModifiedAt)
    }

    @Test
    fun `updateCrossRefSyncStatus updates status and timestamp`() = runTest {
        // Given
        dao.upsert(createTestBook("book-1", "Book 1"))
        dao.upsertShelf(createTestShelf("shelf-1", "Shelf 1", 0))
        dao.upsertCrossRef(
            BookshelfBookCrossRef("shelf-1", "book-1", 1000L, syncStatus = "PENDING")
        )

        // When
        dao.updateCrossRefSyncStatus("shelf-1", "book-1", "SYNCED", 7000L)

        // Then
        val crossRefs = dao.getPendingSyncCrossRefs()
        assertTrue("No pending cross refs should remain", crossRefs.isEmpty())
    }

    // ========== Owner Assignment Queries ==========

    @Test
    fun `assignOwnerToOrphanBooks assigns userId to books without owner`() = runTest {
        // Given - Books with and without owners
        dao.upsert(createTestBook("book-1", "Orphan 1", ownerId = null))
        dao.upsert(createTestBook("book-2", "Owned", ownerId = "existing-user"))
        dao.upsert(createTestBook("book-3", "Orphan 2", ownerId = null))

        // When
        dao.assignOwnerToOrphanBooks("new-user")

        // Then
        assertEquals("new-user", dao.getBookById("book-1")?.ownerId)
        assertEquals("existing-user", dao.getBookById("book-2")?.ownerId) // Unchanged
        assertEquals("new-user", dao.getBookById("book-3")?.ownerId)
    }

    @Test
    fun `assignOwnerToOrphanShelves assigns userId to shelves without owner`() = runTest {
        // Given - Shelves with and without owners
        dao.upsertShelf(createTestShelf("shelf-1", "Orphan 1", 0, ownerId = null))
        dao.upsertShelf(createTestShelf("shelf-2", "Owned", 1, ownerId = "existing-user"))
        dao.upsertShelf(createTestShelf("shelf-3", "Orphan 2", 2, ownerId = null))

        // When
        dao.assignOwnerToOrphanShelves("new-user")

        // Then
        assertEquals("new-user", dao.getShelfById("shelf-1")?.ownerId)
        assertEquals("existing-user", dao.getShelfById("shelf-2")?.ownerId) // Unchanged
        assertEquals("new-user", dao.getShelfById("shelf-3")?.ownerId)
    }

    // ========== Owner-based Queries ==========

    @Test
    fun `getBooksByOwner returns only books owned by specified user`() = runTest {
        // Given
        dao.upsert(createTestBook("book-1", "User1 Book", ownerId = "user-1"))
        dao.upsert(createTestBook("book-2", "User2 Book", ownerId = "user-2"))
        dao.upsert(createTestBook("book-3", "User1 Book 2", ownerId = "user-1"))

        // When
        val user1Books = dao.getBooksByOwner("user-1")

        // Then
        assertEquals(2, user1Books.size)
        assertTrue(user1Books.all { it.ownerId == "user-1" })
    }

    @Test
    fun `getShelvesByOwner returns shelves ordered by position`() = runTest {
        // Given
        dao.upsertShelf(createTestShelf("shelf-3", "Third", 2, ownerId = "user-1"))
        dao.upsertShelf(createTestShelf("shelf-1", "First", 0, ownerId = "user-1"))
        dao.upsertShelf(createTestShelf("shelf-2", "Second", 1, ownerId = "user-1"))
        dao.upsertShelf(createTestShelf("shelf-4", "Other User", 0, ownerId = "user-2"))

        // When
        val user1Shelves = dao.getShelvesByOwner("user-1")

        // Then
        assertEquals(3, user1Shelves.size)
        assertEquals("First", user1Shelves[0].name)
        assertEquals("Second", user1Shelves[1].name)
        assertEquals("Third", user1Shelves[2].name)
    }

    // ========== Mark Pending Queries ==========

    @Test
    fun `markAllBooksPending updates all books for owner to PENDING status`() = runTest {
        // Given
        dao.upsert(createTestBook("book-1", "Book 1", ownerId = "user-1", syncStatus = "SYNCED"))
        dao.upsert(createTestBook("book-2", "Book 2", ownerId = "user-1", syncStatus = "SYNCED"))
        dao.upsert(createTestBook("book-3", "Book 3", ownerId = "user-2", syncStatus = "SYNCED"))

        // When
        dao.markAllBooksPending("user-1")

        // Then
        assertEquals("PENDING", dao.getBookById("book-1")?.syncStatus)
        assertEquals("PENDING", dao.getBookById("book-2")?.syncStatus)
        assertEquals("SYNCED", dao.getBookById("book-3")?.syncStatus) // Different owner
    }

    @Test
    fun `markAllShelvesPending updates all shelves for owner to PENDING status`() = runTest {
        // Given
        dao.upsertShelf(createTestShelf("shelf-1", "Shelf 1", 0, ownerId = "user-1", syncStatus = "SYNCED"))
        dao.upsertShelf(createTestShelf("shelf-2", "Shelf 2", 1, ownerId = "user-1", syncStatus = "SYNCED"))
        dao.upsertShelf(createTestShelf("shelf-3", "Shelf 3", 2, ownerId = "user-2", syncStatus = "SYNCED"))

        // When
        dao.markAllShelvesPending("user-1")

        // Then
        assertEquals("PENDING", dao.getShelfById("shelf-1")?.syncStatus)
        assertEquals("PENDING", dao.getShelfById("shelf-2")?.syncStatus)
        assertEquals("SYNCED", dao.getShelfById("shelf-3")?.syncStatus) // Different owner
    }

    // ========== Sharing Queries ==========

    @Test
    fun `getShelfByShareCode returns shelf with matching code`() = runTest {
        // Given
        dao.upsertShelf(
            createTestShelf("shelf-1", "Shared Shelf", 0)
                .copy(isShared = true, shareCode = "ABC123")
        )
        dao.upsertShelf(
            createTestShelf("shelf-2", "Other Shelf", 1)
                .copy(isShared = false, shareCode = null)
        )

        // When
        val sharedShelf = dao.getShelfByShareCode("ABC123")

        // Then
        assertNotNull(sharedShelf)
        assertEquals("shelf-1", sharedShelf?.id)
        assertEquals("Shared Shelf", sharedShelf?.name)
    }

    @Test
    fun `getShelfByShareCode returns null for non-existent code`() = runTest {
        // Given
        dao.upsertShelf(createTestShelf("shelf-1", "Shelf 1", 0))

        // When
        val result = dao.getShelfByShareCode("NONEXISTENT")

        // Then
        assertNull(result)
    }

    @Test
    fun `updateShelfSharingStatus updates sharing fields`() = runTest {
        // Given
        dao.upsertShelf(
            createTestShelf("shelf-1", "Shelf 1", 0)
                .copy(isShared = false, shareCode = null)
        )

        // When - Enable sharing
        dao.updateShelfSharingStatus("shelf-1", isShared = true, shareCode = "XYZ789")

        // Then
        val updated = dao.getShelfById("shelf-1")
        assertEquals(true, updated?.isShared)
        assertEquals("XYZ789", updated?.shareCode)

        // When - Disable sharing
        dao.updateShelfSharingStatus("shelf-1", isShared = false, shareCode = null)

        // Then
        val disabled = dao.getShelfById("shelf-1")
        assertEquals(false, disabled?.isShared)
        assertNull(disabled?.shareCode)
    }

    // ========== SyncDao Tests ==========

    @Test
    fun `syncDao upsertSyncMetadata creates new metadata`() = runTest {
        // Given
        val metadata = SyncMetadataEntity(
            userId = "user-123",
            lastSyncTimestamp = 1000L,
            syncInProgress = false,
            lastSyncError = null,
            pendingOperationsCount = 0
        )

        // When
        syncDao.upsertSyncMetadata(metadata)

        // Then
        val retrieved = syncDao.getSyncMetadata("user-123")
        assertNotNull(retrieved)
        assertEquals("user-123", retrieved?.userId)
        assertEquals(1000L, retrieved?.lastSyncTimestamp)
    }

    @Test
    fun `syncDao getSyncMetadata returns null for non-existent user`() = runTest {
        // When
        val result = syncDao.getSyncMetadata("non-existent")

        // Then
        assertNull(result)
    }

    @Test
    fun `syncDao observeSyncMetadata emits updates`() = runTest {
        // Given
        val metadata = SyncMetadataEntity(
            userId = "user-123",
            lastSyncTimestamp = 1000L
        )
        syncDao.upsertSyncMetadata(metadata)

        // When
        val observed = syncDao.observeSyncMetadata("user-123").first()

        // Then
        assertNotNull(observed)
        assertEquals(1000L, observed?.lastSyncTimestamp)
    }

    @Test
    fun `syncDao updateLastSyncTimestamp updates only timestamp`() = runTest {
        // Given
        syncDao.upsertSyncMetadata(
            SyncMetadataEntity(
                userId = "user-123",
                lastSyncTimestamp = 1000L,
                pendingOperationsCount = 5
            )
        )

        // When
        syncDao.updateLastSyncTimestamp("user-123", 2000L)

        // Then
        val updated = syncDao.getSyncMetadata("user-123")
        assertEquals(2000L, updated?.lastSyncTimestamp)
        assertEquals(5, updated?.pendingOperationsCount) // Unchanged
    }

    @Test
    fun `syncDao updateSyncInProgress updates flag`() = runTest {
        // Given
        syncDao.upsertSyncMetadata(
            SyncMetadataEntity(
                userId = "user-123",
                lastSyncTimestamp = 1000L,
                syncInProgress = false
            )
        )

        // When
        syncDao.updateSyncInProgress("user-123", true)

        // Then
        val updated = syncDao.getSyncMetadata("user-123")
        assertEquals(true, updated?.syncInProgress)
    }

    @Test
    fun `syncDao updateLastSyncError updates error message`() = runTest {
        // Given
        syncDao.upsertSyncMetadata(
            SyncMetadataEntity(
                userId = "user-123",
                lastSyncTimestamp = 1000L
            )
        )

        // When
        syncDao.updateLastSyncError("user-123", "Network timeout")

        // Then
        val updated = syncDao.getSyncMetadata("user-123")
        assertEquals("Network timeout", updated?.lastSyncError)

        // When - Clear error
        syncDao.updateLastSyncError("user-123", null)

        // Then
        val cleared = syncDao.getSyncMetadata("user-123")
        assertNull(cleared?.lastSyncError)
    }

    @Test
    fun `syncDao deleteSyncMetadata removes metadata`() = runTest {
        // Given
        syncDao.upsertSyncMetadata(
            SyncMetadataEntity(
                userId = "user-123",
                lastSyncTimestamp = 1000L
            )
        )

        // When
        syncDao.deleteSyncMetadata("user-123")

        // Then
        val result = syncDao.getSyncMetadata("user-123")
        assertNull(result)
    }

    // ========== Helper Methods ==========

    private fun createTestBook(
        id: String,
        title: String,
        ownerId: String? = null,
        syncStatus: String = "PENDING"
    ): BookEntity {
        return BookEntity(
            id = id,
            title = title,
            description = "Test description",
            imageUrl = "https://example.com/cover.jpg",
            languages = listOf("en"),
            authors = listOf("Test Author"),
            firstPublishYear = "2020",
            ratingsAverage = 4.5,
            ratingsCount = 100,
            numPagesMedian = 300,
            numEditions = 5,
            purchased = false,
            spineColor = -16711936,
            ownerId = ownerId,
            syncStatus = syncStatus
        )
    }

    private fun createTestShelf(
        id: String,
        name: String,
        position: Int,
        ownerId: String? = null,
        syncStatus: String = "PENDING"
    ): BookshelfEntity {
        return BookshelfEntity(
            id = id,
            name = name,
            shelfMaterial = "DARK_WOOD",
            position = position,
            ownerId = ownerId,
            syncStatus = syncStatus
        )
    }
}
