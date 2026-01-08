package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

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
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.TestTimeProvider
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRemoteBookDataSource

@RunWith(RobolectricTestRunner::class)
class BookRepositoryImplTest {
    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var mockRemoteDataSource: MockRemoteBookDataSource
    private lateinit var repository: BookRepositoryImpl
    private val testTimeProvider = TestTimeProvider(currentTime = 1234567890L)

    private val mockCurrentUserProvider =
        object : CurrentUserProvider {
            override fun getCurrentUserId(): String = "test-user-id"
        }

    @Before
    fun setup() {
        // Create in-memory database for testing
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                MyBookshelfRoomDatabase::class.java,
            ).allowMainThreadQueries().build()

        mockRemoteDataSource = MockRemoteBookDataSource()
        repository = BookRepositoryImpl(mockRemoteDataSource, database.bookshelfDao, mockCurrentUserProvider, testTimeProvider)
    }

    @After
    fun tearDown() {
        database.close()
        mockRemoteDataSource.reset()
    }

    @Test
    fun `getBookById returns null when book does not exist`() =
        runTest {
            // Given
            val nonExistentBookId = "non-existent-book"

            // When
            val result = repository.getBookById(nonExistentBookId)

            // Then
            assertNull("Should return null for non-existent book", result)
        }

    @Test
    fun `getBookById returns book when it exists`() =
        runTest {
            // Given
            val book =
                TestBookBuilder()
                    .withId("test-book-123")
                    .withTitle("Test Book")
                    .withAuthors(listOf("Test Author"))
                    .build()

            // Insert book first
            repository.upsertBook(book)

            // When
            val result = repository.getBookById("test-book-123")

            // Then
            assertEquals("Should return the stored book", book.id, result?.id)
            assertEquals("Should preserve title", book.title, result?.title)
            assertEquals("Should preserve authors", book.authors, result?.authors)
            assertEquals("Should preserve purchase status", book.purchased, result?.purchased)
        }

    @Test
    fun `upsertBook inserts new book successfully`() =
        runTest {
            // Given
            val newBook =
                TestBookBuilder()
                    .withId("new-book")
                    .withTitle("New Book Title")
                    .withDescription("New book description")
                    .withPurchased(false)
                    .build()

            // When
            repository.upsertBook(newBook)

            // Then
            val retrievedBook = repository.getBookById("new-book")
            assertEquals("Should store book with correct ID", newBook.id, retrievedBook?.id)
            assertEquals("Should store book with correct title", newBook.title, retrievedBook?.title)
            assertEquals("Should store book with correct description", newBook.description, retrievedBook?.description)
            assertEquals("Should store book with correct purchase status", newBook.purchased, retrievedBook?.purchased)
        }

    @Test
    fun `upsertBook updates existing book successfully`() =
        runTest {
            // Given
            val originalBook =
                TestBookBuilder()
                    .withId("update-book")
                    .withTitle("Original Title")
                    .withPurchased(false)
                    .build()

            val updatedBook =
                originalBook.copy(
                    title = "Updated Title",
                    purchased = true,
                )

            // Insert original book
            repository.upsertBook(originalBook)

            // When - Update the book
            repository.upsertBook(updatedBook)

            // Then
            val retrievedBook = repository.getBookById("update-book")
            assertEquals("Should have updated title", "Updated Title", retrievedBook?.title)
            assertTrue("Should have updated purchase status", retrievedBook?.purchased == true)
            assertEquals("Should preserve ID", originalBook.id, retrievedBook?.id)
        }

    @Test
    fun `upsertBook preserves all book data fields`() =
        runTest {
            // Given
            val completeBook = TestBookBuilder.completeBook()

            // When
            repository.upsertBook(completeBook)

            // Then
            val retrievedBook = repository.getBookById(completeBook.id)!!
            assertEquals("Should preserve ID", completeBook.id, retrievedBook.id)
            assertEquals("Should preserve title", completeBook.title, retrievedBook.title)
            assertEquals("Should preserve image URL", completeBook.imageUrl, retrievedBook.imageUrl)
            assertEquals("Should preserve authors", completeBook.authors, retrievedBook.authors)
            assertEquals("Should preserve description", completeBook.description, retrievedBook.description)
            assertEquals("Should preserve languages", completeBook.languages, retrievedBook.languages)
            assertEquals("Should preserve publish year", completeBook.firstPublishYear, retrievedBook.firstPublishYear)
            assertEquals("Should preserve rating", completeBook.averageRating, retrievedBook.averageRating)
            assertEquals("Should preserve rating count", completeBook.ratingCount, retrievedBook.ratingCount)
            assertEquals("Should preserve page count", completeBook.numPages, retrievedBook.numPages)
            assertEquals("Should preserve edition count", completeBook.numEditions, retrievedBook.numEditions)
            assertEquals("Should preserve purchase status", completeBook.purchased, retrievedBook.purchased)
            assertEquals("Should preserve spine color", completeBook.spineColor, retrievedBook.spineColor)
        }

    @Test
    fun `deleteBook removes book from database`() =
        runTest {
            // Given
            val book =
                TestBookBuilder()
                    .withId("book-to-delete")
                    .withTitle("Book for Deletion")
                    .build()

            repository.upsertBook(book)

            // Verify book exists
            val beforeDeletion = repository.getBookById("book-to-delete")
            assertEquals("Book should exist before deletion", book.id, beforeDeletion?.id)

            // When
            repository.deleteBook("book-to-delete")

            // Then
            val afterDeletion = repository.getBookById("book-to-delete")
            assertNull("Book should not exist after deletion", afterDeletion)
        }

    @Test
    fun `deleteBook handles non-existent book gracefully`() =
        runTest {
            // Given
            val nonExistentBookId = "does-not-exist"

            // When - Should not throw exception
            repository.deleteBook(nonExistentBookId)

            // Then - Should complete successfully
            val result = repository.getBookById(nonExistentBookId)
            assertNull("Should return null for non-existent book", result)
        }

    @Test
    fun `upsertBook handles books with null optional fields`() =
        runTest {
            // Given
            val bookWithNulls =
                TestBookBuilder()
                    .withId("book-with-nulls")
                    .withTitle("Book with Null Fields")
                    .withAverageRating(null)
                    .build()

            // When
            repository.upsertBook(bookWithNulls)

            // Then
            val retrievedBook = repository.getBookById("book-with-nulls")!!
            assertEquals("Should preserve title", bookWithNulls.title, retrievedBook.title)
            assertEquals("Should handle null rating", null, retrievedBook.averageRating)
        }

    @Test
    fun `upsertBook handles books with empty collections`() =
        runTest {
            // Given
            val bookWithEmptyCollections =
                TestBookBuilder()
                    .withId("empty-collections")
                    .withTitle("Book with Empty Collections")
                    .withAuthors(emptyList())
                    .withLanguages(emptyList())
                    .build()

            // When
            repository.upsertBook(bookWithEmptyCollections)

            // Then
            val retrievedBook = repository.getBookById("empty-collections")!!
            assertTrue("Should handle empty authors", retrievedBook.authors.isEmpty())
            assertTrue("Should handle empty languages", retrievedBook.languages.isEmpty())
        }

    @Test
    fun `multiple books can be stored and retrieved independently`() =
        runTest {
            // Given
            val book1 =
                TestBookBuilder()
                    .withId("book-1")
                    .withTitle("First Book")
                    .build()

            val book2 =
                TestBookBuilder()
                    .withId("book-2")
                    .withTitle("Second Book")
                    .build()

            val book3 =
                TestBookBuilder()
                    .withId("book-3")
                    .withTitle("Third Book")
                    .build()

            // When
            repository.upsertBook(book1)
            repository.upsertBook(book2)
            repository.upsertBook(book3)

            // Then
            val retrieved1 = repository.getBookById("book-1")
            val retrieved2 = repository.getBookById("book-2")
            val retrieved3 = repository.getBookById("book-3")

            assertEquals("Should retrieve first book correctly", "First Book", retrieved1?.title)
            assertEquals("Should retrieve second book correctly", "Second Book", retrieved2?.title)
            assertEquals("Should retrieve third book correctly", "Third Book", retrieved3?.title)
        }

    @Test
    fun `getBookDescription returns success when remote source succeeds`() =
        runTest {
            // Given
            val bookId = "test-book-details"
            val expectedDescription = "This is a test book description"

            // Configure mock to return book details with description
            val mockBookDetails =
                uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto(
                    description = expectedDescription,
                )
            mockRemoteDataSource.configureBookDetailsResponse(mockBookDetails)

            // When
            val result = repository.getBookDescription(bookId)

            // Then
            assertTrue("Should return success", result is Result.Success)
            val description = (result as Result.Success).data
            assertEquals("Should return correct description", expectedDescription, description)
        }

    @Test
    fun `getBookDescription returns error when remote source fails`() =
        runTest {
            // Given
            val bookId = "failing-book"
            mockRemoteDataSource.shouldThrowException = true
            mockRemoteDataSource.networkError = DataError.Remote.NO_INTERNET

            // When
            val result = repository.getBookDescription(bookId)

            // Then
            assertTrue("Should return error", result is Result.Error)
            val error = (result as Result.Error).error
            assertEquals("Should return network error", DataError.Remote.NO_INTERNET, error)
        }

    @Test
    fun `getBookDescription handles null description gracefully`() =
        runTest {
            // Given
            val bookId = "book-no-description"

            val mockBookDetails =
                uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto(
                    description = null,
                )
            mockRemoteDataSource.configureBookDetailsResponse(mockBookDetails)

            // When
            val result = repository.getBookDescription(bookId)

            // Then
            assertTrue("Should return success", result is Result.Success)
            val description = (result as Result.Success).data
            assertNull("Should return null for missing description", description)
        }

    @Test
    fun `upsertBook with extremely long data fields`() =
        runTest {
            // Given
            val longString = "A".repeat(10000) // Very long string
            val bookWithLongData =
                TestBookBuilder()
                    .withId("long-data-book")
                    .withTitle(longString)
                    .withDescription(longString)
                    .build()

            // When
            repository.upsertBook(bookWithLongData)

            // Then
            val retrievedBook = repository.getBookById("long-data-book")!!
            assertEquals("Should handle long title", longString, retrievedBook.title)
            assertEquals("Should handle long description", longString, retrievedBook.description)
        }
}
