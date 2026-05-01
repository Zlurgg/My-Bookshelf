package uk.co.zlurgg.mybookshelf.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncSchedulerService

class SyncingBookshelfRepositoryTest {

    private val mockSyncScheduler = MockSyncSchedulerService()

    private val delegate = object : BookshelfRepository {
        var addBookToShelfResult: Result<Unit, DataError.Local> = Result.Success(Unit)
        var removeBookFromShelfResult: Result<Unit, DataError.Local> = Result.Success(Unit)

        override suspend fun addBookToShelf(shelfId: String, bookId: String) = addBookToShelfResult
        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) = removeBookFromShelfResult
        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> = flowOf(emptyList())
        override fun isBookInAnyShelf(bookId: String): Flow<Boolean> = flowOf(false)
        override fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean> = flowOf(false)
        override fun getShelvesForBook(bookId: String): Flow<List<String>> = flowOf(emptyList())
    }

    private val repository = SyncingBookshelfRepository(delegate, mockSyncScheduler)

    @Test
    fun `addBookToShelf success triggers sync`() = runTest {
        val result = repository.addBookToShelf("shelf-1", "book-1")

        assertTrue(result is Result.Success)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `addBookToShelf error does not trigger sync`() = runTest {
        delegate.addBookToShelfResult = Result.Error(DataError.Local.UNKNOWN)

        val result = repository.addBookToShelf("shelf-1", "book-1")

        assertTrue(result is Result.Error)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `removeBookFromShelf success triggers sync`() = runTest {
        val result = repository.removeBookFromShelf("shelf-1", "book-1")

        assertTrue(result is Result.Success)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `removeBookFromShelf error does not trigger sync`() = runTest {
        delegate.removeBookFromShelfResult = Result.Error(DataError.Local.UNKNOWN)

        val result = repository.removeBookFromShelf("shelf-1", "book-1")

        assertTrue(result is Result.Error)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `getBooksForShelf delegates without triggering sync`() = runTest {
        repository.getBooksForShelf("shelf-1")

        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }
}
