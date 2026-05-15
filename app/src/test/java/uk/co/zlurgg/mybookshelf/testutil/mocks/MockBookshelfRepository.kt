package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class MockBookshelfRepository : BookshelfRepository {

    private val shelfBookRelations = mutableMapOf<String, MutableSet<String>>() // shelfId -> bookIds
    private val configuredBooks = mutableMapOf<String, Book>() // bookId -> Book
    private val addedByUserIds = mutableMapOf<String, String>() // "shelfId:bookId" -> userId

    var errorToReturn: DataError.Local? = null
    var addBookToShelfCallCount = 0
    var removeBookFromShelfCallCount = 0
    var lastAddedBookId: String? = null
    var lastAddedShelfId: String? = null
    var lastAddedByUserId: String? = null
    var lastRemovedBookId: String? = null
    var lastRemovedShelfId: String? = null

    fun reset() {
        shelfBookRelations.clear()
        configuredBooks.clear()
        addedByUserIds.clear()
        errorToReturn = null
        addBookToShelfCallCount = 0
        removeBookFromShelfCallCount = 0
        lastAddedBookId = null
        lastAddedShelfId = null
        lastAddedByUserId = null
        lastRemovedBookId = null
        lastRemovedShelfId = null
    }

    fun configureAddedByUserId(shelfId: String, bookId: String, userId: String) {
        addedByUserIds["$shelfId:$bookId"] = userId
    }

    fun configureBook(book: Book) {
        configuredBooks[book.id] = book
    }

    fun configureShelfWithBooks(shelfId: String, bookIds: List<String>) {
        shelfBookRelations[shelfId] = bookIds.toMutableSet()
    }

    fun configureBooksForShelf(shelfId: String, books: List<Book>) {
        books.forEach { configureBook(it) }
        configureShelfWithBooks(shelfId, books.map { it.id })
    }

    fun getShelfBookRelations(): Map<String, Set<String>> {
        return shelfBookRelations.mapValues { it.value.toSet() }
    }

    override suspend fun addBookToShelf(
        shelfId: String,
        bookId: String,
        addedByUserId: String?,
    ): Result<Unit, DataError.Local> {
        addBookToShelfCallCount++
        lastAddedShelfId = shelfId
        lastAddedBookId = bookId
        lastAddedByUserId = addedByUserId

        errorToReturn?.let { return Result.Error(it) }

        shelfBookRelations.getOrPut(shelfId) { mutableSetOf() }.add(bookId)
        if (addedByUserId != null) {
            addedByUserIds["$shelfId:$bookId"] = addedByUserId
        }
        return Result.Success(Unit)
    }

    override suspend fun removeBookFromShelf(shelfId: String, bookId: String): Result<Unit, DataError.Local> {
        removeBookFromShelfCallCount++
        lastRemovedShelfId = shelfId
        lastRemovedBookId = bookId

        errorToReturn?.let { return Result.Error(it) }

        shelfBookRelations[shelfId]?.remove(bookId)
        return Result.Success(Unit)
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

    override suspend fun getAddedByUserId(shelfId: String, bookId: String): Result<String?, DataError.Local> {
        errorToReturn?.let { return Result.Error(it) }
        return Result.Success(addedByUserIds["$shelfId:$bookId"])
    }
}
