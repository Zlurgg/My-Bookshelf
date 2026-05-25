package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class MockBookRepository : BookRepository {

    private val books = mutableMapOf<String, Book>()
    private val personalBooksFlow = MutableStateFlow<List<Book>>(emptyList())

    var errorToReturn: DataError.Local? = null
    var remoteErrorToReturn: DataError.Remote? = null
    var upsertBookCallCount = 0
    var upsertSystemBookCallCount = 0
    var getBookByIdCallCount = 0
    var deleteBooksCallCount = 0
    var lastUpsertedBook: Book? = null
    var lastUpsertedSystemBook: Book? = null
    var lastQueriedBookId: String? = null
    var lastDeletedBookIds: List<String> = emptyList()

    private val nonRemovableBookIdsFlow = MutableStateFlow<Set<String>>(emptySet())

    fun reset() {
        books.clear()
        errorToReturn = null
        remoteErrorToReturn = null
        upsertBookCallCount = 0
        upsertSystemBookCallCount = 0
        getBookByIdCallCount = 0
        deleteBooksCallCount = 0
        updateDescriptionCallCount = 0
        lastUpsertedBook = null
        lastUpsertedSystemBook = null
        lastQueriedBookId = null
        lastDeletedBookIds = emptyList()
        lastUpdatedDescriptionBookId = null
        lastUpdatedDescription = null
        nonRemovableBookIdsFlow.value = emptySet()
    }

    fun addBook(book: Book) {
        books[book.id] = book
    }

    fun getAllBooks(): List<Book> = books.values.toList()

    /** Direct access for test verification - bypasses Result wrapping */
    fun getStoredBook(bookId: String): Book? = books[bookId]

    override suspend fun getBookById(bookId: String): Result<Book?, DataError.Local> {
        getBookByIdCallCount++
        lastQueriedBookId = bookId
        errorToReturn?.let { return Result.Error(it) }
        return Result.Success(books[bookId])
    }

    override suspend fun upsertBook(book: Book): Result<Unit, DataError.Local> {
        upsertBookCallCount++
        lastUpsertedBook = book
        errorToReturn?.let { return Result.Error(it) }
        books[book.id] = book
        return Result.Success(Unit)
    }

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        remoteErrorToReturn?.let { return Result.Error(it) }
        val book = books[bookId]
        return Result.Success(book?.description)
    }

    var updateDescriptionCallCount = 0
    var lastUpdatedDescriptionBookId: String? = null
    var lastUpdatedDescription: String? = null

    override suspend fun updateDescription(
        bookId: String,
        description: String?
    ): Result<Unit, DataError.Local> {
        updateDescriptionCallCount++
        lastUpdatedDescriptionBookId = bookId
        lastUpdatedDescription = description
        errorToReturn?.let { return Result.Error(it) }
        books[bookId] = books[bookId]?.copy(description = description) ?: return Result.Success(Unit)
        return Result.Success(Unit)
    }

    override suspend fun upsertSystemBook(book: Book): Result<Unit, DataError.Local> {
        upsertSystemBookCallCount++
        lastUpsertedSystemBook = book
        errorToReturn?.let { return Result.Error(it) }
        books[book.id] = book
        return Result.Success(Unit)
    }

    fun setPersonalBooks(books: List<Book>) {
        personalBooksFlow.value = books
    }

    fun setNonRemovableBookIds(ids: Set<String>) {
        nonRemovableBookIdsFlow.value = ids
    }

    override fun getAllPersonalBooks(): Flow<List<Book>> = personalBooksFlow

    override suspend fun deleteBooks(bookIds: List<String>): Result<Unit, DataError.Local> {
        deleteBooksCallCount++
        lastDeletedBookIds = bookIds
        errorToReturn?.let { return Result.Error(it) }
        bookIds.forEach { books.remove(it) }
        personalBooksFlow.value = personalBooksFlow.value.filter { it.id !in bookIds }
        return Result.Success(Unit)
    }

    override fun getNonRemovableBookIds(): Flow<Set<String>> = nonRemovableBookIdsFlow
}
