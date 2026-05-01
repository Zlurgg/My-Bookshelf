package uk.co.zlurgg.mybookshelf.sync.domain.repository

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class SyncingBookshelfRepository(
    private val delegate: BookshelfRepository,
    private val syncScheduler: SyncSchedulerService,
) : BookshelfRepository by delegate {

    override suspend fun addBookToShelf(
        shelfId: String,
        bookId: String,
    ): Result<Unit, DataError.Local> {
        return delegate.addBookToShelf(shelfId, bookId).onSuccess {
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: addBookToShelf")
            syncScheduler.triggerImmediateSync()
        }
    }

    override suspend fun removeBookFromShelf(
        shelfId: String,
        bookId: String,
    ): Result<Unit, DataError.Local> {
        return delegate.removeBookFromShelf(shelfId, bookId).onSuccess {
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: removeBookFromShelf")
            syncScheduler.triggerImmediateSync()
        }
    }
}
