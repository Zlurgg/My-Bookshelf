package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

/**
 * Tests for UpdateBookMetadataUseCase.
 *
 * The use case is pure delegation under v3: validate, then call the column-scoped
 * repository method. No get-then-upsert, no dateAdded backfill (that moved to
 * insert sites). The leak-fix invariant is locked at the repository/DAO layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateBookMetadataUseCaseTest {

    private val mockRepository = MockBookRepository()
    private val useCase = UpdateBookMetadataUseCaseImpl(mockRepository)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    @Test
    fun `update reading status routes to the reading-status column with enum name`() = runTest {
        val existingBook = TestBookBuilder()
            .withId("book-1")
            .withReadingStatus(ReadingStatus.NOT_READ)
            .build()
        mockRepository.addBook(existingBook)

        val result = useCase(bookId = "book-1", readingStatus = ReadingStatus.READING)

        assertTrue("Should return success", result is Result.Success)
        assertEquals(1, mockRepository.updatePersonalMetadataCallCount)
        assertEquals("book-1", mockRepository.lastPersonalMetadataBookId)
        assertEquals("READING", mockRepository.lastPersonalMetadataReadingStatus)
        assertNull("Should not touch rating", mockRepository.lastPersonalMetadataRating)
        assertNull("Should not touch notes", mockRepository.lastPersonalMetadataNotes)
        assertEquals(0, mockRepository.upsertBookCallCount)
    }

    @Test
    fun `update personal rating routes to the rating column only`() = runTest {
        val existingBook = TestBookBuilder().withId("book-2").withPersonalRating(0f).build()
        mockRepository.addBook(existingBook)

        val result = useCase(bookId = "book-2", personalRating = 4.5f)

        assertTrue(result is Result.Success)
        assertEquals(4.5f, mockRepository.lastPersonalMetadataRating!!, 0.001f)
        assertNull(mockRepository.lastPersonalMetadataReadingStatus)
        assertNull(mockRepository.lastPersonalMetadataNotes)
    }

    @Test
    fun `update personal notes routes to the notes column only`() = runTest {
        val existingBook = TestBookBuilder().withId("book-3").withPersonalNotes("").build()
        mockRepository.addBook(existingBook)
        val notes = "Really enjoyed this book!"

        val result = useCase(bookId = "book-3", personalNotes = notes)

        assertTrue(result is Result.Success)
        assertEquals(notes, mockRepository.lastPersonalMetadataNotes)
        assertNull(mockRepository.lastPersonalMetadataReadingStatus)
        assertNull(mockRepository.lastPersonalMetadataRating)
    }

    @Test
    fun `returns validation error for rating below 0_0`() = runTest {
        mockRepository.addBook(TestBookBuilder().withId("book-5").build())

        val result = useCase(bookId = "book-5", personalRating = -0.5f)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_FORMAT, (result as Result.Error).error)
        assertEquals("Validation must fail before repository call", 0, mockRepository.updatePersonalMetadataCallCount)
    }

    @Test
    fun `returns validation error for rating above 5_0`() = runTest {
        mockRepository.addBook(TestBookBuilder().withId("book-6").build())

        val result = useCase(bookId = "book-6", personalRating = 5.1f)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_FORMAT, (result as Result.Error).error)
        assertEquals(0, mockRepository.updatePersonalMetadataCallCount)
    }

    @Test
    fun `returns validation error for notes exceeding 5000 characters`() = runTest {
        mockRepository.addBook(TestBookBuilder().withId("book-7").build())
        val tooLongNotes = "a".repeat(5001)

        val result = useCase(bookId = "book-7", personalNotes = tooLongNotes)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.TOO_LONG, (result as Result.Error).error)
        assertEquals(0, mockRepository.updatePersonalMetadataCallCount)
    }

    @Test
    fun `update on a previewed (cache-only) book is a silent no-op success`() = runTest {
        // No DB row for this id — this is the v3 invariant: the use case must not
        // promote a previewed book into storage. The repository (DAO UPDATE) is
        // the gate; the use case just calls through.
        val result = useCase(bookId = "previewed-only", readingStatus = ReadingStatus.FINISHED)

        assertTrue("UPDATE on missing row must succeed silently", result is Result.Success)
        assertEquals("Repository was still called", 1, mockRepository.updatePersonalMetadataCallCount)
        assertNull(
            "Previewed book must NOT be promoted into storage",
            mockRepository.getStoredBook("previewed-only")
        )
    }

    @Test
    fun `surfaces repository error`() = runTest {
        mockRepository.addBook(TestBookBuilder().withId("book-10").build())
        mockRepository.errorToReturn = DataError.Local.DATABASE_ERROR

        val result = useCase(bookId = "book-10", readingStatus = ReadingStatus.FINISHED)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Local.DATABASE_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `updates all three fields in a single call`() = runTest {
        val existingBook = TestBookBuilder()
            .withId("book-11")
            .withReadingStatus(ReadingStatus.NOT_READ)
            .withPersonalRating(0f)
            .withPersonalNotes("")
            .build()
        mockRepository.addBook(existingBook)

        val notes = "Amazing book with great storytelling!"
        val result = useCase(
            bookId = "book-11",
            readingStatus = ReadingStatus.FINISHED,
            personalRating = 4.8f,
            personalNotes = notes,
        )

        assertTrue(result is Result.Success)
        assertEquals(1, mockRepository.updatePersonalMetadataCallCount)
        assertEquals("FINISHED", mockRepository.lastPersonalMetadataReadingStatus)
        assertEquals(4.8f, mockRepository.lastPersonalMetadataRating!!, 0.001f)
        assertEquals(notes, mockRepository.lastPersonalMetadataNotes)
    }

    @Test
    fun `allows exactly 5000 character notes`() = runTest {
        mockRepository.addBook(TestBookBuilder().withId("book-12").build())
        val maxLengthNotes = "a".repeat(5000)

        val result = useCase(bookId = "book-12", personalNotes = maxLengthNotes)

        assertTrue(result is Result.Success)
        assertEquals(5000, mockRepository.lastPersonalMetadataNotes?.length)
    }

    @Test
    fun `allows rating of exactly 1_0`() = runTest {
        mockRepository.addBook(TestBookBuilder().withId("book-13").build())

        val result = useCase(bookId = "book-13", personalRating = 1.0f)

        assertTrue(result is Result.Success)
        assertEquals(1.0f, mockRepository.lastPersonalMetadataRating!!, 0.001f)
    }

    @Test
    fun `allows rating of exactly 5_0`() = runTest {
        mockRepository.addBook(TestBookBuilder().withId("book-14").build())

        val result = useCase(bookId = "book-14", personalRating = 5.0f)

        assertTrue(result is Result.Success)
        assertEquals(5.0f, mockRepository.lastPersonalMetadataRating!!, 0.001f)
    }

    @Test
    fun `allows rating of exactly 0_0 to clear rating`() = runTest {
        val existingBook = TestBookBuilder()
            .withId("book-16")
            .withPersonalRating(3.5f)
            .build()
        mockRepository.addBook(existingBook)

        val result = useCase(bookId = "book-16", personalRating = 0.0f)

        assertTrue(result is Result.Success)
        assertEquals(0.0f, mockRepository.lastPersonalMetadataRating!!, 0.001f)
    }
}
