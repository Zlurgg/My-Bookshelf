package uk.co.zlurgg.mybookshelf.book.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookshelfRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.StubClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants

class AddBookToShelfUseCaseTest {

    private val mockBookRepository = MockBookRepository()
    private val mockBookshelfRepository = MockBookshelfRepository()
    private val mockBookcaseRepository = MockBookcaseRepository()
    private val mockClubOperations = StubClubOperations(
        syncBookToClubResult = Result.Success(Unit),
    )
    private val useCase = AddBookToShelfUseCaseImpl(
        mockBookRepository,
        mockBookshelfRepository,
        mockBookcaseRepository,
        mockClubOperations,
    )

    /**
     * Helper to set up a default shelf for tests that don't need specific shelf configuration.
     */
    private fun setUpDefaultShelf(shelfId: String) {
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(shelfId)
            .withName("Test Shelf")
            .withBooks(emptyList())
            .build()
    }

    @After
    fun tearDown() {
        mockBookRepository.reset()
        mockBookshelfRepository.reset()
        mockBookcaseRepository.reset()
    }

    @Test
    fun `execute successfully adds book to shelf`() = runTest {
        // Given
        val book = TestBookBuilder()
            .withId("book-123")
            .withTitle("Test Book")
            .build()
        val shelfId = "shelf-456"
        setUpDefaultShelf(shelfId)

        // When
        val result = useCase(book, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call upsertBook once", 1, mockBookRepository.upsertBookCallCount)
        assertEquals("Should call addBookToShelf once", 1, mockBookshelfRepository.addBookToShelfCallCount)
        val upsertedBook = mockBookRepository.lastUpsertedBook!!
        assertEquals("Should upsert correct book ID", book.id, upsertedBook.id)
        assertEquals("Should upsert correct book title", book.title, upsertedBook.title)
        assertTrue("Should generate spine color for new book", upsertedBook.spineColor != 0)
        assertEquals("Should add to correct shelf", shelfId, mockBookshelfRepository.lastAddedShelfId)
        assertEquals("Should add correct book", book.id, mockBookshelfRepository.lastAddedBookId)
    }

    @Test
    fun `execute creates proper shelf-book relationship`() = runTest {
        // Given
        val book = TestBookBuilder()
            .withId("test-book-id")
            .withTitle("Science Fiction Novel")
            .build()
        val shelfId = "sci-fi-shelf"
        setUpDefaultShelf(shelfId)

        // When
        val result = useCase(book, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val relations = mockBookshelfRepository.getShelfBookRelations()
        assertTrue("Should create shelf relationship", relations.containsKey(shelfId))
        assertTrue("Should add book to shelf", relations[shelfId]?.contains(book.id) == true)
    }

    @Test
    fun `execute with existing book updates book data but preserves personal metadata`() = runTest {
        // Given - Book already exists in repository
        val existingBook = TestBookBuilder()
            .withId("existing-book")
            .withTitle("Old Title")
            .withPurchased(false)
            .build()
        mockBookRepository.addBook(existingBook)

        val updatedBook = TestBookBuilder()
            .withId("existing-book") // Same ID
            .withTitle("Updated Title")
            .withPurchased(true)
            .build()
        val shelfId = "test-shelf"
        setUpDefaultShelf(shelfId)

        // When
        val result = useCase(updatedBook, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val storedBook = mockBookRepository.getStoredBook("existing-book")
        assertEquals("Should update book title", "Updated Title", storedBook?.title)
        // Personal metadata (purchased) should be preserved from existing book
        assertEquals("Should preserve purchased status", false, storedBook?.purchased)
    }

    @Test
    fun `execute can add same book to multiple shelves`() = runTest {
        // Given
        val book = TestBookBuilder()
            .withId("popular-book")
            .withTitle("Popular Novel")
            .build()
        val firstShelf = "fiction-shelf"
        val secondShelf = "favorites-shelf"
        setUpDefaultShelf(firstShelf)

        // When - Add to first shelf
        val firstResult = useCase(book, firstShelf)
        // Set up second shelf
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(secondShelf)
            .withName("Favorites")
            .withBooks(emptyList())
            .build()
        // When - Add to second shelf
        val secondResult = useCase(book, secondShelf)

        // Then
        assertTrue("First addition should succeed", firstResult is Result.Success)
        assertTrue("Second addition should succeed", secondResult is Result.Success)
        assertEquals("Should call upsertBook twice", 2, mockBookRepository.upsertBookCallCount)
        assertEquals("Should call addBookToShelf twice", 2, mockBookshelfRepository.addBookToShelfCallCount)

        val relations = mockBookshelfRepository.getShelfBookRelations()
        assertTrue("Should add to first shelf", relations[firstShelf]?.contains(book.id) == true)
        assertTrue("Should add to second shelf", relations[secondShelf]?.contains(book.id) == true)
    }

    @Test
    fun `execute returns error when book repository fails`() = runTest {
        // Given
        val book = TestBookBuilder().withId("test-book").build()
        val shelfId = "test-shelf"
        setUpDefaultShelf(shelfId)
        mockBookRepository.errorToReturn = DataError.Local.UNKNOWN

        // When
        val result = useCase(book, shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.UNKNOWN, error)
        // getBookById throws exception first, so upsertBook is never called
        assertEquals("Should not call upsertBook", 0, mockBookRepository.upsertBookCallCount)
        assertEquals("Should not call addBookToShelf", 0, mockBookshelfRepository.addBookToShelfCallCount)
    }

    @Test
    fun `execute returns error when bookshelf repository fails`() = runTest {
        // Given
        val book = TestBookBuilder().withId("test-book").build()
        val shelfId = "test-shelf"
        setUpDefaultShelf(shelfId)
        mockBookshelfRepository.errorToReturn = DataError.Local.UNKNOWN

        // When
        val result = useCase(book, shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.UNKNOWN, error)
        assertEquals("Should call upsertBook once", 1, mockBookRepository.upsertBookCallCount)
        assertEquals("Should call addBookToShelf once", 1, mockBookshelfRepository.addBookToShelfCallCount)
    }

    @Test
    fun `execute handles empty book ID gracefully`() = runTest {
        // Given
        val book = TestBookBuilder()
            .withId("") // Empty ID
            .withTitle("Book with Empty ID")
            .build()
        val shelfId = "test-shelf"
        setUpDefaultShelf(shelfId)

        // When
        val result = useCase(book, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should still call repositories", 1, mockBookRepository.upsertBookCallCount)
        assertEquals("Should still call addBookToShelf", 1, mockBookshelfRepository.addBookToShelfCallCount)
        assertEquals("Should pass empty ID to shelf repository", "", mockBookshelfRepository.lastAddedBookId)
    }

    @Test
    fun `execute handles empty shelf ID gracefully`() = runTest {
        // Given
        val book = TestBookBuilder().withId("test-book").build()
        val shelfId = "" // Empty shelf ID
        setUpDefaultShelf(shelfId)

        // When
        val result = useCase(book, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call upsertBook", 1, mockBookRepository.upsertBookCallCount)
        assertEquals("Should call addBookToShelf", 1, mockBookshelfRepository.addBookToShelfCallCount)
        assertEquals("Should pass empty shelf ID", "", mockBookshelfRepository.lastAddedShelfId)
    }

    @Test
    fun `execute persists complete book data`() = runTest {
        // Given
        val book = TestBookBuilder.completeBook()
        val shelfId = "complete-shelf"
        setUpDefaultShelf(shelfId)

        // When
        val result = useCase(book, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val upsertedBook = mockBookRepository.lastUpsertedBook!!
        assertEquals("Should preserve all book fields", book.title, upsertedBook.title)
        assertEquals("Should preserve authors", book.authors, upsertedBook.authors)
        assertEquals("Should preserve rating", book.averageRating, upsertedBook.averageRating)
        assertEquals("Should preserve purchase status", book.purchased, upsertedBook.purchased)
        assertTrue("Should generate spine color for new book", upsertedBook.spineColor != 0)
    }

    @Test
    fun `execute preserves personal metadata when book already exists`() = runTest {
        // Given - Book already exists with personal metadata
        val existingBook = TestBookBuilder()
            .withId("book-with-metadata")
            .withTitle("Old Title")
            .withSpineColor(12345) // Existing spine color
            .withPersonalRating(4.5f)
            .withPersonalNotes("Great book!")
            .withReadingStatus(ReadingStatus.READ)
            .withDateAdded(1609459200000L) // 2021-01-01
            .withPurchaseDate(1609545600000L) // 2021-01-02
            .withPurchased(true)
            .build()
        mockBookRepository.addBook(existingBook)

        // Fresh book from API (same ID, updated metadata, NO personal data)
        val freshBookFromApi = TestBookBuilder()
            .withId("book-with-metadata") // Same ID
            .withTitle("Updated Title from API")
            .withSpineColor(0) // Placeholder from search
            .withPersonalRating(0f) // API doesn't have this
            .withPersonalNotes("") // API doesn't have this
            .withReadingStatus(ReadingStatus.WANT_TO_READ) // Default
            .withDateAdded(null) // API doesn't track this
            .withPurchaseDate(null) // API doesn't track this
            .withPurchased(false) // Default
            .build()
        val shelfId = "test-shelf"
        setUpDefaultShelf(shelfId)

        // When - Add fresh API book to shelf
        val result = useCase(freshBookFromApi, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val upsertedBook = mockBookRepository.lastUpsertedBook!!

        // API data should be updated
        assertEquals("Should update title from API", "Updated Title from API", upsertedBook.title)

        // Personal metadata should be preserved from existing book
        assertEquals("Should preserve spine color", 12345, upsertedBook.spineColor)
        assertEquals("Should preserve personal rating", 4.5f, upsertedBook.personalRating, 0.01f)
        assertEquals("Should preserve personal notes", "Great book!", upsertedBook.personalNotes)
        assertEquals(
            "Should preserve reading status",
            ReadingStatus.READ,
            upsertedBook.readingStatus
        )
        assertEquals("Should preserve dateAdded", 1609459200000L, upsertedBook.dateAdded)
        assertEquals("Should preserve purchaseDate", 1609545600000L, upsertedBook.purchaseDate)
        assertTrue("Should preserve purchased flag", upsertedBook.purchased)
    }

    @Test
    fun `execute uses API data as-is for new books`() = runTest {
        // Given - Book does NOT exist
        val newBookFromApi = TestBookBuilder()
            .withId("new-book")
            .withTitle("Brand New Book")
            .withPersonalRating(0f)
            .withPersonalNotes("")
            .withReadingStatus(ReadingStatus.WANT_TO_READ)
            .withDateAdded(null)
            .withPurchaseDate(null)
            .withPurchased(false)
            .build()
        val shelfId = "test-shelf"
        setUpDefaultShelf(shelfId)

        // When
        val result = useCase(newBookFromApi, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val upsertedBook = mockBookRepository.lastUpsertedBook!!

        // Should use API data exactly as-is
        assertEquals("Should use API title", "Brand New Book", upsertedBook.title)
        assertEquals("Should use default rating", 0f, upsertedBook.personalRating, 0.01f)
        assertEquals("Should use default notes", "", upsertedBook.personalNotes)
        assertEquals(
            "Should use default reading status",
            ReadingStatus.WANT_TO_READ,
            upsertedBook.readingStatus
        )
    }

    // Book Limit Tests

    @Test
    fun `execute returns MAX_BOOKS_REACHED when shelf has 20 books`() = runTest {
        // Given - Shelf already at max capacity
        val existingBooks = (1..BookshelfConstants.MAX_BOOKS_PER_SHELF).map { i ->
            TestBookBuilder()
                .withId("book-$i")
                .withTitle("Book $i")
                .build()
        }
        val shelfId = "full-shelf"
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(shelfId)
            .withName("Full Shelf")
            .withBooks(existingBooks)
            .build()

        val newBook = TestBookBuilder()
            .withId("new-book")
            .withTitle("One Too Many")
            .build()

        // When
        val result = useCase(newBook, shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should be MAX_BOOKS_REACHED error", DataError.Local.MAX_BOOKS_REACHED, error)
        assertEquals("Should not call upsertBook", 0, mockBookRepository.upsertBookCallCount)
        assertEquals("Should not call addBookToShelf", 0, mockBookshelfRepository.addBookToShelfCallCount)
    }

    @Test
    fun `execute adds book successfully when shelf has fewer than 20 books`() = runTest {
        // Given - Shelf with only 5 books
        val existingBooks = (1..5).map { i ->
            TestBookBuilder()
                .withId("book-$i")
                .withTitle("Book $i")
                .build()
        }
        val shelfId = "partial-shelf"
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(shelfId)
            .withName("Partial Shelf")
            .withBooks(existingBooks)
            .build()

        val newBook = TestBookBuilder()
            .withId("new-book")
            .withTitle("New Book")
            .build()

        // When
        val result = useCase(newBook, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call upsertBook once", 1, mockBookRepository.upsertBookCallCount)
        assertEquals("Should call addBookToShelf once", 1, mockBookshelfRepository.addBookToShelfCallCount)
    }

    @Test
    fun `execute adds 20th book successfully at the limit boundary`() = runTest {
        // Given - Shelf with 19 books (one below max)
        val existingBooks = (1..19).map { i ->
            TestBookBuilder()
                .withId("book-$i")
                .withTitle("Book $i")
                .build()
        }
        val shelfId = "almost-full-shelf"
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(shelfId)
            .withName("Almost Full Shelf")
            .withBooks(existingBooks)
            .build()

        val book20 = TestBookBuilder()
            .withId("book-20")
            .withTitle("Book 20 - The Last One")
            .build()

        // When
        val result = useCase(book20, shelfId)

        // Then
        assertTrue("Should return success for 20th book", result is Result.Success)
        assertEquals("Should call upsertBook once", 1, mockBookRepository.upsertBookCallCount)
        assertEquals("Should call addBookToShelf once", 1, mockBookshelfRepository.addBookToShelfCallCount)
    }

    @Test
    fun `execute returns NOT_FOUND when shelf does not exist`() = runTest {
        // Given - No shelf configured (returns null)
        mockBookcaseRepository.shelfByIdToReturn = null
        val book = TestBookBuilder().withId("test-book").build()
        val shelfId = "non-existent-shelf"

        // When
        val result = useCase(book, shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should be NOT_FOUND error", DataError.Local.NOT_FOUND, error)
        assertEquals("Should not call upsertBook", 0, mockBookRepository.upsertBookCallCount)
        assertEquals("Should not call addBookToShelf", 0, mockBookshelfRepository.addBookToShelfCallCount)
    }
}
