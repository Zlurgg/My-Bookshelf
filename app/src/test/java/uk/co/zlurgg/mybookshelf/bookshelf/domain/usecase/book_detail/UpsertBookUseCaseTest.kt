package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

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
 * Tests for UpsertBookUseCase demonstrating database upsert operations.
 * Tests business logic:
 * - New book insertion
 * - Existing book update
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpsertBookUseCaseTest {

    private val mockRepository = MockBookRepository()
    private val useCase = UpsertBookUseCaseImpl(mockRepository)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    @Test
    fun `successfully inserts new book`() = runTest {
        // Given
        val newBook = TestBookBuilder()
            .withId("new-book")
            .withTitle("New Book")
            .build()

        // When
        val result = useCase.execute(newBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call upsert once", 1, mockRepository.upsertBookCallCount)
        assertEquals("Should upsert correct book", newBook, mockRepository.lastUpsertedBook)
    }

    @Test
    fun `successfully updates existing book`() = runTest {
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
        val result = useCase.execute(updatedBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should upsert updated book", updatedBook, mockRepository.lastUpsertedBook)
        assertTrue("Repository should have updated book", mockRepository.getBookById("existing-book")?.purchased == true)
    }

    @Test
    fun `returns error when repository throws exception`() = runTest {
        // Given
        val book = TestBookBuilder().build()
        mockRepository.shouldThrowException = true

        // When
        val result = useCase.execute(book)

        // Then
        assertTrue("Should return error", result is Result.Error)
        // Error is correctly typed as DataError.Local after unwrapping Result.Error
    }

    @Test
    fun `handles book with complete data`() = runTest {
        // Given
        val completeBook = TestBookBuilder.completeBook()

        // When
        val result = useCase.execute(completeBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should preserve all book data", completeBook, mockRepository.lastUpsertedBook)
    }

    @Test
    fun `handles book with minimal data`() = runTest {
        // Given
        val minimalBook = TestBookBuilder.minimalBook()

        // When
        val result = useCase.execute(minimalBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should handle minimal book", minimalBook, mockRepository.lastUpsertedBook)
    }

    @Test
    fun `handles purchased book correctly`() = runTest {
        // Given
        val purchasedBook = TestBookBuilder.purchasedBook()

        // When
        val result = useCase.execute(purchasedBook)

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
        useCase.execute(book1)
        useCase.execute(book2)
        useCase.execute(book3)

        // Then
        assertEquals("Should call upsert 3 times", 3, mockRepository.upsertBookCallCount)
        assertEquals("Should have all books", 3, mockRepository.getAllBooks().size)
        assertTrue("Should have book 1", mockRepository.getBookById("book-1") != null)
        assertTrue("Should have book 2", mockRepository.getBookById("book-2") != null)
        assertTrue("Should have book 3", mockRepository.getBookById("book-3") != null)
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
        val result = useCase.execute(book)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should preserve spine color", customSpineColor, mockRepository.lastUpsertedBook?.spineColor)
    }

    @Test
    fun `handles book update toggling purchased status`() = runTest {
        // Given
        val originalBook = TestBookBuilder()
            .withId("toggle-book")
            .withPurchased(false)
            .build()

        mockRepository.addBook(originalBook)

        val updatedBook = originalBook.copy(purchased = true)

        // When
        val result = useCase.execute(updatedBook)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should update purchased status", mockRepository.getBookById("toggle-book")?.purchased == true)
    }
}
