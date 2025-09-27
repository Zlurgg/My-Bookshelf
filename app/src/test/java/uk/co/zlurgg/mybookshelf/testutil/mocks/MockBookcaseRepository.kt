package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository

/**
 * Reusable mock BookcaseRepository for testing.
 * Provides configurable behavior for testing different scenarios.
 */
class MockBookcaseRepository : BookcaseRepository {

    // Configuration properties
    var shouldThrowException = false
    var shelvesToReturn = emptyList<Bookshelf>()
    var shelfByIdToReturn: Bookshelf? = null
    var bookCountsToReturn = emptyMap<String, Int>()

    // Tracking properties
    var addShelfCalled = false
    var removeShelfCalled = false
    var updateShelfCalled = false
    var lastAddedShelf: Bookshelf? = null
    var lastRemovedShelfId: String? = null
    var lastUpdatedShelf: Bookshelf? = null

    override fun getAllShelves(): Flow<List<Bookshelf>> = flowOf(shelvesToReturn)

    override fun getBookCountForShelf(shelfId: String): Flow<Int> =
        flowOf(bookCountsToReturn[shelfId] ?: 0)

    override suspend fun getShelfById(shelfId: String): Bookshelf? {
        if (shouldThrowException) throw RuntimeException("Test exception")
        return shelfByIdToReturn
    }

    override suspend fun addShelf(shelf: Bookshelf) {
        if (shouldThrowException) throw RuntimeException("Test exception")
        addShelfCalled = true
        lastAddedShelf = shelf
    }

    override suspend fun removeShelf(shelfId: String) {
        if (shouldThrowException) throw RuntimeException("Test exception")
        removeShelfCalled = true
        lastRemovedShelfId = shelfId
    }

    override suspend fun updateShelf(shelf: Bookshelf) {
        if (shouldThrowException) throw RuntimeException("Test exception")
        updateShelfCalled = true
        lastUpdatedShelf = shelf
    }

    // Helper methods for test setup
    fun reset() {
        shouldThrowException = false
        shelvesToReturn = emptyList()
        shelfByIdToReturn = null
        bookCountsToReturn = emptyMap()
        addShelfCalled = false
        removeShelfCalled = false
        updateShelfCalled = false
        lastAddedShelf = null
        lastRemovedShelfId = null
        lastUpdatedShelf = null
    }

    fun configureShelves(shelves: List<Bookshelf>) {
        shelvesToReturn = shelves
    }

    fun configureBookCounts(counts: Map<String, Int>) {
        bookCountsToReturn = counts
    }
}