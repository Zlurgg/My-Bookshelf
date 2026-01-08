package uk.co.zlurgg.mybookshelf.sync.domain.usecase

import uk.co.zlurgg.mybookshelf.sync.domain.model.GuestDataInfo

/**
 * Use case for checking if there's any guest (orphan) data in the local database.
 *
 * This should be called after a user signs in to determine if we need to
 * ask them about importing existing guest data to their account.
 */
interface HasGuestDataUseCase {
    /**
     * Checks for orphan data (books/shelves with no owner).
     *
     * @return GuestDataInfo containing counts of orphan books and shelves
     */
    suspend fun execute(): GuestDataInfo
}
