package uk.co.zlurgg.mybookshelf.sync.domain.repository

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class SyncingBookcaseRepository(
    private val delegate: BookcaseRepository,
    private val syncScheduler: SyncSchedulerService,
) : BookcaseRepository by delegate {

    override suspend fun addShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return delegate.addShelf(shelf).onSuccess {
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: addShelf")
            syncScheduler.triggerImmediateSync()
        }
    }

    override suspend fun removeShelf(shelfId: String): Result<Unit, DataError.Local> {
        return delegate.removeShelf(shelfId).onSuccess {
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: removeShelf")
            syncScheduler.triggerImmediateSync()
        }
    }

    override suspend fun updateShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return delegate.updateShelf(shelf).onSuccess {
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: updateShelf")
            syncScheduler.triggerImmediateSync()
        }
    }
}
