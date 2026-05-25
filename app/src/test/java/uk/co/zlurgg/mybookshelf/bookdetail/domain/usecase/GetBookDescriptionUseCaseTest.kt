package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

/**
 * Tests for [GetBookDescriptionUseCase].
 *
 * Pure delegation use case — coverage focuses on success vs error propagation.
 * Ported from the previous `GetBookDetailsUseCaseTest.loadBookDescription*` tests,
 * which were removed when the description fetch was split into its own use case
 * (see 1.4 remediation).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetBookDescriptionUseCaseTest {

    private val mockBookRepository = MockBookRepository()
    private val useCase = GetBookDescriptionUseCaseImpl(mockBookRepository)

    @After
    fun tearDown() {
        mockBookRepository.reset()
    }

    @Test
    fun `returns success with description when repository succeeds`() = runTest {
        // Given
        val bookId = "book-1"
        val book = TestBookBuilder()
            .withId(bookId)
            .withDescription("A great book")
            .build()
        mockBookRepository.addBook(book)

        // When
        val result = useCase(bookId, BookProvider.GOOGLE_BOOKS)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("A great book", (result as Result.Success).data)
    }

    @Test
    fun `returns error when repository returns remote error`() = runTest {
        // Given
        mockBookRepository.remoteErrorToReturn = DataError.Remote.TOO_MANY_REQUESTS

        // When
        val result = useCase("book-1", BookProvider.GOOGLE_BOOKS)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            "Should propagate remote error verbatim",
            DataError.Remote.TOO_MANY_REQUESTS,
            (result as Result.Error).error
        )
    }

    @Test
    fun `returns success with null when no book exists`() = runTest {
        // When
        val result = useCase("does-not-exist", BookProvider.GOOGLE_BOOKS)

        // Then — mock returns null description for an unknown id
        assertTrue("Should return success", result is Result.Success)
    }
}
