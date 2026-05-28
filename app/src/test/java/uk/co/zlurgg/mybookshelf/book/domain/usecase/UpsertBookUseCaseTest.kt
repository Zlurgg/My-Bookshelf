package uk.co.zlurgg.mybookshelf.book.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.TestTimeProvider
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

/**
 * Tests for UpsertBookUseCase demonstrating database upsert operations.
 * Tests business logic:
 * - New book insertion
 * - Existing book update
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpsertBookUseCaseTest {

    private val mockRepository = MockBookRepository()
    private val testTimeProvider = TestTimeProvider(currentTime = 1234567890L)
    private val useCase = UpsertBookUseCaseImpl(mockRepository, testTimeProvider)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    @Test
    fun `successfully inserts new book and backfills dateAdded at insert`() = runTest {
        // Given
        val newBook = TestBookBuilder()
            .withId("new-book")
            .withTitle("New Book")
            .withDateAdded(null)
            .build()
        testTimeProvider.setTime(1700000000000L)

        // When
        val result = useCase(newBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call upsert once", 1, mockRepository.upsertBookCallCount)
        val stored = mockRepository.lastUpsertedBook!!
        assertEquals("Should preserve id", newBook.id, stored.id)
        assertEquals("Should preserve title", newBook.title, stored.title)
        assertEquals(
            "dateAdded must be backfilled at insert since update use cases no longer do it",
            1700000000000L,
            stored.dateAdded,
        )
    }

    @Test
    fun `successfully updates existing book but preserves personal metadata`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("existing-book")
            .withTitle("Original Title")
            .withPurchased(false)
            .build()

        val updatedBook = existingBook.copy(
            title = "Updated Title",
            purchased = true
        )

        mockRepository.addBook(existingBook)

        // When
        val result = useCase(updatedBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val storedBook = mockRepository.getStoredBook("existing-book")
        assertEquals("Should update title", "Updated Title", storedBook?.title)
        // Personal metadata (purchased) should be preserved from existing book
        assertEquals("Should preserve purchased status", false, storedBook?.purchased)
    }

    @Test
    fun `returns error when repository returns error`() = runTest {
        // Given
        val book = TestBookBuilder().build()
        mockRepository.errorToReturn = DataError.Local.DATABASE_ERROR

        // When
        val result = useCase(book)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Local.DATABASE_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `handles book with complete data`() = runTest {
        // Given
        val completeBook = TestBookBuilder.completeBook()

        // When
        val result = useCase(completeBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val stored = mockRepository.lastUpsertedBook!!
        // dateAdded is backfilled at insert; everything else preserved as-is.
        assertEquals(
            "Should preserve all non-dateAdded fields",
            completeBook.copy(dateAdded = stored.dateAdded),
            stored,
        )
    }

    @Test
    fun `handles book with minimal data`() = runTest {
        // Given
        val minimalBook = TestBookBuilder.minimalBook()

        // When
        val result = useCase(minimalBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val stored = mockRepository.lastUpsertedBook!!
        assertEquals(
            "Should handle minimal book (dateAdded backfilled)",
            minimalBook.copy(dateAdded = stored.dateAdded),
            stored,
        )
    }

    @Test
    fun `handles purchased book correctly`() = runTest {
        // Given
        val purchasedBook = TestBookBuilder.purchasedBook()

        // When
        val result = useCase(purchasedBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should preserve purchased status", mockRepository.lastUpsertedBook?.purchased == true)
    }

    @Test
    fun `handles multiple upserts sequentially`() = runTest {
        // Given
        val book1 = TestBookBuilder().withId("book-1").withTitle("Book 1").build()
        val book2 = TestBookBuilder().withId("book-2").withTitle("Book 2").build()
        val book3 = TestBookBuilder().withId("book-3").withTitle("Book 3").build()

        // When
        useCase(book1)
        useCase(book2)
        useCase(book3)

        // Then
        assertEquals("Should call upsert 3 times", 3, mockRepository.upsertBookCallCount)
        assertEquals("Should have all books", 3, mockRepository.getAllBooks().size)
        assertTrue("Should have book 1", mockRepository.getStoredBook("book-1") != null)
        assertTrue("Should have book 2", mockRepository.getStoredBook("book-2") != null)
        assertTrue("Should have book 3", mockRepository.getStoredBook("book-3") != null)
    }

    @Test
    fun `preserves spine color when upserting`() = runTest {
        // Given
        val customSpineColor = 0xFF336699.toInt()
        val book = TestBookBuilder()
            .withId("colored-book")
            .withSpineColor(customSpineColor)
            .build()

        // When
        val result = useCase(book)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should preserve spine color", customSpineColor, mockRepository.lastUpsertedBook?.spineColor)
    }

    @Test
    fun `handles book update preserving purchased status from existing book`() = runTest {
        // Given
        val originalBook = TestBookBuilder()
            .withId("toggle-book")
            .withPurchased(false)
            .build()

        mockRepository.addBook(originalBook)

        val updatedBook = originalBook.copy(purchased = true)

        // When
        val result = useCase(updatedBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        // Personal metadata (purchased) should be preserved from existing book
        assertEquals("Should preserve purchased status", false, mockRepository.getStoredBook("toggle-book")?.purchased)
    }

    @Test
    fun `preserves personal metadata when upserting existing book`() = runTest {
        // Given - Book already exists with personal metadata
        val existingBook = TestBookBuilder()
            .withId("book-with-metadata")
            .withTitle("Old Title")
            .withPersonalRating(4.5f)
            .withPersonalNotes("Great book!")
            .withReadingStatus(ReadingStatus.FINISHED)
            .withDateAdded(1609459200000L) // 2021-01-01
            .withPurchaseDate(1609545600000L) // 2021-01-02
            .withPurchased(true)
            .build()
        mockRepository.addBook(existingBook)

        // Fresh book from API (same ID, updated title, NO personal data)
        val freshBookFromApi = TestBookBuilder()
            .withId("book-with-metadata") // Same ID
            .withTitle("Updated Title from API")
            .withPersonalRating(0f) // API doesn't have this
            .withPersonalNotes("") // API doesn't have this
            .withReadingStatus(ReadingStatus.NOT_READ) // Default
            .withDateAdded(null) // API doesn't track this
            .withPurchaseDate(null) // API doesn't track this
            .withPurchased(false) // Default
            .build()

        // When - Upsert fresh API book
        val result = useCase(freshBookFromApi)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val upsertedBook = mockRepository.lastUpsertedBook!!

        // API data should be updated
        assertEquals("Should update title from API", "Updated Title from API", upsertedBook.title)

        // Personal metadata should be preserved from existing book
        assertEquals("Should preserve personal rating", 4.5f, upsertedBook.personalRating, 0.01f)
        assertEquals("Should preserve personal notes", "Great book!", upsertedBook.personalNotes)
        assertEquals(
            "Should preserve reading status",
            ReadingStatus.FINISHED,
            upsertedBook.readingStatus
        )
        assertEquals("Should preserve dateAdded", 1609459200000L, upsertedBook.dateAdded)
        assertEquals("Should preserve purchaseDate", 1609545600000L, upsertedBook.purchaseDate)
        assertTrue("Should preserve purchased flag", upsertedBook.purchased)
    }

    @Test
    fun `uses API data as-is for new books`() = runTest {
        // Given - Book does NOT exist
        val newBookFromApi = TestBookBuilder()
            .withId("new-book")
            .withTitle("Brand New Book")
            .withPersonalRating(0f)
            .withPersonalNotes("")
            .withReadingStatus(ReadingStatus.NOT_READ)
            .withDateAdded(null)
            .withPurchaseDate(null)
            .withPurchased(false)
            .build()

        // When
        val result = useCase(newBookFromApi)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val upsertedBook = mockRepository.lastUpsertedBook!!

        // Should use API data exactly as-is
        assertEquals("Should use API title", "Brand New Book", upsertedBook.title)
        assertEquals("Should use default rating", 0f, upsertedBook.personalRating, 0.01f)
        assertEquals("Should use default notes", "", upsertedBook.personalNotes)
        assertEquals(
            "Should use default reading status",
            ReadingStatus.NOT_READ,
            upsertedBook.readingStatus
        )
    }
}
