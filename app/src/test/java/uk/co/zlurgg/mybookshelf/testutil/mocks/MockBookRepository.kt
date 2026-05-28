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
    private val previewCache = mutableMapOf<String, Book>()
    private val personalBooksFlow = MutableStateFlow<List<Book>>(emptyList())

    var errorToReturn: DataError.Local? = null
    var remoteErrorToReturn: DataError.Remote? = null
    var upsertBookCallCount = 0
    var upsertSystemBookCallCount = 0
    var getBookByIdCallCount = 0
    var deleteBooksCallCount = 0
    var cacheSearchPreviewsCallCount = 0
    var lastUpsertedBook: Book? = null
    var lastUpsertedSystemBook: Book? = null
    var lastQueriedBookId: String? = null
    var lastDeletedBookIds: List<String> = emptyList()
    var lastCachedPreviewIds: List<String> = emptyList()

    // Column-scoped update tracking — preview-cache leak fix moved personal-metadata
    // writes off get-then-upsert onto targeted UPDATEs, so tests assert on these.
    var updatePersonalMetadataCallCount = 0
    var lastPersonalMetadataBookId: String? = null
    var lastPersonalMetadataReadingStatus: String? = null
    var lastPersonalMetadataRating: Float? = null
    var lastPersonalMetadataNotes: String? = null
    var updatePurchasedCallCount = 0
    var lastPurchasedBookId: String? = null
    var lastPurchasedValue: Boolean? = null

    private val nonRemovableBookIdsFlow = MutableStateFlow<Set<String>>(emptySet())

    fun reset() {
        books.clear()
        previewCache.clear()
        errorToReturn = null
        remoteErrorToReturn = null
        upsertBookCallCount = 0
        upsertSystemBookCallCount = 0
        getBookByIdCallCount = 0
        deleteBooksCallCount = 0
        cacheSearchPreviewsCallCount = 0
        updateDescriptionCallCount = 0
        updatePersonalMetadataCallCount = 0
        lastPersonalMetadataBookId = null
        lastPersonalMetadataReadingStatus = null
        lastPersonalMetadataRating = null
        lastPersonalMetadataNotes = null
        updatePurchasedCallCount = 0
        lastPurchasedBookId = null
        lastPurchasedValue = null
        lastUpsertedBook = null
        lastUpsertedSystemBook = null
        lastQueriedBookId = null
        lastDeletedBookIds = emptyList()
        lastCachedPreviewIds = emptyList()
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

    override suspend fun updatePersonalMetadata(
        bookId: String,
        readingStatus: String?,
        personalRating: Float?,
        personalNotes: String?,
    ): Result<Unit, DataError.Local> {
        updatePersonalMetadataCallCount++
        lastPersonalMetadataBookId = bookId
        lastPersonalMetadataReadingStatus = readingStatus
        lastPersonalMetadataRating = personalRating
        lastPersonalMetadataNotes = personalNotes
        errorToReturn?.let { return Result.Error(it) }
        // UPDATE on a missing row is a no-op (matches DAO behaviour).
        val existing = books[bookId] ?: return Result.Success(Unit)
        books[bookId] = existing.copy(
            readingStatus = readingStatus?.let {
                uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus.valueOf(it)
            } ?: existing.readingStatus,
            personalRating = personalRating ?: existing.personalRating,
            personalNotes = personalNotes ?: existing.personalNotes,
        )
        return Result.Success(Unit)
    }

    override suspend fun updatePurchased(
        bookId: String,
        purchased: Boolean,
    ): Result<Unit, DataError.Local> {
        updatePurchasedCallCount++
        lastPurchasedBookId = bookId
        lastPurchasedValue = purchased
        errorToReturn?.let { return Result.Error(it) }
        val existing = books[bookId] ?: return Result.Success(Unit)
        books[bookId] = existing.copy(purchased = purchased)
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

    override fun cacheSearchPreviews(books: List<Book>) {
        cacheSearchPreviewsCallCount++
        lastCachedPreviewIds = books.map { it.id }
        previewCache.clear()
        books.forEach { previewCache[it.id] = it }
    }

    override fun peekPreview(bookId: String): Book? = previewCache[bookId]

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
