package uk.co.zlurgg.mybookshelf.sync.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.model.GuestDataInfo
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository

/**
 * Implementation of HasGuestDataUseCase.
 *
 * Checks the database for orphan (guest) data that could be imported
 * to a user's account.
 */
class HasGuestDataUseCaseImpl(
    private val syncRepository: SyncRepository
) : HasGuestDataUseCase {

    override suspend operator fun invoke(): GuestDataInfo {
        return when (val result = syncRepository.getOrphanDataCounts()) {
            is Result.Success -> result.data
            is Result.Error -> GuestDataInfo(bookCount = 0, shelfCount = 0)
        }
    }
}
