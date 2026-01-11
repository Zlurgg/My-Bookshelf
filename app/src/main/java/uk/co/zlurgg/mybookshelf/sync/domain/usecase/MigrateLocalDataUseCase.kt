package uk.co.zlurgg.mybookshelf.sync.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.MigrationResult

/**
 * Use case for migrating local data to the current user's account.
 *
 * This should be called when a user signs in and chooses to import
 * their guest data. It assigns all orphan entities (books and shelves
 * with no owner) to the current signed-in user and triggers an initial sync.
 */
interface MigrateLocalDataUseCase {

    /**
     * Migrates local data to the current user's account.
     *
     * Steps:
     * 1. Get current user ID from CurrentUserProvider
     * 2. Count orphan entities (no owner)
     * 3. Assign userId to all orphan books
     * 4. Assign userId to all orphan shelves
     * 5. Mark all as PENDING sync
     * 6. Trigger immediate sync
     *
     * @return Result containing migration statistics or error
     */
    suspend operator fun invoke(): Result<MigrationResult, DataError.Sync>
}
