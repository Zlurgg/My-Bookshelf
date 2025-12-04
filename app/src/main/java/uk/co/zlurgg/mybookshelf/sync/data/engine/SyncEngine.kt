package uk.co.zlurgg.mybookshelf.sync.data.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.sync.data.database.SyncDao
import uk.co.zlurgg.mybookshelf.sync.data.database.SyncMetadataEntity
import uk.co.zlurgg.mybookshelf.sync.data.mapper.toEntity
import uk.co.zlurgg.mybookshelf.sync.data.mapper.toFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictResolution
import uk.co.zlurgg.mybookshelf.sync.domain.model.EntityType
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncConflict
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncPhase
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncProgress
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncResult
import uk.co.zlurgg.mybookshelf.sync.domain.service.ConflictResolver
import uk.co.zlurgg.mybookshelf.sync.domain.service.ConnectivityMonitor
import uk.co.zlurgg.mybookshelf.sync.domain.service.RemoteSyncDataSource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Core sync engine that coordinates local and remote data synchronization.
 *
 * Responsibilities:
 * - Push local changes to cloud
 * - Pull remote changes from cloud
 * - Detect and resolve conflicts
 * - Track sync progress
 */
class SyncEngine(
    private val bookshelfDao: BookshelfDao,
    private val syncDao: SyncDao,
    private val remoteDataSource: RemoteSyncDataSource,
    private val conflictResolver: ConflictResolver,
    private val connectivityMonitor: ConnectivityMonitor,
    private val timeProvider: TimeProvider
) {
    private val _progress = MutableStateFlow(SyncProgress())
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    private val isCancelled = AtomicBoolean(false)
    private val conflicts = mutableListOf<SyncConflict>()

    /**
     * Performs a full sync cycle: push local changes, then pull remote changes.
     */
    suspend fun performFullSync(userId: String): Result<SyncResult, DataError.Sync> {
        Timber.tag(TAG).d("=== SYNC START for user: %s ===", userId)

        if (!connectivityMonitor.isConnected()) {
            Timber.tag(TAG).w("Sync aborted: no network connection")
            return Result.Error(DataError.Sync.NETWORK_ERROR)
        }

        // Check if sync already in progress
        val metadata = syncDao.getSyncMetadata(userId)
        if (metadata?.syncInProgress == true) {
            Timber.tag(TAG).w("Sync already in progress")
            return Result.Error(DataError.Sync.SYNC_IN_PROGRESS)
        }

        // Initialize sync metadata if needed
        if (metadata == null) {
            syncDao.upsertSyncMetadata(
                SyncMetadataEntity(
                    userId = userId,
                    lastSyncTimestamp = 0L,
                    syncInProgress = true
                )
            )
        } else {
            syncDao.updateSyncInProgress(userId, true)
        }

        isCancelled.set(false)
        conflicts.clear()

        try {
            // Push local changes
            updateProgress(SyncPhase.PUSHING_BOOKS, 0, 0)
            val pushResult = pushLocalChanges(userId)
            if (pushResult is Result.Error) {
                markSyncComplete(userId, pushResult.error.name)
                return pushResult
            }

            if (isCancelled.get()) {
                markSyncComplete(userId, "CANCELLED")
                return Result.Success(SyncResult.EMPTY)
            }

            // Pull remote changes
            updateProgress(SyncPhase.PULLING_BOOKS, 0, 0)
            val pullResult = pullRemoteChanges(userId)
            if (pullResult is Result.Error) {
                markSyncComplete(userId, pullResult.error.name)
                return pullResult
            }

            if (isCancelled.get()) {
                markSyncComplete(userId, "CANCELLED")
                return Result.Success(SyncResult.EMPTY)
            }

            // Finalize
            updateProgress(SyncPhase.FINALIZING, 0, 0)
            val pushData = (pushResult as Result.Success).data
            val pullData = (pullResult as Result.Success).data

            val finalResult = SyncResult(
                pushedCount = pushData.pushedCount,
                pulledCount = pullData.pulledCount,
                conflictCount = conflicts.size,
                resolvedCount = conflicts.count { conflictResolver.canAutoResolve(it) },
                deletedCount = pushData.deletedCount + pullData.deletedCount,
                unresolvedConflictIds = conflicts
                    .filter { !conflictResolver.canAutoResolve(it) }
                    .map { it.entityId },
                completedAt = timeProvider.currentTimeMillis()
            )

            markSyncComplete(userId, null)
            Timber.tag(TAG).d("=== SYNC COMPLETE: pushed=%d, pulled=%d, conflicts=%d ===",
                finalResult.pushedCount, finalResult.pulledCount, finalResult.conflictCount)

            return Result.Success(finalResult)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Sync failed with exception")
            markSyncComplete(userId, e.message)
            return Result.Error(DataError.Sync.UNKNOWN)
        }
    }

    /**
     * Pushes all pending local changes to the cloud.
     */
    suspend fun pushLocalChanges(userId: String): Result<SyncResult, DataError.Sync> {
        Timber.tag(TAG).d("Pushing local changes for user: %s", userId)

        var pushedBooks = 0
        var pushedShelves = 0
        var deletedCount = 0

        // Get pending books
        val pendingBooks = bookshelfDao.getPendingSyncBooks()
            .filter { it.ownerId == userId || it.ownerId == null }

        Timber.tag(TAG).d("Found %d pending books", pendingBooks.size)
        updateProgress(SyncPhase.PUSHING_BOOKS, 0, pendingBooks.size)

        // Push books
        pendingBooks.forEachIndexed { index, book ->
            if (isCancelled.get()) return Result.Success(SyncResult(pushedCount = pushedBooks))

            updateProgress(SyncPhase.PUSHING_BOOKS, index + 1, pendingBooks.size)

            if (book.syncStatus == "DELETED") {
                val deleteResult = remoteDataSource.deleteBook(userId, book.id)
                if (deleteResult is Result.Success) {
                    bookshelfDao.deleteBook(book.id)
                    deletedCount++
                }
            } else {
                val uploadResult = remoteDataSource.uploadBook(userId, book.toFirestoreDto())
                if (uploadResult is Result.Success) {
                    bookshelfDao.updateBookSyncStatus(
                        book.id,
                        "SYNCED",
                        timeProvider.currentTimeMillis()
                    )
                    pushedBooks++
                } else if (uploadResult is Result.Error) {
                    Timber.tag(TAG).w("Failed to upload book %s: %s", book.id, uploadResult.error)
                }
            }
        }

        // Get pending shelves
        val pendingShelves = bookshelfDao.getPendingSyncShelves()
            .filter { it.ownerId == userId || it.ownerId == null }

        Timber.tag(TAG).d("Found %d pending shelves", pendingShelves.size)
        updateProgress(SyncPhase.PUSHING_SHELVES, 0, pendingShelves.size)

        // Push shelves
        pendingShelves.forEachIndexed { index, shelf ->
            if (isCancelled.get()) return Result.Success(
                SyncResult(pushedCount = pushedBooks + pushedShelves)
            )

            updateProgress(SyncPhase.PUSHING_SHELVES, index + 1, pendingShelves.size)

            if (shelf.syncStatus == "DELETED") {
                val deleteResult = remoteDataSource.deleteBookshelf(userId, shelf.id)
                if (deleteResult is Result.Success) {
                    bookshelfDao.deleteShelf(shelf.id)
                    deletedCount++
                }
            } else {
                // Get book IDs for this shelf
                val bookIds = bookshelfDao.getBooksForShelf(shelf.id)
                    .first()
                    .map { it.id }

                val uploadResult = remoteDataSource.uploadBookshelf(
                    userId,
                    shelf.toFirestoreDto(bookIds)
                )
                if (uploadResult is Result.Success) {
                    bookshelfDao.updateShelfSyncStatus(
                        shelf.id,
                        "SYNCED",
                        timeProvider.currentTimeMillis()
                    )
                    pushedShelves++
                } else if (uploadResult is Result.Error) {
                    Timber.tag(TAG).w("Failed to upload shelf %s: %s", shelf.id, uploadResult.error)
                }
            }
        }

        return Result.Success(
            SyncResult(
                pushedCount = pushedBooks + pushedShelves,
                deletedCount = deletedCount
            )
        )
    }

    /**
     * Pulls all remote changes since last sync.
     */
    suspend fun pullRemoteChanges(userId: String): Result<SyncResult, DataError.Sync> {
        Timber.tag(TAG).d("Pulling remote changes for user: %s", userId)

        val lastSync = syncDao.getSyncMetadata(userId)?.lastSyncTimestamp ?: 0L
        var pulledBooks = 0
        var pulledShelves = 0

        // Download books modified since last sync
        updateProgress(SyncPhase.PULLING_BOOKS, 0, 0)
        val booksResult = remoteDataSource.downloadBooksSince(userId, lastSync)
        if (booksResult is Result.Error) {
            return Result.Error(booksResult.error)
        }

        val remoteBooks = (booksResult as Result.Success).data
        Timber.tag(TAG).d("Downloaded %d books from cloud", remoteBooks.size)

        remoteBooks.forEach { remoteBook ->
            if (isCancelled.get()) return Result.Success(SyncResult(pulledCount = pulledBooks))

            val localBook = bookshelfDao.getBookById(remoteBook.id)

            if (localBook == null) {
                // New book from cloud - insert locally
                bookshelfDao.upsert(remoteBook.toEntity(userId))
                pulledBooks++
            } else if (localBook.syncStatus == "SYNCED") {
                // Local is synced, update with remote
                bookshelfDao.upsert(remoteBook.toEntity(userId, localBook.cloudId ?: localBook.id))
                pulledBooks++
            } else {
                // Conflict - local has pending changes
                val conflict = SyncConflict(
                    entityId = localBook.id,
                    entityType = EntityType.BOOK,
                    localTimestamp = localBook.lastModifiedAt,
                    remoteTimestamp = remoteBook.lastModifiedAt,
                    localVersion = localBook.version,
                    remoteVersion = remoteBook.version
                )

                val resolution = conflictResolver.resolve(conflict)
                if (resolution != null) {
                    applyResolution(userId, conflict, resolution, remoteBook)
                    pulledBooks++
                } else {
                    conflicts.add(conflict)
                }
            }
        }

        // Download shelves modified since last sync
        updateProgress(SyncPhase.PULLING_SHELVES, 0, 0)
        val shelvesResult = remoteDataSource.downloadBookshelvesSince(userId, lastSync)
        if (shelvesResult is Result.Error) {
            return Result.Error(shelvesResult.error)
        }

        val remoteShelves = (shelvesResult as Result.Success).data
        Timber.tag(TAG).d("Downloaded %d shelves from cloud", remoteShelves.size)

        remoteShelves.forEach { remoteShelf ->
            if (isCancelled.get()) return Result.Success(
                SyncResult(pulledCount = pulledBooks + pulledShelves)
            )

            val localShelf = bookshelfDao.getShelfById(remoteShelf.id)

            if (localShelf == null) {
                // New shelf from cloud - insert locally
                bookshelfDao.upsertShelf(remoteShelf.toEntity(userId))
                pulledShelves++
            } else if (localShelf.syncStatus == "SYNCED") {
                // Local is synced, update with remote
                bookshelfDao.upsertShelf(
                    remoteShelf.toEntity(userId, localShelf.cloudId ?: localShelf.id)
                )
                pulledShelves++
            } else {
                // Conflict - local has pending changes
                val conflict = SyncConflict(
                    entityId = localShelf.id,
                    entityType = EntityType.BOOKSHELF,
                    localTimestamp = localShelf.lastModifiedAt,
                    remoteTimestamp = remoteShelf.lastModifiedAt,
                    localVersion = localShelf.version,
                    remoteVersion = remoteShelf.version
                )

                val resolution = conflictResolver.resolve(conflict)
                if (resolution != null) {
                    applyShelfResolution(userId, conflict, resolution, remoteShelf)
                    pulledShelves++
                } else {
                    conflicts.add(conflict)
                }
            }
        }

        // Update last sync timestamp
        syncDao.updateLastSyncTimestamp(userId, timeProvider.currentTimeMillis())

        return Result.Success(SyncResult(pulledCount = pulledBooks + pulledShelves))
    }

    /**
     * Cancels the current sync operation.
     */
    fun cancel() {
        Timber.tag(TAG).d("Sync cancellation requested")
        isCancelled.set(true)
    }

    /**
     * Gets any unresolved conflicts from the last sync.
     */
    fun getUnresolvedConflicts(): List<SyncConflict> = conflicts.toList()

    private suspend fun applyResolution(
        userId: String,
        conflict: SyncConflict,
        resolution: ConflictResolution,
        remoteBook: uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto
    ) {
        when (resolution) {
            ConflictResolution.KeepLocal -> {
                // Mark local as pending to push
                bookshelfDao.updateBookSyncStatus(
                    conflict.entityId,
                    "PENDING",
                    timeProvider.currentTimeMillis()
                )
            }
            ConflictResolution.KeepRemote, ConflictResolution.LastWriteWins -> {
                // Overwrite local with remote
                bookshelfDao.upsert(remoteBook.toEntity(userId))
            }
            else -> {
                // Skip or merge - mark as conflict
                bookshelfDao.updateBookSyncStatus(
                    conflict.entityId,
                    "CONFLICT",
                    timeProvider.currentTimeMillis()
                )
            }
        }
    }

    private suspend fun applyShelfResolution(
        userId: String,
        conflict: SyncConflict,
        resolution: ConflictResolution,
        remoteShelf: uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto
    ) {
        when (resolution) {
            ConflictResolution.KeepLocal -> {
                bookshelfDao.updateShelfSyncStatus(
                    conflict.entityId,
                    "PENDING",
                    timeProvider.currentTimeMillis()
                )
            }
            ConflictResolution.KeepRemote, ConflictResolution.LastWriteWins -> {
                bookshelfDao.upsertShelf(remoteShelf.toEntity(userId))
            }
            else -> {
                bookshelfDao.updateShelfSyncStatus(
                    conflict.entityId,
                    "CONFLICT",
                    timeProvider.currentTimeMillis()
                )
            }
        }
    }

    private suspend fun markSyncComplete(userId: String, error: String?) {
        syncDao.updateSyncInProgress(userId, false)
        if (error != null) {
            syncDao.updateLastSyncError(userId, error)
        } else {
            syncDao.updateLastSyncError(userId, null)
            syncDao.updateLastSyncTimestamp(userId, timeProvider.currentTimeMillis())
        }
    }

    private fun updateProgress(phase: SyncPhase, current: Int, total: Int) {
        _progress.value = SyncProgress(phase, current, total)
    }

    companion object {
        private const val TAG = "SyncEngine"
    }
}
