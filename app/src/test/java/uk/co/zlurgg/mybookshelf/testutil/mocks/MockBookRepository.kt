package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class MockBookRepository : BookRepository {

    private val books = mutableMapOf<String, Book>()

    var errorToReturn: DataError.Local? = null
    var remoteErrorToReturn: DataError.Remote? = null
    var upsertBookCallCount = 0
    var upsertSystemBookCallCount = 0
    var deleteBookCallCount = 0
    var getBookByIdCallCount = 0
    var lastUpsertedBook: Book? = null
    var lastUpsertedSystemBook: Book? = null
    var lastDeletedBookId: String? = null
    var lastQueriedBookId: String? = null

    fun reset() {
        books.clear()
        errorToReturn = null
        remoteErrorToReturn = null
        upsertBookCallCount = 0
        upsertSystemBookCallCount = 0
        deleteBookCallCount = 0
        getBookByIdCallCount = 0
        lastUpsertedBook = null
        lastUpsertedSystemBook = null
        lastDeletedBookId = null
        lastQueriedBookId = null
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

    override suspend fun deleteBook(bookId: String): Result<Unit, DataError.Local> {
        deleteBookCallCount++
        lastDeletedBookId = bookId
        errorToReturn?.let { return Result.Error(it) }
        books.remove(bookId)
        return Result.Success(Unit)
    }

    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        remoteErrorToReturn?.let { return Result.Error(it) }
        val book = books[bookId]
        return Result.Success(book?.description)
    }

    override suspend fun upsertSystemBook(book: Book): Result<Unit, DataError.Local> {
        upsertSystemBookCallCount++
        lastUpsertedSystemBook = book
        errorToReturn?.let { return Result.Error(it) }
        books[book.id] = book
        return Result.Success(Unit)
    }
}
