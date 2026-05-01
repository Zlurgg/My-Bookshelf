package uk.co.zlurgg.mybookshelf.sync.domain.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncSchedulerService

class SyncingBookRepositoryTest {

    private val mockSyncScheduler = MockSyncSchedulerService()

    private val delegate = object : BookRepository {
        var upsertBookResult: Result<Unit, DataError.Local> = Result.Success(Unit)
        var deleteBookResult: Result<Unit, DataError.Local> = Result.Success(Unit)
        var upsertSystemBookResult: Result<Unit, DataError.Local> = Result.Success(Unit)

        override suspend fun getBookById(bookId: String): Result<Book?, DataError.Local> =
            Result.Success(null)
        override suspend fun upsertBook(book: Book) = upsertBookResult
        override suspend fun deleteBook(bookId: String) = deleteBookResult
        override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> =
            Result.Success(null)
        override suspend fun upsertSystemBook(book: Book) = upsertSystemBookResult
    }

    private val repository = SyncingBookRepository(delegate, mockSyncScheduler)

    @Test
    fun `upsertBook success triggers sync`() = runTest {
        val book = TestBookBuilder().build()

        val result = repository.upsertBook(book)

        assertTrue(result is Result.Success)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `upsertBook error does not trigger sync`() = runTest {
        delegate.upsertBookResult = Result.Error(DataError.Local.UNKNOWN)
        val book = TestBookBuilder().build()

        val result = repository.upsertBook(book)

        assertTrue(result is Result.Error)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `deleteBook success triggers sync`() = runTest {
        val result = repository.deleteBook("book-1")

        assertTrue(result is Result.Success)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `deleteBook error does not trigger sync`() = runTest {
        delegate.deleteBookResult = Result.Error(DataError.Local.UNKNOWN)

        val result = repository.deleteBook("book-1")

        assertTrue(result is Result.Error)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `upsertSystemBook does not trigger sync`() = runTest {
        val book = TestBookBuilder().build()

        val result = repository.upsertSystemBook(book)

        assertTrue(result is Result.Success)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }
}
