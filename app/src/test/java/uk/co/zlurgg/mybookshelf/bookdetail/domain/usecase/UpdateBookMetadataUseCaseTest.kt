package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

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
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncSchedulerService

/**
 * Tests for UpdateBookMetadataUseCase demonstrating personal metadata management.
 * Tests business logic:
 * - Reading status updates (Want to Read, Currently Reading, Read)
 * - Personal rating updates (0.0-5.0 validation, where 0.0 = unrated)
 * - Personal notes updates (≤5000 character validation)
 * - Purchase date updates
 * - Auto-setting dateAdded on first metadata update
 * - Error handling (validation, not found, exceptions)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateBookMetadataUseCaseTest {

    private val mockRepository = MockBookRepository()
    private val testTimeProvider = TestTimeProvider(currentTime = 1234567890L)
    private val mockSyncSchedulerService = MockSyncSchedulerService()
    private val useCase =
        UpdateBookMetadataUseCaseImpl(mockRepository, testTimeProvider, mockSyncSchedulerService)

    @After
    fun tearDown() {
        mockRepository.reset()
        testTimeProvider.setTime(0L)
        mockSyncSchedulerService.reset()
    }

    @Test
    fun `successfully updates reading status`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-1")
            .withReadingStatus(ReadingStatus.WANT_TO_READ)
            .build()
        mockRepository.addBook(existingBook)

        // When
        val result = useCase(
            bookId = "book-1",
            readingStatus = ReadingStatus.CURRENTLY_READING
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-1")
        assertEquals("Should update reading status", ReadingStatus.CURRENTLY_READING, updatedBook?.readingStatus)
    }

    @Test
    fun `successfully updates personal rating`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-2")
            .withPersonalRating(0f)
            .build()
        mockRepository.addBook(existingBook)

        // When
        val result = useCase(
            bookId = "book-2",
            personalRating = 4.5f
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-2")
        assertEquals("Should update personal rating", 4.5f, updatedBook?.personalRating ?: 0f, 0.01f)
    }

    @Test
    fun `successfully updates personal notes`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-3")
            .withPersonalNotes("")
            .build()
        mockRepository.addBook(existingBook)

        val notes = "Really enjoyed this book! The characters were well-developed."

        // When
        val result = useCase(
            bookId = "book-3",
            personalNotes = notes
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-3")
        assertEquals("Should update personal notes", notes, updatedBook?.personalNotes)
    }

    @Test
    fun `successfully updates purchase date`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-4")
            .withPurchaseDate(null)
            .build()
        mockRepository.addBook(existingBook)

        val purchaseDate = 1609459200000L // 2021-01-01

        // When
        val result = useCase(
            bookId = "book-4",
            purchaseDate = purchaseDate
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-4")
        assertEquals("Should update purchase date", purchaseDate, updatedBook?.purchaseDate)
    }

    @Test
    fun `returns validation error for invalid rating below 0_0`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-5")
            .build()
        mockRepository.addBook(existingBook)

        // When
        val result = useCase(
            bookId = "book-5",
            personalRating = -0.5f
        )

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            "Should return INVALID_FORMAT error",
            DataError.Validation.INVALID_FORMAT,
            (result as Result.Error).error
        )
        assertEquals("Should not call repository", 0, mockRepository.upsertBookCallCount)
    }

    @Test
    fun `returns validation error for invalid rating above 5_0`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-6")
            .build()
        mockRepository.addBook(existingBook)

        // When
        val result = useCase(
            bookId = "book-6",
            personalRating = 5.1f
        )

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            "Should return INVALID_FORMAT error",
            DataError.Validation.INVALID_FORMAT,
            (result as Result.Error).error
        )
        assertEquals("Should not call repository", 0, mockRepository.upsertBookCallCount)
    }

    @Test
    fun `returns validation error for notes exceeding 5000 characters`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-7")
            .build()
        mockRepository.addBook(existingBook)

        val tooLongNotes = "a".repeat(5001)

        // When
        val result = useCase(
            bookId = "book-7",
            personalNotes = tooLongNotes
        )

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals("Should return TOO_LONG error", DataError.Validation.TOO_LONG, (result as Result.Error).error)
        assertEquals("Should not call repository", 0, mockRepository.upsertBookCallCount)
    }

    @Test
    fun `returns not found error when book does not exist`() = runTest {
        // Given - no book in repository

        // When
        val result = useCase(
            bookId = "non-existent-book",
            readingStatus = ReadingStatus.READ
        )

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals("Should return NOT_FOUND error", DataError.Local.NOT_FOUND, (result as Result.Error).error)
        assertEquals("Should not call upsert", 0, mockRepository.upsertBookCallCount)
    }

    @Test
    fun `auto-sets dateAdded when not already set`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-8")
            .withDateAdded(null)
            .build()
        mockRepository.addBook(existingBook)

        testTimeProvider.setTime(1700000000000L)

        // When
        val result = useCase(
            bookId = "book-8",
            readingStatus = ReadingStatus.READ
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-8")
        assertEquals("Should auto-set dateAdded", 1700000000000L, updatedBook?.dateAdded)
    }

    @Test
    fun `preserves existing dateAdded when already set`() = runTest {
        // Given
        val originalDateAdded = 1600000000000L
        val existingBook = TestBookBuilder()
            .withId("book-9")
            .withDateAdded(originalDateAdded)
            .build()
        mockRepository.addBook(existingBook)

        testTimeProvider.setTime(1700000000000L)

        // When
        val result = useCase(
            bookId = "book-9",
            personalRating = 5.0f
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-9")
        assertEquals("Should preserve original dateAdded", originalDateAdded, updatedBook?.dateAdded)
    }

    @Test
    fun `returns error when repository returns error`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-10")
            .build()
        mockRepository.addBook(existingBook)
        mockRepository.errorToReturn = DataError.Local.DATABASE_ERROR

        // When
        val result = useCase(
            bookId = "book-10",
            readingStatus = ReadingStatus.READ
        )

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Local.DATABASE_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `successfully updates all fields at once`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-11")
            .withReadingStatus(ReadingStatus.WANT_TO_READ)
            .withPersonalRating(0f)
            .withPersonalNotes("")
            .withDateAdded(null)
            .withPurchaseDate(null)
            .build()
        mockRepository.addBook(existingBook)

        testTimeProvider.setTime(1750000000000L)
        val notes = "Amazing book with great storytelling!"
        val purchaseDate = 1640000000000L

        // When
        val result = useCase(
            bookId = "book-11",
            readingStatus = ReadingStatus.READ,
            personalRating = 4.8f,
            personalNotes = notes,
            purchaseDate = purchaseDate
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-11")!!
        assertEquals("Should update reading status", ReadingStatus.READ, updatedBook.readingStatus)
        assertEquals("Should update personal rating", 4.8f, updatedBook.personalRating, 0.01f)
        assertEquals("Should update personal notes", notes, updatedBook.personalNotes)
        assertEquals("Should update purchase date", purchaseDate, updatedBook.purchaseDate)
        assertEquals("Should auto-set dateAdded", 1750000000000L, updatedBook.dateAdded)
    }

    @Test
    fun `allows exactly 5000 character notes`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-12")
            .build()
        mockRepository.addBook(existingBook)

        val maxLengthNotes = "a".repeat(5000)

        // When
        val result = useCase(
            bookId = "book-12",
            personalNotes = maxLengthNotes
        )

        // Then
        assertTrue("Should return success for exactly 5000 characters", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-12")
        assertEquals("Should accept 5000 character notes", 5000, updatedBook?.personalNotes?.length)
    }

    @Test
    fun `allows rating of exactly 1_0`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-13")
            .build()
        mockRepository.addBook(existingBook)

        // When
        val result = useCase(
            bookId = "book-13",
            personalRating = 1.0f
        )

        // Then
        assertTrue("Should return success for rating 1.0", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-13")
        assertEquals("Should accept rating of 1.0", 1.0f, updatedBook?.personalRating ?: 0f, 0.01f)
    }

    @Test
    fun `allows rating of exactly 5_0`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-14")
            .build()
        mockRepository.addBook(existingBook)

        // When
        val result = useCase(
            bookId = "book-14",
            personalRating = 5.0f
        )

        // Then
        assertTrue("Should return success for rating 5.0", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-14")
        assertEquals("Should accept rating of 5.0", 5.0f, updatedBook?.personalRating ?: 0f, 0.01f)
    }

    @Test
    fun `allows rating of exactly 0_0 to clear rating`() = runTest {
        // Given - Book with existing rating
        val existingBook = TestBookBuilder()
            .withId("book-16")
            .withPersonalRating(3.5f)
            .build()
        mockRepository.addBook(existingBook)

        // When - Clear rating by setting to 0.0f
        val result = useCase(
            bookId = "book-16",
            personalRating = 0.0f
        )

        // Then
        assertTrue("Should return success for rating 0.0", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-16")
        assertEquals("Should accept rating of 0.0 (unrated)", 0.0f, updatedBook?.personalRating ?: 0f, 0.01f)
    }

    @Test
    fun `preserves existing values when only updating one field`() = runTest {
        // Given
        val existingBook = TestBookBuilder()
            .withId("book-15")
            .withReadingStatus(ReadingStatus.CURRENTLY_READING)
            .withPersonalRating(3.5f)
            .withPersonalNotes("Original notes")
            .withPurchaseDate(1500000000000L)
            .build()
        mockRepository.addBook(existingBook)

        // When - only update reading status
        val result = useCase(
            bookId = "book-15",
            readingStatus = ReadingStatus.READ
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = mockRepository.getStoredBook("book-15")!!
        assertEquals("Should update reading status", ReadingStatus.READ, updatedBook.readingStatus)
        assertEquals("Should preserve personal rating", 3.5f, updatedBook.personalRating, 0.01f)
        assertEquals("Should preserve personal notes", "Original notes", updatedBook.personalNotes)
        assertEquals("Should preserve purchase date", 1500000000000L, updatedBook.purchaseDate)
    }
}
