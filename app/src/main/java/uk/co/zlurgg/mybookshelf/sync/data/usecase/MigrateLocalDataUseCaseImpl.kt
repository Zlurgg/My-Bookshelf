package uk.co.zlurgg.mybookshelf.sync.data.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.MigrationResult
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.MigrateLocalDataUseCase

/**
 * Implementation of MigrateLocalDataUseCase.
 *
 * Migrates orphan local data to a user account when they sign in
 * for the first time.
 */
class MigrateLocalDataUseCaseImpl(
    private val bookshelfDao: BookshelfDao,
    private val syncScheduler: SyncSchedulerService
) : MigrateLocalDataUseCase {

    override suspend fun execute(userId: String): Result<MigrationResult, DataError.Sync> {
        Timber.tag(TAG).d("=== MIGRATION START for user: %s ===", userId)

        return try {
            // Step 1: Count orphan entities
            val orphanBookCount = bookshelfDao.countOrphanBooks()
            val orphanShelfCount = bookshelfDao.countOrphanShelves()

            Timber.tag(TAG).d(
                "Found orphan entities - Books: %d, Shelves: %d",
                orphanBookCount,
                orphanShelfCount
            )

            // If no orphan data, no migration needed
            if (orphanBookCount == 0 && orphanShelfCount == 0) {
                Timber.tag(TAG).d("No orphan data to migrate")
                return Result.Success(MigrationResult.NO_MIGRATION_NEEDED)
            }

            // Step 2: Assign owner to orphan entities
            Timber.tag(TAG).d("Assigning owner to orphan books...")
            bookshelfDao.assignOwnerToOrphanBooks(userId)

            Timber.tag(TAG).d("Assigning owner to orphan shelves...")
            bookshelfDao.assignOwnerToOrphanShelves(userId)

            // Step 3: Mark all entities as pending sync
            Timber.tag(TAG).d("Marking all entities as pending sync...")
            bookshelfDao.markAllBooksPending(userId)
            bookshelfDao.markAllShelvesPending(userId)

            // Step 4: Trigger immediate sync
            Timber.tag(TAG).d("Triggering immediate sync...")
            syncScheduler.triggerImmediateSync()

            val result = MigrationResult(
                booksAssigned = orphanBookCount,
                shelvesAssigned = orphanShelfCount,
                hadDataToMigrate = true,
                syncTriggered = true
            )

            Timber.tag(TAG).d(
                "=== MIGRATION COMPLETE === Books: %d, Shelves: %d",
                result.booksAssigned,
                result.shelvesAssigned
            )

            Result.Success(result)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Migration failed")
            Result.Error(DataError.Sync.MIGRATION_FAILED)
        }
    }

    companion object {
        private const val TAG = "Migration"
    }
}
