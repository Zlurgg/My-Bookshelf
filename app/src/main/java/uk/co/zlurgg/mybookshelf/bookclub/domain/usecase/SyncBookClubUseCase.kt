package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for syncing books from a book club to the local shelf.
 * Fetches new books added by other members and removes deleted ones.
 */
interface SyncBookClubUseCase {
    /**
     * Syncs books from the remote book club to the local shelf.
     *
     * @param clubCode The book club code
     * @param localShelfId The local shelf ID to sync to
     * @return SyncResult with number of books added/removed
     */
    suspend operator fun invoke(clubCode: String, localShelfId: String): Result<SyncResult, DataError.Sync>
}
