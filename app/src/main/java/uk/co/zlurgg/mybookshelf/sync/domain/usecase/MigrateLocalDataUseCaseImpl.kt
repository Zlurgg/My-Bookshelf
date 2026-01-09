package uk.co.zlurgg.mybookshelf.sync.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.MigrationResult
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Implementation of MigrateLocalDataUseCase.
 *
 * Migrates orphan local data to the current user's account when they
 * choose to import their guest data after signing in.
 */
class MigrateLocalDataUseCaseImpl(
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncSchedulerService,
    private val currentUserProvider: CurrentUserProvider
) : MigrateLocalDataUseCase {

    override suspend fun execute(): Result<MigrationResult, DataError.Sync> {
        val userId = currentUserProvider.getCurrentUserId()
            ?: return Result.Error(DataError.Sync.MIGRATION_FAILED)

        Timber.tag(TAG).d("Starting migration for user: %s", userId)

        // Perform migration via repository
        val migrationResult = when (val result = syncRepository.migrateOrphanData(userId)) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }

        // If data was migrated, trigger immediate sync
        return if (migrationResult.hadDataToMigrate) {
            Timber.tag(TAG).d("Triggering immediate sync...")
            syncScheduler.triggerImmediateSync()

            Timber.tag(TAG).d(
                "=== MIGRATION COMPLETE === Books: %d, Shelves: %d",
                migrationResult.booksAssigned,
                migrationResult.shelvesAssigned
            )

            Result.Success(migrationResult.copy(syncTriggered = true))
        } else {
            Timber.tag(TAG).d("No data to migrate")
            Result.Success(migrationResult)
        }
    }

    companion object {
        private const val TAG = "Migration"
    }
}
