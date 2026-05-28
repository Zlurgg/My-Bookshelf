package uk.co.zlurgg.mybookshelf.book.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRemoteBookDataSource

@RunWith(RobolectricTestRunner::class)
class BookRepositoryImplTest {

    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var mockRemoteDataSource: MockRemoteBookDataSource
    private lateinit var repository: BookRepositoryImpl

    /** Helper to unwrap Result for test assertions */
    private suspend fun getBookOrNull(bookId: String) = when (val r = repository.getBookById(bookId)) {
        is Result.Success -> r.data
        is Result.Error -> null
    }

    /** Helper to unwrap Result - throws on error */
    private suspend fun getBookOrFail(bookId: String) = when (val r = repository.getBookById(bookId)) {
        is Result.Success -> r.data!!
        is Result.Error -> throw AssertionError("Expected book but got error: ${r.error}")
    }

    /** Helper to save book and throw on error */
    private suspend fun saveBook(book: uk.co.zlurgg.mybookshelf.book.domain.model.Book) {
        val result = repository.upsertBook(book)
        if (result is Result.Error) throw AssertionError("Failed to save book: ${result.error}")
    }

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MyBookshelfRoomDatabase::class.java
        ).allowMainThreadQueries().build()

        mockRemoteDataSource = MockRemoteBookDataSource()
        repository = BookRepositoryImpl(
            mockRemoteDataSource,
            database.bookshelfDao,
        )
    }

    @After
    fun tearDown() {
        database.close()
        mockRemoteDataSource.reset()
    }

    @Test
    fun `getBookById returns null when book does not exist`() = runTest {
        // Given
        val nonExistentBookId = "non-existent-book"

        // When
        val result = getBookOrNull(nonExistentBookId)

        // Then
        assertNull("Should return null for non-existent book", result)
    }

    @Test
    fun `getBookById returns book when it exists`() = runTest {
        // Given
        val book = TestBookBuilder()
            .withId("test-book-123")
            .withTitle("Test Book")
            .withAuthors(listOf("Test Author"))
            .build()

        // Insert book first
        saveBook(book)

        // When
        val result = getBookOrNull("test-book-123")

        // Then
        assertEquals("Should return the stored book", book.id, result?.id)
        assertEquals("Should preserve title", book.title, result?.title)
        assertEquals("Should preserve authors", book.authors, result?.authors)
        assertEquals("Should preserve purchase status", book.purchased, result?.purchased)
    }

    @Test
    fun `upsertBook inserts new book successfully`() = runTest {
        // Given
        val newBook = TestBookBuilder()
            .withId("new-book")
            .withTitle("New Book Title")
            .withDescription("New book description")
            .withPurchased(false)
            .build()

        // When
        saveBook(newBook)

        // Then
        val retrievedBook = getBookOrNull("new-book")
        assertEquals("Should store book with correct ID", newBook.id, retrievedBook?.id)
        assertEquals("Should store book with correct title", newBook.title, retrievedBook?.title)
        assertEquals("Should store book with correct description", newBook.description, retrievedBook?.description)
        assertEquals("Should store book with correct purchase status", newBook.purchased, retrievedBook?.purchased)
    }

    @Test
    fun `upsertBook updates existing book successfully`() = runTest {
        // Given
        val originalBook = TestBookBuilder()
            .withId("update-book")
            .withTitle("Original Title")
            .withPurchased(false)
            .build()

        val updatedBook = originalBook.copy(
            title = "Updated Title",
            purchased = true
        )

        // Insert original book
        saveBook(originalBook)

        // When - Update the book
        saveBook(updatedBook)

        // Then
        val retrievedBook = getBookOrNull("update-book")
        assertEquals("Should have updated title", "Updated Title", retrievedBook?.title)
        assertTrue("Should have updated purchase status", retrievedBook?.purchased == true)
        assertEquals("Should preserve ID", originalBook.id, retrievedBook?.id)
    }

    @Test
    fun `upsertBook preserves all book data fields`() = runTest {
        // Given
        val completeBook = TestBookBuilder.completeBook()

        // When
        saveBook(completeBook)

        // Then
        val retrievedBook = getBookOrFail(completeBook.id)
        assertEquals("Should preserve ID", completeBook.id, retrievedBook.id)
        assertEquals("Should preserve title", completeBook.title, retrievedBook.title)
        assertEquals("Should preserve image URL", completeBook.imageUrl, retrievedBook.imageUrl)
        assertEquals("Should preserve authors", completeBook.authors, retrievedBook.authors)
        assertEquals("Should preserve description", completeBook.description, retrievedBook.description)
        assertEquals("Should preserve languages", completeBook.languages, retrievedBook.languages)
        assertEquals("Should preserve publish year", completeBook.firstPublishYear, retrievedBook.firstPublishYear)
        assertEquals("Should preserve page count", completeBook.numPages, retrievedBook.numPages)
        assertEquals("Should preserve provider", completeBook.provider, retrievedBook.provider)
        assertEquals("Should preserve purchase status", completeBook.purchased, retrievedBook.purchased)
        assertEquals("Should preserve spine color", completeBook.spineColor, retrievedBook.spineColor)
    }

    @Test
    fun `upsertBook handles books with null optional fields`() = runTest {
        // Given
        val bookWithNulls = TestBookBuilder()
            .withId("book-with-nulls")
            .withTitle("Book with Null Fields")
            .withDescription(null)
            .build()

        // When
        saveBook(bookWithNulls)

        // Then
        val retrievedBook = getBookOrFail("book-with-nulls")
        assertEquals("Should preserve title", bookWithNulls.title, retrievedBook.title)
        assertNull("Should handle null description", retrievedBook.description)
    }

    @Test
    fun `upsertBook handles books with empty collections`() = runTest {
        // Given
        val bookWithEmptyCollections = TestBookBuilder()
            .withId("empty-collections")
            .withTitle("Book with Empty Collections")
            .withAuthors(emptyList())
            .withLanguages(emptyList())
            .build()

        // When
        saveBook(bookWithEmptyCollections)

        // Then
        val retrievedBook = getBookOrFail("empty-collections")
        assertTrue("Should handle empty authors", retrievedBook.authors.isEmpty())
        assertTrue("Should handle empty languages", retrievedBook.languages.isEmpty())
    }

    @Test
    fun `multiple books can be stored and retrieved independently`() = runTest {
        // Given
        val book1 = TestBookBuilder()
            .withId("book-1")
            .withTitle("First Book")
            .build()

        val book2 = TestBookBuilder()
            .withId("book-2")
            .withTitle("Second Book")
            .build()

        val book3 = TestBookBuilder()
            .withId("book-3")
            .withTitle("Third Book")
            .build()

        // When
        saveBook(book1)
        saveBook(book2)
        saveBook(book3)

        // Then
        val retrieved1 = getBookOrNull("book-1")
        val retrieved2 = getBookOrNull("book-2")
        val retrieved3 = getBookOrNull("book-3")

        assertEquals("Should retrieve first book correctly", "First Book", retrieved1?.title)
        assertEquals("Should retrieve second book correctly", "Second Book", retrieved2?.title)
        assertEquals("Should retrieve third book correctly", "Third Book", retrieved3?.title)
    }

    @Test
    fun `getBookDescription returns success when remote source succeeds`() = runTest {
        val bookId = "test-book-details"
        val expectedDescription = "This is a test book description"
        mockRemoteDataSource.configureBookDescription(expectedDescription)

        val result = repository.getBookDescription(
            bookId,
            BookProvider.GOOGLE_BOOKS
        )

        assertTrue("Should return success", result is Result.Success)
        assertEquals(expectedDescription, (result as Result.Success).data)
    }

    @Test
    fun `getBookDescription returns error when remote source fails`() = runTest {
        mockRemoteDataSource.shouldThrowException = true
        mockRemoteDataSource.networkError = DataError.Remote.NO_INTERNET

        val result = repository.getBookDescription(
            "failing-book",
            BookProvider.GOOGLE_BOOKS
        )

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Remote.NO_INTERNET, (result as Result.Error).error)
    }

    @Test
    fun `updateDescription persists description and preserves other book fields`() = runTest {
        // Given
        val original = TestBookBuilder()
            .withId("desc-book")
            .withTitle("Fixed Title")
            .withDescription(null)
            .withPersonalNotes("My notes")
            .withPersonalRating(3.5f)
            .build()
        saveBook(original)

        // When
        val result = repository.updateDescription("desc-book", "Fresh description")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val retrieved = getBookOrFail("desc-book")
        assertEquals("Fresh description", retrieved.description)
        assertEquals("Fixed Title", retrieved.title)
        assertEquals("My notes", retrieved.personalNotes)
        assertEquals(3.5f, retrieved.personalRating)
    }

    @Test
    fun `updateDescription maps DAO exception to DataError Local`() = runTest {
        // Given — closing the database before invoking the DAO causes Room to throw
        // (IllegalStateException). ErrorMapper should catch and surface DataError.Local.
        database.close()

        // When
        val result = repository.updateDescription("any-book", "anything")

        // Then
        assertTrue("Should return error when DAO throws", result is Result.Error)
        val error = (result as Result.Error).error
        // The error type is DataError.Local by repository contract; we just confirm
        // it produced an error result (no need to re-check the static type).
        assertEquals(
            "Should map IllegalStateException to DATABASE_ERROR",
            DataError.Local.DATABASE_ERROR,
            error
        )
    }

    @Test
    fun `getBookById does NOT fall back to the preview cache`() = runTest {
        // v3 contract: getBookById is DAO-only. Cache access lives on peekPreview.
        // Routing cache through getBookById previously caused metadata-update use
        // cases to promote previewed books into the library on back-press.
        val cachedOnly = TestBookBuilder()
            .withId("cache-only")
            .withTitle("Cached Preview")
            .build()
        repository.cacheSearchPreviews(listOf(cachedOnly))

        assertNull("getBookById must ignore the cache", getBookOrNull("cache-only"))
    }

    @Test
    fun `peekPreview returns the cached book when present and null when absent`() = runTest {
        val cachedOnly = TestBookBuilder()
            .withId("cache-only")
            .withTitle("Cached Preview")
            .build()
        repository.cacheSearchPreviews(listOf(cachedOnly))

        assertEquals("Cached Preview", repository.peekPreview("cache-only")?.title)
        assertNull("No cache entry", repository.peekPreview("nothing-cached"))
    }

    @Test
    fun `getBookById and peekPreview are independent for the same id`() = runTest {
        // DB row and cache entry can both exist for the same id — callers compose
        // them explicitly (DB-first) in GetBookDetailsUseCase rather than letting
        // the repository decide.
        val dbBook = TestBookBuilder()
            .withId("conflicting-id")
            .withTitle("DB Title")
            .withPersonalNotes("Important notes")
            .build()
        val cachedBook = TestBookBuilder()
            .withId("conflicting-id")
            .withTitle("API Title")
            .build()
        saveBook(dbBook)
        repository.cacheSearchPreviews(listOf(cachedBook))

        assertEquals("DB Title", getBookOrFail("conflicting-id").title)
        assertEquals("Important notes", getBookOrFail("conflicting-id").personalNotes)
        assertEquals("API Title", repository.peekPreview("conflicting-id")?.title)
    }

    @Test
    fun `cacheSearchPreviews stores all books readable via peekPreview`() = runTest {
        val books = listOf(
            TestBookBuilder().withId("a").withTitle("A").build(),
            TestBookBuilder().withId("b").withTitle("B").build(),
            TestBookBuilder().withId("c").withTitle("C").build(),
        )

        repository.cacheSearchPreviews(books)

        assertEquals("A", repository.peekPreview("a")?.title)
        assertEquals("B", repository.peekPreview("b")?.title)
        assertEquals("C", repository.peekPreview("c")?.title)
    }

    @Test
    fun `cacheSearchPreviews evicts entries from previous calls`() = runTest {
        // Each search supersedes the previous — older entries are unreachable
        // through the UI, so the cache should not accumulate them across queries.
        val firstSearch = listOf(
            TestBookBuilder().withId("old-1").withTitle("Old 1").build(),
            TestBookBuilder().withId("old-2").withTitle("Old 2").build(),
        )
        val secondSearch = listOf(
            TestBookBuilder().withId("new-1").withTitle("New 1").build(),
        )
        repository.cacheSearchPreviews(firstSearch)
        repository.cacheSearchPreviews(secondSearch)

        assertNull("Old cached entry should be evicted", repository.peekPreview("old-1"))
        assertNull("Old cached entry should be evicted", repository.peekPreview("old-2"))
        assertEquals("New 1", repository.peekPreview("new-1")?.title)
    }

    @Test
    fun `updatePersonalMetadata on a row that does not exist is a silent no-op`() = runTest {
        // SQLite UPDATE on a missing row is a no-op. This is the leak-fix
        // invariant at the storage layer — a previewed book (no DB row) cannot
        // be promoted into the library by an edit on the detail screen.
        val result = repository.updatePersonalMetadata(
            bookId = "ghost-id",
            readingStatus = "FINISHED",
            personalRating = 5.0f,
            personalNotes = "Should not land",
        )

        assertTrue("UPDATE on missing row must succeed", result is Result.Success)
        assertNull("Row must NOT exist after no-op", getBookOrNull("ghost-id"))
    }

    @Test
    fun `updatePersonalMetadata writes each non-null column and leaves others alone`() = runTest {
        val original = TestBookBuilder()
            .withId("meta-book")
            .withReadingStatus(ReadingStatus.NOT_READ)
            .withPersonalRating(0f)
            .withPersonalNotes("original")
            .build()
        saveBook(original)

        val notesOnly = repository.updatePersonalMetadata(
            bookId = "meta-book",
            personalNotes = "updated notes",
        )
        assertTrue(notesOnly is Result.Success)
        val afterNotes = getBookOrFail("meta-book")
        assertEquals("updated notes", afterNotes.personalNotes)
        assertEquals("Reading status untouched", ReadingStatus.NOT_READ, afterNotes.readingStatus)
        assertEquals("Rating untouched", 0f, afterNotes.personalRating)

        val ratingOnly = repository.updatePersonalMetadata(bookId = "meta-book", personalRating = 4.5f)
        assertTrue(ratingOnly is Result.Success)
        val afterRating = getBookOrFail("meta-book")
        assertEquals(4.5f, afterRating.personalRating)
        assertEquals("Notes untouched", "updated notes", afterRating.personalNotes)
    }

    @Test
    fun `updatePurchased on a row that does not exist is a silent no-op`() = runTest {
        val result = repository.updatePurchased(bookId = "ghost-id", purchased = true)

        assertTrue(result is Result.Success)
        assertNull(getBookOrNull("ghost-id"))
    }

    @Test
    fun `updatePurchased writes only the purchased column`() = runTest {
        val original = TestBookBuilder()
            .withId("purchased-book")
            .withTitle("Original")
            .withPurchased(false)
            .withPersonalNotes("keeps these")
            .withPersonalRating(3.5f)
            .build()
        saveBook(original)

        val result = repository.updatePurchased(bookId = "purchased-book", purchased = true)

        assertTrue(result is Result.Success)
        val updated = getBookOrFail("purchased-book")
        assertEquals(true, updated.purchased)
        assertEquals("Original", updated.title)
        assertEquals("keeps these", updated.personalNotes)
        assertEquals(3.5f, updated.personalRating)
    }

    @Test
    fun `upsertBook with extremely long data fields`() = runTest {
        // Given
        val longString = "A".repeat(10000) // Very long string
        val bookWithLongData = TestBookBuilder()
            .withId("long-data-book")
            .withTitle(longString)
            .withDescription(longString)
            .build()

        // When
        saveBook(bookWithLongData)

        // Then
        val retrievedBook = getBookOrFail("long-data-book")
        assertEquals("Should handle long title", longString, retrievedBook.title)
        assertEquals("Should handle long description", longString, retrievedBook.description)
    }
}
