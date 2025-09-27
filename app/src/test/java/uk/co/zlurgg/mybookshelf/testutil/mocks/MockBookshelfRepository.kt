package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository

class MockBookshelfRepository : BookshelfRepository {

    private val shelfBookRelations = mutableMapOf<String, MutableSet<String>>() // shelfId -> bookIds
    private val configuredBooks = mutableMapOf<String, Book>() // bookId -> Book

    var shouldThrowException = false
    var addBookToShelfCallCount = 0
    var removeBookFromShelfCallCount = 0
    var lastAddedBookId: String? = null
    var lastAddedShelfId: String? = null
    var lastRemovedBookId: String? = null
    var lastRemovedShelfId: String? = null

    fun reset() {
        shelfBookRelations.clear()
        configuredBooks.clear()
        shouldThrowException = false
        addBookToShelfCallCount = 0
        removeBookFromShelfCallCount = 0
        lastAddedBookId = null
        lastAddedShelfId = null
        lastRemovedBookId = null
        lastRemovedShelfId = null
    }

    fun configureBook(book: Book) {
        configuredBooks[book.id] = book
    }

    fun configureShelfWithBooks(shelfId: String, bookIds: List<String>) {
        shelfBookRelations[shelfId] = bookIds.toMutableSet()
    }

    fun getShelfBookRelations(): Map<String, Set<String>> {
        return shelfBookRelations.mapValues { it.value.toSet() }
    }

    override suspend fun addBookToShelf(shelfId: String, bookId: String) {
        addBookToShelfCallCount++
        lastAddedShelfId = shelfId
        lastAddedBookId = bookId

        if (shouldThrowException) {
            throw RuntimeException("Mock repository error")
        }

        shelfBookRelations.getOrPut(shelfId) { mutableSetOf() }.add(bookId)
    }

    override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
        removeBookFromShelfCallCount++
        lastRemovedShelfId = shelfId
        lastRemovedBookId = bookId

        if (shouldThrowException) {
            throw RuntimeException("Mock repository error")
        }

        shelfBookRelations[shelfId]?.remove(bookId)
    }

    override fun getBooksForShelf(shelfId: String): Flow<List<Book>> {
        val bookIds = shelfBookRelations[shelfId] ?: emptySet()
        val books = bookIds.mapNotNull { configuredBooks[it] }
        return flowOf(books)
    }

    override fun isBookInAnyShelf(bookId: String): Flow<Boolean> {
        val isInAnyShelf = shelfBookRelations.values.any { it.contains(bookId) }
        return flowOf(isInAnyShelf)
    }

    override fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean> {
        val isOnShelf = shelfBookRelations[shelfId]?.contains(bookId) == true
        return flowOf(isOnShelf)
    }

    override fun getShelvesForBook(bookId: String): Flow<List<String>> {
        val shelfIds = shelfBookRelations
            .filterValues { it.contains(bookId) }
            .keys
            .toList()
        return flowOf(shelfIds)
    }
}