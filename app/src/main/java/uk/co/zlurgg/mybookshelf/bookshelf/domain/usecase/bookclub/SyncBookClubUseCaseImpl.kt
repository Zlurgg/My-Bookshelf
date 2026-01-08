package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of SyncBookClubUseCase.
 * Delegates to BookClubRepository for the actual sync operation.
 */
class SyncBookClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository,
) : SyncBookClubUseCase {
    override suspend fun execute(
        clubCode: String,
        localShelfId: String,
    ): Result<SyncResult, DataError.Sync> {
        return bookClubRepository.syncBooksFromClub(clubCode, localShelfId)
    }
}
