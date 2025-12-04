package uk.co.zlurgg.mybookshelf.sync.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.MigrationResult

/**
 * Use case for migrating local data to a user account.
 *
 * This should be called after a user signs in for the first time.
 * It assigns all orphan entities (books and shelves with no owner)
 * to the signed-in user and triggers an initial sync.
 */
interface MigrateLocalDataUseCase {

    /**
     * Migrates local data to the specified user account.
     *
     * Steps:
     * 1. Count orphan entities (no owner)
     * 2. Assign userId to all orphan books
     * 3. Assign userId to all orphan shelves
     * 4. Mark all as PENDING sync
     * 5. Trigger immediate sync
     *
     * @param userId The user ID to assign to orphan entities
     * @return Result containing migration statistics or error
     */
    suspend fun execute(userId: String): Result<MigrationResult, DataError.Sync>
}
