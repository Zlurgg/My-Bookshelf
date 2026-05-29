package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

/**
 * Under v3 + the B1 signature shrink, ToggleBookPurchaseUseCase is a thin
 * delegator to a column-scoped UPDATE on the purchased flag — no get-then-
 * upsert, no full-row write, no returned Book. The ViewModel owns the
 * optimistic state copy from the value it already has.
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
        val result = useCase("test-book", true)

        assertTrue("Should return success", result is Result.Success)
        assertEquals(1, mockBookRepository.updatePurchasedCallCount)
        assertEquals("test-book", mockBookRepository.lastPurchasedBookId)
        assertEquals(true, mockBookRepository.lastPurchasedValue)
        assertEquals("Must not full-row upsert", 0, mockBookRepository.upsertBookCallCount)
    }

    @Test
    fun `marks purchased book as unpurchased`() = runTest {
        val result = useCase("purchased-book", false)

        assertTrue(result is Result.Success)
        assertEquals(false, mockBookRepository.lastPurchasedValue)
    }

    @Test
    fun `toggle to current status still issues the column update`() = runTest {
        // The use case does not care whether the value changed — the ViewModel
        // computes the new flag. Repository call still happens.
        val result = useCase("already-purchased", true)

        assertTrue(result is Result.Success)
        assertEquals(1, mockBookRepository.updatePurchasedCallCount)
    }

    @Test
    fun `toggle on a previewed (cache-only) book is a no-op against storage`() = runTest {
        // v3 invariant: column UPDATE on a missing row is a SQLite no-op, so a
        // previewed book never gets promoted into the library by a tap on the
        // purchased toggle (the screen gates the card under v3, but the
        // storage-layer guarantee is what the test locks in).
        val result = useCase("preview-only", true)

        assertTrue(result is Result.Success)
        assertEquals(1, mockBookRepository.updatePurchasedCallCount)
        assertNull(
            "Previewed book must NOT be promoted into storage",
            mockBookRepository.getStoredBook("preview-only")
        )
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        mockBookRepository.errorToReturn = DataError.Local.DATABASE_ERROR

        val result = useCase("test-book", true)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Local.DATABASE_ERROR, (result as Result.Error).error)
        assertEquals(0, mockBookRepository.upsertBookCallCount)
    }

    @Test
    fun `multiple toggles all hit the column update`() = runTest {
        val book = TestBookBuilder().withId("toggle-test").withPurchased(false).build()

        useCase(book.id, true)
        useCase(book.id, false)
        useCase(book.id, true)

        assertEquals(3, mockBookRepository.updatePurchasedCallCount)
        assertEquals(0, mockBookRepository.upsertBookCallCount)
    }
}
