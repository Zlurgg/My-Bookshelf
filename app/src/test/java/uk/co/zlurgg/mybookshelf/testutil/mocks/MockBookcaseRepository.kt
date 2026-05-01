package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Reusable mock BookcaseRepository for testing.
 * Provides configurable behavior for testing different scenarios.
 */
class MockBookcaseRepository : BookcaseRepository {

    private val shelves = mutableMapOf<String, Bookshelf>()

    // Configuration properties
    var errorToReturn: DataError.Local? = null
    var shouldThrowFlowException = false // For Flow methods that don't return Result
    var shelvesToReturn = emptyList<Bookshelf>()
    var shelfByIdToReturn: Bookshelf? = null
    var shelfById: Map<String, Bookshelf> = emptyMap() // Map-based lookup (takes priority over shelfByIdToReturn)
    var bookCountsToReturn = emptyMap<String, Int>()

    // Tracking properties
    var addShelfCalled = false
    var addSystemShelfCalled = false
    var removeShelfCalled = false
    var hardDeleteShelfCalled = false
    var updateShelfCalled = false
    var addShelfCallCount = 0
    var addSystemShelfCallCount = 0
    var removeShelfCallCount = 0
    var hardDeleteShelfCallCount = 0
    var lastAddedShelf: Bookshelf? = null
    var lastAddedSystemShelf: Bookshelf? = null
    var lastRemovedShelfId: String? = null
    var lastHardDeletedShelfId: String? = null
    var lastUpdatedShelf: Bookshelf? = null

    override fun getAllShelves(): Flow<List<Bookshelf>> = flow {
        if (shouldThrowFlowException) {
            throw RuntimeException("Test exception from Flow")
        }
        emit(shelvesToReturn)
    }

    override fun getBookCountForShelf(shelfId: String): Flow<Int> =
        flowOf(bookCountsToReturn[shelfId] ?: 0)

    override suspend fun getShelfById(shelfId: String): Result<Bookshelf?, DataError.Local> {
        errorToReturn?.let { return Result.Error(it) }
        // Map-based lookup takes priority for multi-shelf tests
        return Result.Success(shelfById[shelfId] ?: shelfByIdToReturn)
    }

    override suspend fun addShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        addShelfCalled = true
        addShelfCallCount++
        lastAddedShelf = shelf

        errorToReturn?.let { return Result.Error(it) }

        shelves[shelf.id] = shelf
        return Result.Success(Unit)
    }

    override suspend fun removeShelf(shelfId: String): Result<Unit, DataError.Local> {
        removeShelfCalled = true
        removeShelfCallCount++
        lastRemovedShelfId = shelfId

        errorToReturn?.let { return Result.Error(it) }

        shelves.remove(shelfId)
        return Result.Success(Unit)
    }

    override suspend fun hardDeleteShelf(shelfId: String): Result<Unit, DataError.Local> {
        hardDeleteShelfCalled = true
        hardDeleteShelfCallCount++
        lastHardDeletedShelfId = shelfId

        errorToReturn?.let { return Result.Error(it) }

        shelves.remove(shelfId)
        return Result.Success(Unit)
    }

    override suspend fun updateShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        updateShelfCalled = true
        lastUpdatedShelf = shelf

        errorToReturn?.let { return Result.Error(it) }

        return Result.Success(Unit)
    }

    override suspend fun addSystemShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        addSystemShelfCalled = true
        addSystemShelfCallCount++
        lastAddedSystemShelf = shelf

        errorToReturn?.let { return Result.Error(it) }

        shelves[shelf.id] = shelf
        return Result.Success(Unit)
    }

    override suspend fun clearUserData(userId: String): Result<Int, DataError.Local> {
        clearUserDataCalled = true
        lastClearedUserId = userId

        errorToReturn?.let { return Result.Error(it) }

        val count = shelves.size
        shelves.clear()
        return Result.Success(count)
    }

    // Tracking properties for clearUserData
    var clearUserDataCalled = false
    var lastClearedUserId: String? = null

    // Tracking properties for revert methods
    var revertUserDataToGuestCalled = false
    var lastRevertedUserId: String? = null
    var revertOrphanedDataToGuestCalled = false
    var revertErrorToReturn: DataError.Local? = null

    override suspend fun revertUserDataToGuest(userId: String): Result<Unit, DataError.Local> {
        revertUserDataToGuestCalled = true
        lastRevertedUserId = userId
        revertErrorToReturn?.let { return Result.Error(it) }
        return Result.Success(Unit)
    }

    override suspend fun revertOrphanedDataToGuest(): Result<Unit, DataError.Local> {
        revertOrphanedDataToGuestCalled = true
        revertErrorToReturn?.let { return Result.Error(it) }
        return Result.Success(Unit)
    }

    // Helper methods for test setup
    fun reset() {
        shelves.clear()
        errorToReturn = null
        shouldThrowFlowException = false
        shelvesToReturn = emptyList()
        shelfByIdToReturn = null
        shelfById = emptyMap()
        bookCountsToReturn = emptyMap()
        addShelfCalled = false
        addSystemShelfCalled = false
        removeShelfCalled = false
        hardDeleteShelfCalled = false
        updateShelfCalled = false
        addShelfCallCount = 0
        addSystemShelfCallCount = 0
        removeShelfCallCount = 0
        hardDeleteShelfCallCount = 0
        lastAddedShelf = null
        lastAddedSystemShelf = null
        lastRemovedShelfId = null
        lastHardDeletedShelfId = null
        lastUpdatedShelf = null
        clearUserDataCalled = false
        lastClearedUserId = null
        revertUserDataToGuestCalled = false
        lastRevertedUserId = null
        revertOrphanedDataToGuestCalled = false
        revertErrorToReturn = null
    }

    fun configureShelves(shelves: List<Bookshelf>) {
        shelvesToReturn = shelves
    }

    fun configureBookCounts(counts: Map<String, Int>) {
        bookCountsToReturn = counts
    }

    fun addShelfForTest(shelf: Bookshelf) {
        shelves[shelf.id] = shelf
    }

    fun hasShelf(shelfId: String): Boolean = shelves.containsKey(shelfId)

    fun getShelf(shelfId: String): Bookshelf? = shelves[shelfId]
}
