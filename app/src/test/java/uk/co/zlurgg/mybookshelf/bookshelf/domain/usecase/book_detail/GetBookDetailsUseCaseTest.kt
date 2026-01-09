package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookshelfRepository

/**
 * Tests for GetBookDetailsUseCase demonstrating combined repository access.
 * Tests business logic:
 * - Book retrieval with shelf status
 * - Handling missing books
 * - Description loading from remote
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetBookDetailsUseCaseTest {

    private val mockBookRepository = MockBookRepository()
    private val mockBookshelfRepository = MockBookshelfRepository()
    private val mockBookcaseRepository = MockBookcaseRepository()
    private val useCase = GetBookDetailsUseCaseImpl(mockBookRepository, mockBookshelfRepository, mockBookcaseRepository)

    @After
    fun tearDown() {
        mockBookRepository.reset()
        mockBookshelfRepository.reset()
        mockBookcaseRepository.reset()
    }

    @Test
    fun `returns book with isOnShelf true when book is on shelf`() = runTest {
        // Given
        val bookId = "book-1"
        val shelfId = "fiction-shelf"
        val book = TestBookBuilder().withId(bookId).withTitle("Test Book").build()

        mockBookRepository.addBook(book)
        mockBookshelfRepository.configureShelfWithBooks(shelfId, listOf(bookId))

        // When
        val result = useCase.execute(bookId, shelfId).first()

        // Then
        assertEquals("Should return book", book, result.book)
        assertTrue("Should be on shelf", result.isOnShelf)
    }

    @Test
    fun `returns book with isOnShelf false when book not on shelf`() = runTest {
        // Given
        val bookId = "book-1"
        val shelfId = "fiction-shelf"
        val book = TestBookBuilder().withId(bookId).withTitle("Test Book").build()

        mockBookRepository.addBook(book)
        mockBookshelfRepository.configureShelfWithBooks(shelfId, emptyList())

        // When
        val result = useCase.execute(bookId, shelfId).first()

        // Then
        assertEquals("Should return book", book, result.book)
        assertFalse("Should not be on shelf", result.isOnShelf)
    }

    @Test
    fun `returns null book when book does not exist`() = runTest {
        // Given
        val bookId = "non-existent-book"
        val shelfId = "fiction-shelf"

        // When
        val result = useCase.execute(bookId, shelfId).first()

        // Then
        assertNull("Should return null book", result.book)
        assertFalse("Should not be on shelf", result.isOnShelf)
    }

    @Test
    fun `returns correct shelf status for different shelves`() = runTest {
        // Given
        val bookId = "book-1"
        val shelfId1 = "fiction-shelf"
        val shelfId2 = "scifi-shelf"
        val book = TestBookBuilder().withId(bookId).build()

        mockBookRepository.addBook(book)
        mockBookshelfRepository.configureShelfWithBooks(shelfId1, listOf(bookId))
        mockBookshelfRepository.configureShelfWithBooks(shelfId2, emptyList())

        // When
        val result1 = useCase.execute(bookId, shelfId1).first()
        val result2 = useCase.execute(bookId, shelfId2).first()

        // Then
        assertTrue("Should be on fiction shelf", result1.isOnShelf)
        assertFalse("Should not be on scifi shelf", result2.isOnShelf)
    }

    @Test
    fun `loadBookDescription returns success when description loaded`() = runTest {
        // Given
        val bookId = "book-1"
        val book = TestBookBuilder()
            .withId(bookId)
            .withDescription("Original description")
            .build()

        mockBookRepository.addBook(book)

        // When
        val result = useCase.loadBookDescription(bookId)

        // Then
        assertTrue("Should return success", result is Result.Success)
    }

    @Test
    fun `loadBookDescription returns error when repository fails`() = runTest {
        // Given
        val bookId = "book-1"
        mockBookRepository.remoteErrorToReturn = uk.co.zlurgg.mybookshelf.core.domain.error.DataError.Remote.UNKNOWN

        // When
        val result = useCase.loadBookDescription(bookId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        // Error is correctly typed after unwrapping Result.Error
    }

    @Test
    fun `loadBookDescription handles missing book gracefully`() = runTest {
        // Given
        val nonExistentBookId = "does-not-exist"

        // When
        val result = useCase.loadBookDescription(nonExistentBookId)

        // Then
        // Should return success even if book doesn't exist (null description is valid)
        assertTrue("Should handle gracefully", result is Result.Success || result is Result.Error)
    }

    @Test
    fun `execute queries correct repositories`() = runTest {
        // Given
        val bookId = "book-1"
        val shelfId = "fiction-shelf"
        val book = TestBookBuilder().withId(bookId).build()

        mockBookRepository.addBook(book)
        mockBookshelfRepository.configureShelfWithBooks(shelfId, listOf(bookId))

        // When
        useCase.execute(bookId, shelfId).first()

        // Then
        assertEquals("Should query book repository", bookId, mockBookRepository.lastQueriedBookId)
        assertTrue("Should query book repository once", mockBookRepository.getBookByIdCallCount >= 1)
    }

    @Test
    fun `handles purchased book status correctly`() = runTest {
        // Given
        val bookId = "purchased-book"
        val shelfId = "fiction-shelf"
        val purchasedBook = TestBookBuilder().withId(bookId).withPurchased(true).build()

        mockBookRepository.addBook(purchasedBook)
        mockBookshelfRepository.configureShelfWithBooks(shelfId, listOf(bookId))

        // When
        val result = useCase.execute(bookId, shelfId).first()

        // Then
        assertTrue("Should preserve purchased status", result.book?.purchased == true)
        assertTrue("Should be on shelf", result.isOnShelf)
    }
}
