package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository

/**
 * Reusable mock BookcaseRepository for testing.
 * Provides configurable behavior for testing different scenarios.
 */
class MockBookcaseRepository : BookcaseRepository {

    private val shelves = mutableMapOf<String, Bookshelf>()

    // Configuration properties
    var shouldThrowException = false
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
        if (shouldThrowException) throw RuntimeException("Test exception")
        emit(shelvesToReturn)
    }

    override fun getBookCountForShelf(shelfId: String): Flow<Int> =
        flowOf(bookCountsToReturn[shelfId] ?: 0)

    override suspend fun getShelfById(shelfId: String): Bookshelf? {
        if (shouldThrowException) throw RuntimeException("Test exception")
        // Map-based lookup takes priority for multi-shelf tests
        return shelfById[shelfId] ?: shelfByIdToReturn
    }

    override suspend fun addShelf(shelf: Bookshelf) {
        addShelfCalled = true
        addShelfCallCount++
        lastAddedShelf = shelf

        if (shouldThrowException) throw RuntimeException("Test exception")

        shelves[shelf.id] = shelf
    }

    override suspend fun removeShelf(shelfId: String) {
        removeShelfCalled = true
        removeShelfCallCount++
        lastRemovedShelfId = shelfId

        if (shouldThrowException) throw RuntimeException("Test exception")

        shelves.remove(shelfId)
    }

    override suspend fun hardDeleteShelf(shelfId: String) {
        hardDeleteShelfCalled = true
        hardDeleteShelfCallCount++
        lastHardDeletedShelfId = shelfId

        if (shouldThrowException) throw RuntimeException("Test exception")

        shelves.remove(shelfId)
    }

    override suspend fun updateShelf(shelf: Bookshelf) {
        if (shouldThrowException) throw RuntimeException("Test exception")
        updateShelfCalled = true
        lastUpdatedShelf = shelf
    }

    override suspend fun addSystemShelf(shelf: Bookshelf) {
        addSystemShelfCalled = true
        addSystemShelfCallCount++
        lastAddedSystemShelf = shelf

        if (shouldThrowException) throw RuntimeException("Test exception")

        shelves[shelf.id] = shelf
    }

    // Helper methods for test setup
    fun reset() {
        shelves.clear()
        shouldThrowException = false
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