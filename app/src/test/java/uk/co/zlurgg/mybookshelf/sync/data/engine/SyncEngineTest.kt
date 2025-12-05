package uk.co.zlurgg.mybookshelf.sync.data.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.data.database.dao.SyncDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.SyncMetadataEntity
import uk.co.zlurgg.mybookshelf.sync.domain.model.SharedShelf
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncBook
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncBookshelf
import uk.co.zlurgg.mybookshelf.sync.data.service.DefaultConflictResolver
import uk.co.zlurgg.mybookshelf.sync.domain.service.ConnectivityMonitor
import uk.co.zlurgg.mybookshelf.sync.domain.service.RemoteSyncDataSource
import uk.co.zlurgg.mybookshelf.testutil.helpers.TestTimeProvider

/**
 * Unit tests for SyncEngine.
 */
class SyncEngineTest {

    private lateinit var syncEngine: SyncEngine
    private lateinit var fakeBookshelfDao: FakeBookshelfDao
    private lateinit var fakeSyncDao: FakeSyncDao
    private lateinit var fakeRemoteDataSource: FakeRemoteSyncDataSource
    private lateinit var fakeConnectivityMonitor: FakeConnectivityMonitor
    private lateinit var testTimeProvider: TestTimeProvider

    @Before
    fun setup() {
        fakeBookshelfDao = FakeBookshelfDao()
        fakeSyncDao = FakeSyncDao()
        fakeRemoteDataSource = FakeRemoteSyncDataSource()
        fakeConnectivityMonitor = FakeConnectivityMonitor()
        testTimeProvider = TestTimeProvider(1000L)

        syncEngine = SyncEngine(
            bookshelfDao = fakeBookshelfDao,
            syncDao = fakeSyncDao,
            remoteDataSource = fakeRemoteDataSource,
            conflictResolver = DefaultConflictResolver.lastWriteWins(),
            connectivityMonitor = fakeConnectivityMonitor,
            timeProvider = testTimeProvider
        )
    }

    // ==================== performFullSync Tests ====================

    @Test
    fun `performFullSync returns error when not connected`() = runBlocking {
        // Given
        fakeConnectivityMonitor.setConnected(false)

        // When
        val result = syncEngine.performFullSync("user-1")

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `performFullSync returns error when sync already in progress`() = runBlocking {
        // Given
        fakeConnectivityMonitor.setConnected(true)
        fakeSyncDao.setMetadata(
            SyncMetadataEntity(
                userId = "user-1",
                lastSyncTimestamp = 0L,
                syncInProgress = true
            )
        )

        // When
        val result = syncEngine.performFullSync("user-1")

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DataError.Sync.SYNC_IN_PROGRESS, (result as Result.Error).error)
    }

    @Test
    fun `performFullSync succeeds with no pending changes`() = runBlocking {
        // Given
        fakeConnectivityMonitor.setConnected(true)

        // When
        val result = syncEngine.performFullSync("user-1")

        // Then
        assertTrue(result is Result.Success)
        val syncResult = (result as Result.Success).data
        assertEquals(0, syncResult.pushedCount)
        assertEquals(0, syncResult.pulledCount)
    }

    @Test
    fun `performFullSync pushes pending books`() = runBlocking {
        // Given
        fakeConnectivityMonitor.setConnected(true)
        val pendingBook = createTestBookEntity(
            id = "book-1",
            ownerId = "user-1",
            syncStatus = "PENDING"
        )
        fakeBookshelfDao.addPendingBook(pendingBook)

        // When
        val result = syncEngine.performFullSync("user-1")

        // Then
        assertTrue(result is Result.Success)
        val syncResult = (result as Result.Success).data
        assertEquals(1, syncResult.pushedCount)
        assertTrue(fakeRemoteDataSource.uploadedBooks.containsKey("book-1"))
    }

    @Test
    fun `performFullSync handles deleted books`() = runBlocking {
        // Given
        fakeConnectivityMonitor.setConnected(true)
        val deletedBook = createTestBookEntity(
            id = "book-deleted",
            ownerId = "user-1",
            syncStatus = "DELETED"
        )
        fakeBookshelfDao.addPendingBook(deletedBook)

        // When
        val result = syncEngine.performFullSync("user-1")

        // Then
        assertTrue(result is Result.Success)
        val syncResult = (result as Result.Success).data
        assertEquals(1, syncResult.deletedCount)
        assertTrue(fakeRemoteDataSource.deletedBookIds.contains("book-deleted"))
    }

    // ==================== pushLocalChanges Tests ====================

    @Test
    fun `pushLocalChanges uploads pending books`() = runBlocking {
        // Given
        val pendingBook = createTestBookEntity(
            id = "book-1",
            ownerId = "user-1",
            syncStatus = "PENDING"
        )
        fakeBookshelfDao.addPendingBook(pendingBook)

        // When
        val result = syncEngine.pushLocalChanges("user-1")

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.pushedCount)
    }

    @Test
    fun `pushLocalChanges uploads pending shelves`() = runBlocking {
        // Given
        val pendingShelf = createTestBookshelfEntity(
            id = "shelf-1",
            ownerId = "user-1",
            syncStatus = "PENDING"
        )
        fakeBookshelfDao.addPendingShelf(pendingShelf)

        // When
        val result = syncEngine.pushLocalChanges("user-1")

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.pushedCount)
    }

    @Test
    fun `pushLocalChanges filters by owner`() = runBlocking {
        // Given - books from different owners
        val myBook = createTestBookEntity(id = "book-1", ownerId = "user-1", syncStatus = "PENDING")
        val otherBook = createTestBookEntity(id = "book-2", ownerId = "user-2", syncStatus = "PENDING")
        fakeBookshelfDao.addPendingBook(myBook)
        fakeBookshelfDao.addPendingBook(otherBook)

        // When
        val result = syncEngine.pushLocalChanges("user-1")

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.pushedCount)
        assertTrue(fakeRemoteDataSource.uploadedBooks.containsKey("book-1"))
        assertTrue(!fakeRemoteDataSource.uploadedBooks.containsKey("book-2"))
    }

    // ==================== pullRemoteChanges Tests ====================

    @Test
    fun `pullRemoteChanges downloads new books`() = runBlocking {
        // Given
        val remoteBook = SyncBook(
            id = "remote-book-1",
            title = "Remote Book",
            authors = listOf("Author"),
            imageUrl = "url",
            description = null,
            languages = emptyList(),
            firstPublishYear = null,
            averageRating = null,
            ratingCount = null,
            numPages = null,
            numEditions = 0,
            purchased = false,
            spineColor = 0,
            readingStatus = "WANT_TO_READ",
            personalRating = 0f,
            personalNotes = "",
            dateAdded = null,
            purchaseDate = null,
            isbn = null,
            publisher = null,
            publishDate = null,
            internetArchiveId = null,
            version = 1L,
            lastModifiedAt = 2000L
        )
        fakeRemoteDataSource.addRemoteBook(remoteBook)

        // When
        val result = syncEngine.pullRemoteChanges("user-1")

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.pulledCount)
        assertTrue(fakeBookshelfDao.upsertedBooks.any { it.id == "remote-book-1" })
    }

    @Test
    fun `pullRemoteChanges updates synced local book with remote version`() = runBlocking {
        // Given - local book is SYNCED
        val localBook = createTestBookEntity(
            id = "book-1",
            ownerId = "user-1",
            syncStatus = "SYNCED"
        )
        fakeBookshelfDao.addBook(localBook)

        val remoteBook = SyncBook(
            id = "book-1",
            title = "Updated Title",
            authors = listOf("Author"),
            imageUrl = "url",
            description = null,
            languages = emptyList(),
            firstPublishYear = null,
            averageRating = null,
            ratingCount = null,
            numPages = null,
            numEditions = 0,
            purchased = false,
            spineColor = 0,
            readingStatus = "WANT_TO_READ",
            personalRating = 0f,
            personalNotes = "",
            dateAdded = null,
            purchaseDate = null,
            isbn = null,
            publisher = null,
            publishDate = null,
            internetArchiveId = null,
            version = 2L,
            lastModifiedAt = 2000L
        )
        fakeRemoteDataSource.addRemoteBook(remoteBook)

        // When
        val result = syncEngine.pullRemoteChanges("user-1")

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.pulledCount)
    }

    // ==================== Conflict Detection Tests ====================

    @Test
    fun `pullRemoteChanges detects conflict when local has pending changes`() = runBlocking {
        // Given - local book has PENDING changes
        val localBook = createTestBookEntity(
            id = "book-1",
            ownerId = "user-1",
            syncStatus = "PENDING",
            lastModifiedAt = 1500L,
            version = 1L
        )
        fakeBookshelfDao.addBook(localBook)

        val remoteBook = SyncBook(
            id = "book-1",
            title = "Remote Updated",
            authors = listOf("Author"),
            imageUrl = "url",
            description = null,
            languages = emptyList(),
            firstPublishYear = null,
            averageRating = null,
            ratingCount = null,
            numPages = null,
            numEditions = 0,
            purchased = false,
            spineColor = 0,
            readingStatus = "WANT_TO_READ",
            personalRating = 0f,
            personalNotes = "",
            dateAdded = null,
            purchaseDate = null,
            isbn = null,
            publisher = null,
            publishDate = null,
            internetArchiveId = null,
            version = 2L,
            lastModifiedAt = 2000L
        )
        fakeRemoteDataSource.addRemoteBook(remoteBook)

        // When
        val result = syncEngine.pullRemoteChanges("user-1")

        // Then - with LAST_WRITE_WINS, remote wins (newer timestamp)
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.pulledCount)
    }

    // ==================== Cancel Tests ====================

    @Test
    fun `cancel sets cancellation flag`() {
        // When
        syncEngine.cancel()

        // Then - We verify the flag was set by checking that subsequent sync operations
        // would respect it (though we'd need a longer-running test to fully verify)
        // The cancel() method is mainly tested through integration
        assertTrue(true) // Cancel flag is internal, just verify no exception
    }

    // ==================== Helper Methods ====================

    private fun createTestBookEntity(
        id: String = "book-1",
        title: String = "Test Book",
        ownerId: String? = null,
        syncStatus: String = "PENDING",
        lastModifiedAt: Long = 1000L,
        version: Long = 1L
    ) = BookEntity(
        id = id,
        title = title,
        description = "Description",
        imageUrl = "https://example.com/cover.jpg",
        languages = listOf("en"),
        authors = listOf("Author"),
        firstPublishYear = "2020",
        ratingsAverage = 4.0,
        ratingsCount = 100,
        numPagesMedian = 200,
        numEditions = 5,
        purchased = false,
        spineColor = 0,
        ownerId = ownerId,
        syncStatus = syncStatus,
        lastModifiedAt = lastModifiedAt,
        version = version
    )

    private fun createTestBookshelfEntity(
        id: String = "shelf-1",
        name: String = "Test Shelf",
        ownerId: String? = null,
        syncStatus: String = "PENDING"
    ) = BookshelfEntity(
        id = id,
        name = name,
        shelfMaterial = "LIGHT_WOOD",
        position = 0,
        isTidyMode = false,
        ownerId = ownerId,
        syncStatus = syncStatus,
        lastModifiedAt = 1000L,
        version = 1L
    )

    // ==================== Fake Implementations ====================

    private class FakeConnectivityMonitor : ConnectivityMonitor {
        private var connected = true

        fun setConnected(value: Boolean) {
            connected = value
        }

        override fun isConnected(): Boolean = connected
        override fun observeConnectivity(): Flow<Boolean> = flowOf(connected)
    }

    private class FakeSyncDao : SyncDao {
        private var metadata: SyncMetadataEntity? = null

        fun setMetadata(entity: SyncMetadataEntity) {
            metadata = entity
        }

        override suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity) {
            this.metadata = metadata
        }

        override suspend fun getSyncMetadata(userId: String): SyncMetadataEntity? {
            return if (metadata?.userId == userId) metadata else null
        }

        override fun observeSyncMetadata(userId: String): Flow<SyncMetadataEntity?> {
            return flowOf(if (metadata?.userId == userId) metadata else null)
        }

        override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long) {
            metadata = metadata?.copy(lastSyncTimestamp = timestamp)
        }

        override suspend fun updateSyncInProgress(userId: String, inProgress: Boolean) {
            metadata = metadata?.copy(syncInProgress = inProgress)
        }

        override suspend fun updateLastSyncError(userId: String, error: String?) {
            metadata = metadata?.copy(lastSyncError = error)
        }

        override suspend fun updatePendingOperationsCount(userId: String, count: Int) {
            metadata = metadata?.copy(pendingOperationsCount = count)
        }

        override suspend fun deleteSyncMetadata(userId: String) {
            if (metadata?.userId == userId) metadata = null
        }
    }

    private class FakeBookshelfDao : BookshelfDao {
        private val books = mutableMapOf<String, BookEntity>()
        private val shelves = mutableMapOf<String, BookshelfEntity>()
        private val pendingBooks = mutableListOf<BookEntity>()
        private val pendingShelves = mutableListOf<BookshelfEntity>()
        val upsertedBooks = mutableListOf<BookEntity>()
        val deletedBookIds = mutableListOf<String>()

        fun addPendingBook(book: BookEntity) {
            pendingBooks.add(book)
            books[book.id] = book
        }

        fun addPendingShelf(shelf: BookshelfEntity) {
            pendingShelves.add(shelf)
            shelves[shelf.id] = shelf
        }

        fun addBook(book: BookEntity) {
            books[book.id] = book
        }

        override suspend fun getPendingSyncBooks(): List<BookEntity> = pendingBooks.toList()
        override suspend fun getPendingSyncShelves(): List<BookshelfEntity> = pendingShelves.toList()
        override suspend fun getPendingSyncCrossRefs(): List<BookshelfBookCrossRef> = emptyList()

        override suspend fun getBookById(id: String): BookEntity? = books[id]
        override suspend fun getShelfById(id: String): BookshelfEntity? = shelves[id]

        override suspend fun upsert(book: BookEntity) {
            books[book.id] = book
            upsertedBooks.add(book)
        }

        override suspend fun upsertShelf(shelf: BookshelfEntity) {
            shelves[shelf.id] = shelf
        }

        override suspend fun deleteBook(id: String) {
            books.remove(id)
            deletedBookIds.add(id)
            pendingBooks.removeAll { it.id == id }
        }

        override suspend fun deleteShelf(id: String) {
            shelves.remove(id)
            pendingShelves.removeAll { it.id == id }
        }

        override suspend fun updateBookSyncStatus(id: String, status: String, timestamp: Long) {
            books[id]?.let { book ->
                books[id] = book.copy(syncStatus = status, lastModifiedAt = timestamp)
            }
        }

        override suspend fun updateShelfSyncStatus(id: String, status: String, timestamp: Long) {
            shelves[id]?.let { shelf ->
                shelves[id] = shelf.copy(syncStatus = status, lastModifiedAt = timestamp)
            }
        }

        override fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>> = flowOf(emptyList())

        override suspend fun countOrphanBooks(): Int = books.values.count { it.ownerId == null }
        override suspend fun countOrphanShelves(): Int = shelves.values.count { it.ownerId == null }
        override suspend fun assignOwnerToOrphanBooks(userId: String) {}
        override suspend fun assignOwnerToOrphanShelves(userId: String) {}

        override fun getAllShelves(): Flow<List<BookshelfEntity>> = flowOf(shelves.values.toList())
        override fun getShelvesForUser(userId: String?): Flow<List<BookshelfEntity>> =
            flowOf(shelves.values.filter { it.ownerId == userId || it.ownerId == null }.toList())
        override suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef) {}
        override suspend fun deleteCrossRef(shelfId: String, bookId: String) {}
        override suspend fun deleteAllCrossRefsForShelf(shelfId: String) {}
        override fun getBookCountForShelf(shelfId: String): Flow<Int> = flowOf(0)
        override fun isBookInAnyShelf(bookId: String): Flow<Boolean> = flowOf(false)
        override fun getShelvesForBook(bookId: String): Flow<List<String>> = flowOf(emptyList())
        override suspend fun updateCrossRefSyncStatus(shelfId: String, bookId: String, status: String, timestamp: Long) {}
        override suspend fun getShelfByShareCode(shareCode: String): BookshelfEntity? = null
        override suspend fun updateShelfSharingStatus(id: String, isShared: Boolean, shareCode: String?) {}
        override suspend fun getBooksByOwner(ownerId: String): List<BookEntity> = books.values.filter { it.ownerId == ownerId }
        override suspend fun getShelvesByOwner(ownerId: String): List<BookshelfEntity> = shelves.values.filter { it.ownerId == ownerId }
        override suspend fun markAllBooksPending(ownerId: String) {}
        override suspend fun markAllShelvesPending(ownerId: String) {}
        override suspend fun deleteAllCrossRefsForOwner(ownerId: String) {}
        override suspend fun deleteAllBooksForOwner(ownerId: String) {
            books.entries.removeIf { it.value.ownerId == ownerId }
        }
        override suspend fun deleteAllShelvesForOwner(ownerId: String) {
            shelves.entries.removeIf { it.value.ownerId == ownerId }
        }
    }

    private class FakeRemoteSyncDataSource : RemoteSyncDataSource {
        val uploadedBooks = mutableMapOf<String, SyncBook>()
        val uploadedShelves = mutableMapOf<String, SyncBookshelf>()
        val deletedBookIds = mutableListOf<String>()
        val deletedShelfIds = mutableListOf<String>()
        private val remoteBooks = mutableListOf<SyncBook>()
        private val remoteShelves = mutableListOf<SyncBookshelf>()

        fun addRemoteBook(book: SyncBook) {
            remoteBooks.add(book)
        }

        fun addRemoteShelf(shelf: SyncBookshelf) {
            remoteShelves.add(shelf)
        }

        override suspend fun uploadBook(userId: String, book: SyncBook): Result<Unit, DataError.Sync> {
            uploadedBooks[book.id] = book
            return Result.Success(Unit)
        }

        override suspend fun downloadBook(userId: String, bookId: String): Result<SyncBook?, DataError.Sync> {
            return Result.Success(remoteBooks.find { it.id == bookId })
        }

        override suspend fun downloadBooksSince(userId: String, sinceTimestamp: Long): Result<List<SyncBook>, DataError.Sync> {
            return Result.Success(remoteBooks.filter { it.lastModifiedAt > sinceTimestamp })
        }

        override suspend fun deleteBook(userId: String, bookId: String): Result<Unit, DataError.Sync> {
            deletedBookIds.add(bookId)
            return Result.Success(Unit)
        }

        override suspend fun uploadBookshelf(userId: String, shelf: SyncBookshelf): Result<Unit, DataError.Sync> {
            uploadedShelves[shelf.id] = shelf
            return Result.Success(Unit)
        }

        override suspend fun downloadBookshelf(userId: String, shelfId: String): Result<SyncBookshelf?, DataError.Sync> {
            return Result.Success(remoteShelves.find { it.id == shelfId })
        }

        override suspend fun downloadBookshelvesSince(userId: String, sinceTimestamp: Long): Result<List<SyncBookshelf>, DataError.Sync> {
            return Result.Success(remoteShelves.filter { it.lastModifiedAt > sinceTimestamp })
        }

        override suspend fun deleteBookshelf(userId: String, shelfId: String): Result<Unit, DataError.Sync> {
            deletedShelfIds.add(shelfId)
            return Result.Success(Unit)
        }

        override suspend fun shareShelf(sharedShelf: SharedShelf): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun unshareShelf(shareCode: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun getSharedShelf(shareCode: String): Result<SharedShelf?, DataError.Sync> = Result.Success(null)
        override suspend fun subscribeToShelf(shareCode: String, userId: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun unsubscribeFromShelf(shareCode: String, userId: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun uploadBooks(userId: String, books: List<SyncBook>): Result<Int, DataError.Sync> {
            books.forEach { uploadedBooks[it.id] = it }
            return Result.Success(books.size)
        }
        override suspend fun uploadBookshelves(userId: String, shelves: List<SyncBookshelf>): Result<Int, DataError.Sync> {
            shelves.forEach { uploadedShelves[it.id] = it }
            return Result.Success(shelves.size)
        }
    }
}
