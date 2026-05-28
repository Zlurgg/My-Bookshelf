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
    fun `getBookById falls back to preview cache when DB has no row`() = runTest {
        // Cached search results should be readable through getBookById without
        // first being persisted — that's the whole point of the preview cache.
        val cachedOnly = TestBookBuilder()
            .withId("cache-only")
            .withTitle("Cached Preview")
            .build()
        repository.cacheSearchPreviews(listOf(cachedOnly))

        val retrieved = getBookOrFail("cache-only")

        assertEquals("Should return cached title", "Cached Preview", retrieved.title)
    }

    @Test
    fun `getBookById prefers DB row over preview cache for the same id`() = runTest {
        // DB row carries personal metadata (notes, rating, reading status) that
        // the cached API version would not have, so DB must win.
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

        val retrieved = getBookOrFail("conflicting-id")

        assertEquals("DB row must win", "DB Title", retrieved.title)
        assertEquals("Personal metadata must be preserved", "Important notes", retrieved.personalNotes)
    }

    @Test
    fun `cacheSearchPreviews stores all books in one call`() = runTest {
        val books = listOf(
            TestBookBuilder().withId("a").withTitle("A").build(),
            TestBookBuilder().withId("b").withTitle("B").build(),
            TestBookBuilder().withId("c").withTitle("C").build(),
        )

        repository.cacheSearchPreviews(books)

        assertEquals("A", getBookOrFail("a").title)
        assertEquals("B", getBookOrFail("b").title)
        assertEquals("C", getBookOrFail("c").title)
    }

    @Test
    fun `cacheSearchPreviews evicts entries from previous calls`() = runTest {
        // Each search supersedes the previous — older entries are unreachable
        // through the UI (the search dialog only shows the latest result set),
        // so the cache should not accumulate them across queries.
        val firstSearch = listOf(
            TestBookBuilder().withId("old-1").withTitle("Old 1").build(),
            TestBookBuilder().withId("old-2").withTitle("Old 2").build(),
        )
        val secondSearch = listOf(
            TestBookBuilder().withId("new-1").withTitle("New 1").build(),
        )
        repository.cacheSearchPreviews(firstSearch)
        repository.cacheSearchPreviews(secondSearch)

        assertNull("Old cached entry should be evicted", getBookOrNull("old-1"))
        assertNull("Old cached entry should be evicted", getBookOrNull("old-2"))
        assertEquals("New entry should be readable", "New 1", getBookOrFail("new-1").title)
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
