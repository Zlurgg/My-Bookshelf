package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

/**
 * Tests for [UpdateBookDescriptionUseCase].
 *
 * Pure delegation use case — coverage focuses on success vs error propagation
 * and verifying the targeted-update path is called (rather than upsertBook).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateBookDescriptionUseCaseTest {

    private val mockBookRepository = MockBookRepository()
    private val useCase = UpdateBookDescriptionUseCaseImpl(mockBookRepository)

    @After
    fun tearDown() {
        mockBookRepository.reset()
    }

    @Test
    fun `delegates to repository updateDescription and returns success`() = runTest {
        // Given
        val book = TestBookBuilder().withId("book-1").build()
        mockBookRepository.addBook(book)

        // When
        val result = useCase("book-1", "A fresh description")

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals(1, mockBookRepository.updateDescriptionCallCount)
        assertEquals("book-1", mockBookRepository.lastUpdatedDescriptionBookId)
        assertEquals("A fresh description", mockBookRepository.lastUpdatedDescription)
        // Verify we did NOT round-trip through upsertBook (the anti-pattern this fix avoids).
        assertEquals(0, mockBookRepository.upsertBookCallCount)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        mockBookRepository.errorToReturn = DataError.Local.DATABASE_ERROR

        // When
        val result = useCase("book-1", "anything")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            DataError.Local.DATABASE_ERROR,
            (result as Result.Error).error
        )
    }
}
