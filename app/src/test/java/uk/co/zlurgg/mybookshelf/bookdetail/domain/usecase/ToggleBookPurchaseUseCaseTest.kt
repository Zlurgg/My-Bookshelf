package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

/**
 * Under v3, ToggleBookPurchaseUseCase delegates to a column-scoped UPDATE on
 * the purchased flag — no get-then-upsert, no full-row write, no preservation
 * gymnastics. The returned [Book] is the input copied with the new flag; DB
 * personal metadata isn't read back because nothing else changed.
 */
class ToggleBookPurchaseUseCaseTest {

    private val mockBookRepository = MockBookRepository()
    private val useCase = ToggleBookPurchaseUseCaseImpl(mockBookRepository)

    @After
    fun tearDown() {
        mockBookRepository.reset()
    }

    @Test
    fun `marks unpurchased book as purchased via column-scoped update`() = runTest {
        val unpurchasedBook = TestBookBuilder()
            .withId("test-book")
            .withTitle("Test Book")
            .withPurchased(false)
            .build()

        val result = useCase(unpurchasedBook, true)

        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertTrue("Returned book should be purchased", updatedBook.purchased)
        assertEquals("test-book", updatedBook.id)
        assertEquals(1, mockBookRepository.updatePurchasedCallCount)
        assertEquals("test-book", mockBookRepository.lastPurchasedBookId)
        assertEquals(true, mockBookRepository.lastPurchasedValue)
        assertEquals("Must not full-row upsert", 0, mockBookRepository.upsertBookCallCount)
    }

    @Test
    fun `marks purchased book as unpurchased`() = runTest {
        val purchasedBook = TestBookBuilder()
            .withId("purchased-book")
            .withPurchased(true)
            .build()

        val result = useCase(purchasedBook, false)

        assertTrue(result is Result.Success)
        assertFalse((result as Result.Success).data.purchased)
        assertEquals(false, mockBookRepository.lastPurchasedValue)
    }

    @Test
    fun `toggle to current status still issues the column update`() = runTest {
        // The use case does not care whether the value changed — the ViewModel
        // computes the new flag. Repository call still happens.
        val purchasedBook = TestBookBuilder().withId("already-purchased").withPurchased(true).build()

        val result = useCase(purchasedBook, true)

        assertTrue(result is Result.Success)
        assertEquals(1, mockBookRepository.updatePurchasedCallCount)
    }

    @Test
    fun `returns purchased copy of the supplied book, never a DB read`() = runTest {
        val originalBook = TestBookBuilder.completeBook()

        val result = useCase(originalBook, !originalBook.purchased)

        assertTrue(result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertEquals(originalBook.id, updatedBook.id)
        assertEquals(originalBook.title, updatedBook.title)
        assertEquals(originalBook.authors, updatedBook.authors)
        assertEquals(originalBook.description, updatedBook.description)
        assertEquals(originalBook.personalRating, updatedBook.personalRating)
        assertEquals(originalBook.personalNotes, updatedBook.personalNotes)
        assertEquals(originalBook.readingStatus, updatedBook.readingStatus)
        assertEquals(!originalBook.purchased, updatedBook.purchased)
    }

    @Test
    fun `toggle on a previewed (cache-only) book is a no-op against storage`() = runTest {
        // v3 invariant: column UPDATE on a missing row is a SQLite no-op, so a
        // previewed book never gets promoted into the library by a tap on the
        // purchased toggle (the screen gates the card under v3, but the
        // storage-layer guarantee is what the test locks in).
        val previewBook = TestBookBuilder().withId("preview-only").withPurchased(false).build()

        val result = useCase(previewBook, true)

        assertTrue(result is Result.Success)
        assertEquals(1, mockBookRepository.updatePurchasedCallCount)
        assertNull(
            "Previewed book must NOT be promoted into storage",
            mockBookRepository.getStoredBook("preview-only")
        )
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        val book = TestBookBuilder().withId("test-book").build()
        mockBookRepository.errorToReturn = DataError.Local.DATABASE_ERROR

        val result = useCase(book, true)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Local.DATABASE_ERROR, (result as Result.Error).error)
        assertEquals(0, mockBookRepository.upsertBookCallCount)
    }

    @Test
    fun `multiple toggles all hit the column update`() = runTest {
        val book = TestBookBuilder().withId("toggle-test").withPurchased(false).build()

        useCase(book, true)
        useCase(book.copy(purchased = true), false)
        useCase(book, true)

        assertEquals(3, mockBookRepository.updatePurchasedCallCount)
        assertEquals(0, mockBookRepository.upsertBookCallCount)
    }
}
