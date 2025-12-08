package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class MockBookRepository : BookRepository {

    private val books = mutableMapOf<String, Book>()

    var shouldThrowException = false
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
        shouldThrowException = false
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

    override suspend fun getBookById(bookId: String): Book? {
        getBookByIdCallCount++
        lastQueriedBookId = bookId

        if (shouldThrowException) {
            throw RuntimeException("Mock repository error")
        }

        return books[bookId]
    }

    override suspend fun upsertBook(book: Book) {
        upsertBookCallCount++
        lastUpsertedBook = book

        if (shouldThrowException) {
            throw RuntimeException("Mock repository error")
        }

        books[book.id] = book
    }

    override suspend fun deleteBook(bookId: String) {
        deleteBookCallCount++
        lastDeletedBookId = bookId

        if (shouldThrowException) {
            throw RuntimeException("Mock repository error")
        }

        books.remove(bookId)
    }

    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        if (shouldThrowException) {
            return Result.Error(DataError.Remote.REQUEST_TIMEOUT)
        }

        val book = books[bookId]
        return Result.Success(book?.description)
    }

    override suspend fun upsertSystemBook(book: Book) {
        upsertSystemBookCallCount++
        lastUpsertedSystemBook = book

        if (shouldThrowException) {
            throw RuntimeException("Mock repository error")
        }

        books[book.id] = book
    }
}