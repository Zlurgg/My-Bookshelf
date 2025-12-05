package uk.co.zlurgg.mybookshelf.sync.data.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class MigrateLocalDataUseCaseImplTest {

    private lateinit var fakeDao: FakeBookshelfDao
    private lateinit var fakeSyncScheduler: FakeSyncScheduler
    private lateinit var useCase: MigrateLocalDataUseCaseImpl

    @Before
    fun setup() {
        fakeDao = FakeBookshelfDao()
        fakeSyncScheduler = FakeSyncScheduler()
        useCase = MigrateLocalDataUseCaseImpl(fakeDao, fakeSyncScheduler)
    }

    // ==================== No Migration Needed Tests ====================

    @Test
    fun `returns NO_MIGRATION_NEEDED when no orphan data exists`() = runTest {
        // No orphan data added to the fake DAO

        val result = useCase.execute("user-123")

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertEquals(0, migration.booksAssigned)
        assertEquals(0, migration.shelvesAssigned)
        assertFalse("Should not have data to migrate", migration.hadDataToMigrate)
        assertFalse("Sync should not be triggered", migration.syncTriggered)
    }

    @Test
    fun `does not trigger sync when no orphan data exists`() = runTest {
        val result = useCase.execute("user-123")

        assertTrue("Should be success", result is Result.Success)
        assertFalse("Sync should not be triggered", fakeSyncScheduler.syncTriggered)
    }

    // ==================== Migration With Data Tests ====================

    @Test
    fun `migrates orphan books correctly`() = runTest {
        fakeDao.addOrphanBook(createTestBook("book-1"))
        fakeDao.addOrphanBook(createTestBook("book-2"))
        fakeDao.addOrphanBook(createTestBook("book-3"))

        val result = useCase.execute("user-123")

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertEquals(3, migration.booksAssigned)
        assertEquals("user-123", fakeDao.assignedBookOwnerId)
    }

    @Test
    fun `migrates orphan shelves correctly`() = runTest {
        fakeDao.addOrphanShelf(createTestShelf("shelf-1"))
        fakeDao.addOrphanShelf(createTestShelf("shelf-2"))

        val result = useCase.execute("user-123")

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertEquals(2, migration.shelvesAssigned)
        assertEquals("user-123", fakeDao.assignedShelfOwnerId)
    }

    @Test
    fun `migrates both books and shelves`() = runTest {
        fakeDao.addOrphanBook(createTestBook("book-1"))
        fakeDao.addOrphanShelf(createTestShelf("shelf-1"))
        fakeDao.addOrphanShelf(createTestShelf("shelf-2"))

        val result = useCase.execute("user-123")

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertEquals(1, migration.booksAssigned)
        assertEquals(2, migration.shelvesAssigned)
        assertEquals(3, migration.totalMigrated)
        assertTrue("Should have data to migrate", migration.hadDataToMigrate)
    }

    @Test
    fun `marks entities as pending sync after migration`() = runTest {
        fakeDao.addOrphanBook(createTestBook("book-1"))

        useCase.execute("user-123")

        assertEquals("user-123", fakeDao.pendingMarkedOwnerId)
    }

    @Test
    fun `triggers immediate sync after successful migration`() = runTest {
        fakeDao.addOrphanBook(createTestBook("book-1"))

        val result = useCase.execute("user-123")

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertTrue("Sync should be triggered", migration.syncTriggered)
        assertTrue("Scheduler should have triggered sync", fakeSyncScheduler.syncTriggered)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles only books no shelves`() = runTest {
        fakeDao.addOrphanBook(createTestBook("book-1"))

        val result = useCase.execute("user-123")

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertEquals(1, migration.booksAssigned)
        assertEquals(0, migration.shelvesAssigned)
        assertTrue("Should have data to migrate", migration.hadDataToMigrate)
    }

    @Test
    fun `handles only shelves no books`() = runTest {
        fakeDao.addOrphanShelf(createTestShelf("shelf-1"))

        val result = useCase.execute("user-123")

        assertTrue("Should be success", result is Result.Success)
        val migration = (result as Result.Success).data
        assertEquals(0, migration.booksAssigned)
        assertEquals(1, migration.shelvesAssigned)
        assertTrue("Should have data to migrate", migration.hadDataToMigrate)
    }

    // ==================== Test Helpers ====================

    private fun createTestBook(id: String) = BookEntity(
        id = id,
        title = "Test Book $id",
        description = null,
        imageUrl = "",
        languages = emptyList(),
        authors = emptyList(),
        firstPublishYear = null,
        ratingsAverage = null,
        ratingsCount = null,
        numPagesMedian = null,
        numEditions = 0,
        purchased = false,
        spineColor = 0,
        ownerId = null,
        syncStatus = "PENDING"
    )

    private fun createTestShelf(id: String) = BookshelfEntity(
        id = id,
        name = "Test Shelf $id",
        shelfMaterial = "OAK",
        position = 0,
        ownerId = null,
        syncStatus = "PENDING"
    )

    // ==================== Fakes ====================

    private class FakeSyncScheduler : SyncSchedulerService {
        var syncTriggered = false

        override fun schedulePeriodicSync() {}
        override fun triggerImmediateSync() {
            syncTriggered = true
        }
        override fun cancelAllSync() {}
    }

    private class FakeBookshelfDao : BookshelfDao {
        private val orphanBooks = mutableListOf<BookEntity>()
        private val orphanShelves = mutableListOf<BookshelfEntity>()
        var assignedBookOwnerId: String? = null
        var assignedShelfOwnerId: String? = null
        var pendingMarkedOwnerId: String? = null

        fun addOrphanBook(book: BookEntity) {
            orphanBooks.add(book)
        }

        fun addOrphanShelf(shelf: BookshelfEntity) {
            orphanShelves.add(shelf)
        }

        override suspend fun countOrphanBooks(): Int = orphanBooks.size
        override suspend fun countOrphanShelves(): Int = orphanShelves.size

        override suspend fun assignOwnerToOrphanBooks(userId: String) {
            assignedBookOwnerId = userId
        }

        override suspend fun assignOwnerToOrphanShelves(userId: String) {
            assignedShelfOwnerId = userId
        }

        override suspend fun markAllBooksPending(ownerId: String) {
            pendingMarkedOwnerId = ownerId
        }

        override suspend fun markAllShelvesPending(ownerId: String) {
            pendingMarkedOwnerId = ownerId
        }

        // Unused methods for this test
        override suspend fun upsert(book: BookEntity) {}
        override suspend fun getBookById(id: String): BookEntity? = null
        override suspend fun deleteBook(id: String) {}
        override suspend fun upsertShelf(shelf: BookshelfEntity) {}
        override fun getAllShelves(): Flow<List<BookshelfEntity>> = flowOf(emptyList())
        override fun getShelvesForUser(userId: String?): Flow<List<BookshelfEntity>> = flowOf(emptyList())
        override suspend fun getShelfById(id: String): BookshelfEntity? = null
        override suspend fun deleteShelf(id: String) {}
        override suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef) {}
        override suspend fun deleteCrossRef(shelfId: String, bookId: String) {}
        override suspend fun deleteAllCrossRefsForShelf(shelfId: String) {}
        override fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>> = flowOf(emptyList())
        override fun getBookCountForShelf(shelfId: String): Flow<Int> = flowOf(0)
        override fun isBookInAnyShelf(bookId: String): Flow<Boolean> = flowOf(false)
        override fun getShelvesForBook(bookId: String): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getPendingSyncBooks(): List<BookEntity> = emptyList()
        override suspend fun getPendingSyncShelves(): List<BookshelfEntity> = emptyList()
        override suspend fun getPendingSyncCrossRefs(): List<BookshelfBookCrossRef> = emptyList()
        override suspend fun updateBookSyncStatus(id: String, status: String, timestamp: Long) {}
        override suspend fun updateShelfSyncStatus(id: String, status: String, timestamp: Long) {}
        override suspend fun updateCrossRefSyncStatus(shelfId: String, bookId: String, status: String, timestamp: Long) {}
        override suspend fun getShelfByShareCode(shareCode: String): BookshelfEntity? = null
        override suspend fun updateShelfSharingStatus(id: String, isShared: Boolean, shareCode: String?) {}
        override suspend fun getBooksByOwner(ownerId: String): List<BookEntity> = emptyList()
        override suspend fun getShelvesByOwner(ownerId: String): List<BookshelfEntity> = emptyList()
        override suspend fun deleteAllCrossRefsForOwner(ownerId: String) {}
        override suspend fun deleteAllBooksForOwner(ownerId: String) {}
        override suspend fun deleteAllShelvesForOwner(ownerId: String) {}
    }
}
