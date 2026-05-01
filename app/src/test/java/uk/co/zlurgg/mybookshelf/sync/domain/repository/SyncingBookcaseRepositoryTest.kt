package uk.co.zlurgg.mybookshelf.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncSchedulerService

class SyncingBookcaseRepositoryTest {

    private val mockSyncScheduler = MockSyncSchedulerService()

    private val delegate = object : BookcaseRepository {
        var addShelfResult: Result<Unit, DataError.Local> = Result.Success(Unit)
        var removeShelfResult: Result<Unit, DataError.Local> = Result.Success(Unit)
        var updateShelfResult: Result<Unit, DataError.Local> = Result.Success(Unit)
        var hardDeleteShelfResult: Result<Unit, DataError.Local> = Result.Success(Unit)
        var addSystemShelfResult: Result<Unit, DataError.Local> = Result.Success(Unit)
        var clearUserDataResult: Result<Int, DataError.Local> = Result.Success(0)

        override fun getAllShelves(): Flow<List<Bookshelf>> = flowOf(emptyList())
        override fun getBookCountForShelf(shelfId: String): Flow<Int> = flowOf(0)
        override suspend fun getShelfById(shelfId: String) = Result.Success(null)
        override suspend fun addShelf(shelf: Bookshelf) = addShelfResult
        override suspend fun removeShelf(shelfId: String) = removeShelfResult
        override suspend fun updateShelf(shelf: Bookshelf) = updateShelfResult
        override suspend fun hardDeleteShelf(shelfId: String) = hardDeleteShelfResult
        override suspend fun addSystemShelf(shelf: Bookshelf) = addSystemShelfResult
        override suspend fun clearUserData(userId: String) = clearUserDataResult
        override suspend fun revertUserDataToGuest(userId: String): Result<Unit, DataError.Local> = Result.Success(Unit)
        override suspend fun revertOrphanedDataToGuest(): Result<Unit, DataError.Local> = Result.Success(Unit)
    }

    private val repository = SyncingBookcaseRepository(delegate, mockSyncScheduler)

    @Test
    fun `addShelf success triggers sync`() = runTest {
        val shelf = TestShelfBuilder().build()

        val result = repository.addShelf(shelf)

        assertTrue(result is Result.Success)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `addShelf error does not trigger sync`() = runTest {
        delegate.addShelfResult = Result.Error(DataError.Local.UNKNOWN)
        val shelf = TestShelfBuilder().build()

        val result = repository.addShelf(shelf)

        assertTrue(result is Result.Error)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `removeShelf success triggers sync`() = runTest {
        val result = repository.removeShelf("shelf-1")

        assertTrue(result is Result.Success)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `updateShelf success triggers sync`() = runTest {
        val shelf = TestShelfBuilder().build()

        val result = repository.updateShelf(shelf)

        assertTrue(result is Result.Success)
        assertEquals(1, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `hardDeleteShelf does not trigger sync`() = runTest {
        val result = repository.hardDeleteShelf("shelf-1")

        assertTrue(result is Result.Success)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `addSystemShelf does not trigger sync`() = runTest {
        val shelf = TestShelfBuilder().build()

        val result = repository.addSystemShelf(shelf)

        assertTrue(result is Result.Success)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `clearUserData does not trigger sync`() = runTest {
        val result = repository.clearUserData("user-1")

        assertTrue(result is Result.Success)
        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }

    @Test
    fun `getAllShelves delegates without triggering sync`() = runTest {
        repository.getAllShelves()

        assertEquals(0, mockSyncScheduler.triggerImmediateSyncCallCount)
    }
}
