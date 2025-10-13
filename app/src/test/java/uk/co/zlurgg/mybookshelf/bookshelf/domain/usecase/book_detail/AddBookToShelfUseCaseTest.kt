package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookshelfRepository

class AddBookToShelfUseCaseTest {

    private val mockBookRepository = MockBookRepository()
    private val mockBookshelfRepository = MockBookshelfRepository()
    private val useCase = AddBookToShelfUseCaseImpl(mockBookRepository, mockBookshelfRepository)

    @After
    fun tearDown() {
        mockBookRepository.reset()
        mockBookshelfRepository.reset()
    }

    @Test
    fun `execute successfully adds book to shelf`() = runTest {
        // Given
        val book = TestBookBuilder()
            .withId("book-123")
            .withTitle("Test Book")
            .build()
        val shelfId = "shelf-456"

        // When
        val result = useCase.execute(book, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call upsertBook once", 1, mockBookRepository.upsertBookCallCount)
        assertEquals("Should call addBookToShelf once", 1, mockBookshelfRepository.addBookToShelfCallCount)
        assertEquals("Should upsert correct book", book, mockBookRepository.lastUpsertedBook)
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

        // When
        val result = useCase.execute(book, shelfId)

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

        // When
        val result = useCase.execute(updatedBook, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val storedBook = mockBookRepository.getBookById("existing-book")
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

        // When - Add to first shelf
        val firstResult = useCase.execute(book, firstShelf)
        // When - Add to second shelf
        val secondResult = useCase.execute(book, secondShelf)

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
        mockBookRepository.shouldThrowException = true

        // When
        val result = useCase.execute(book, shelfId)

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
        mockBookshelfRepository.shouldThrowException = true

        // When
        val result = useCase.execute(book, shelfId)

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

        // When
        val result = useCase.execute(book, shelfId)

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

        // When
        val result = useCase.execute(book, shelfId)

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

        // When
        val result = useCase.execute(book, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val upsertedBook = mockBookRepository.lastUpsertedBook!!
        assertEquals("Should preserve all book fields", book.title, upsertedBook.title)
        assertEquals("Should preserve authors", book.authors, upsertedBook.authors)
        assertEquals("Should preserve rating", book.averageRating, upsertedBook.averageRating)
        assertEquals("Should preserve purchase status", book.purchased, upsertedBook.purchased)
        assertEquals("Should preserve spine color", book.spineColor, upsertedBook.spineColor)
    }

    @Test
    fun `execute preserves personal metadata when book already exists`() = runTest {
        // Given - Book already exists with personal metadata
        val existingBook = TestBookBuilder()
            .withId("book-with-metadata")
            .withTitle("Old Title")
            .withPersonalRating(4.5f)
            .withPersonalNotes("Great book!")
            .withReadingStatus(uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.READ)
            .withDateAdded(1609459200000L) // 2021-01-01
            .withPurchaseDate(1609545600000L) // 2021-01-02
            .withPurchased(true)
            .build()
        mockBookRepository.addBook(existingBook)

        // Fresh book from API (same ID, updated metadata, NO personal data)
        val freshBookFromApi = TestBookBuilder()
            .withId("book-with-metadata") // Same ID
            .withTitle("Updated Title from API")
            .withPersonalRating(0f) // API doesn't have this
            .withPersonalNotes("") // API doesn't have this
            .withReadingStatus(uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.WANT_TO_READ) // Default
            .withDateAdded(null) // API doesn't track this
            .withPurchaseDate(null) // API doesn't track this
            .withPurchased(false) // Default
            .build()
        val shelfId = "test-shelf"

        // When - Add fresh API book to shelf
        val result = useCase.execute(freshBookFromApi, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val upsertedBook = mockBookRepository.lastUpsertedBook!!

        // API data should be updated
        assertEquals("Should update title from API", "Updated Title from API", upsertedBook.title)

        // Personal metadata should be preserved from existing book
        assertEquals("Should preserve personal rating", 4.5f, upsertedBook.personalRating, 0.01f)
        assertEquals("Should preserve personal notes", "Great book!", upsertedBook.personalNotes)
        assertEquals("Should preserve reading status", uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.READ, upsertedBook.readingStatus)
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
            .withReadingStatus(uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.WANT_TO_READ)
            .withDateAdded(null)
            .withPurchaseDate(null)
            .withPurchased(false)
            .build()
        val shelfId = "test-shelf"

        // When
        val result = useCase.execute(newBookFromApi, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val upsertedBook = mockBookRepository.lastUpsertedBook!!

        // Should use API data exactly as-is
        assertEquals("Should use API title", "Brand New Book", upsertedBook.title)
        assertEquals("Should use default rating", 0f, upsertedBook.personalRating, 0.01f)
        assertEquals("Should use default notes", "", upsertedBook.personalNotes)
        assertEquals("Should use default reading status", uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.WANT_TO_READ, upsertedBook.readingStatus)
    }
}