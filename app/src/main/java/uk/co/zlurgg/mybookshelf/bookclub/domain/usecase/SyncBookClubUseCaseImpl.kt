package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of SyncBookClubUseCase.
 * Delegates to BookClubRepository for the actual sync operation.
 */
class SyncBookClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : SyncBookClubUseCase {

    override suspend operator fun invoke(
        clubCode: String,
        localShelfId: String
    ): Result<SyncResult, DataError.Sync> {
        return bookClubRepository.syncBooksFromClub(clubCode, localShelfId)
    }
}
