package uk.co.zlurgg.mybookshelf.sync.domain.repository

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.onSuccess
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class SyncingBookRepository(
    private val delegate: BookRepository,
    private val syncScheduler: SyncSchedulerService,
) : BookRepository by delegate {

    override suspend fun upsertBook(book: Book): Result<Unit, DataError.Local> {
        return delegate.upsertBook(book).onSuccess {
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: upsertBook")
            syncScheduler.triggerImmediateSync()
        }
    }

    override suspend fun deleteBook(bookId: String): Result<Unit, DataError.Local> {
        return delegate.deleteBook(bookId).onSuccess {
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: deleteBook")
            syncScheduler.triggerImmediateSync()
        }
    }
}
