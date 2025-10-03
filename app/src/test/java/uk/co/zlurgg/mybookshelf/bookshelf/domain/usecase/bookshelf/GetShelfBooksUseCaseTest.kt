package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookshelfRepository

/**
 * Tests for GetShelfBooksUseCase demonstrating book retrieval logic.
 * Tests business logic:
 * - Empty shelf (no books)
 * - Shelf with books
 * - Multiple books retrieval
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetShelfBooksUseCaseTest {

    private val mockRepository = MockBookshelfRepository()
    private val useCase = GetShelfBooksUseCaseImpl(mockRepository)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    @Test
    fun `returns empty list when shelf has no books`() = runTest {
        // Given
        val shelfId = "empty-shelf"
        mockRepository.configureShelfWithBooks(shelfId, emptyList())

        // When
        val result = useCase.execute(shelfId).first()

        // Then
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun `returns single book when shelf has one book`() = runTest {
        // Given
        val shelfId = "fiction-shelf"
        val book = TestBookBuilder()
            .withId("book-1")
            .withTitle("Test Book")
            .build()

        mockRepository.configureBook(book)
        mockRepository.configureShelfWithBooks(shelfId, listOf("book-1"))

        // When
        val result = useCase.execute(shelfId).first()

        // Then
        assertEquals("Should have 1 book", 1, result.size)
        assertEquals("Should return correct book", book, result[0])
    }

    @Test
    fun `returns multiple books when shelf has multiple books`() = runTest {
        // Given
        val shelfId = "multi-book-shelf"
        val book1 = TestBookBuilder()
            .withId("book-1")
            .withTitle("Book One")
            .build()
        val book2 = TestBookBuilder()
            .withId("book-2")
            .withTitle("Book Two")
            .build()
        val book3 = TestBookBuilder()
            .withId("book-3")
            .withTitle("Book Three")
            .build()

        mockRepository.configureBook(book1)
        mockRepository.configureBook(book2)
        mockRepository.configureBook(book3)
        mockRepository.configureShelfWithBooks(shelfId, listOf("book-1", "book-2", "book-3"))

        // When
        val result = useCase.execute(shelfId).first()

        // Then
        assertEquals("Should have 3 books", 3, result.size)
        assertTrue("Should contain book 1", result.contains(book1))
        assertTrue("Should contain book 2", result.contains(book2))
        assertTrue("Should contain book 3", result.contains(book3))
    }

    @Test
    fun `returns correct books for specific shelf ID`() = runTest {
        // Given
        val shelf1 = "fiction-shelf"
        val shelf2 = "scifi-shelf"

        val fictionBook = TestBookBuilder().withId("fiction-book").withTitle("Fiction Book").build()
        val scifiBook = TestBookBuilder().withId("scifi-book").withTitle("Sci-Fi Book").build()

        mockRepository.configureBook(fictionBook)
        mockRepository.configureBook(scifiBook)
        mockRepository.configureShelfWithBooks(shelf1, listOf("fiction-book"))
        mockRepository.configureShelfWithBooks(shelf2, listOf("scifi-book"))

        // When
        val fictionResults = useCase.execute(shelf1).first()
        val scifiResults = useCase.execute(shelf2).first()

        // Then
        assertEquals("Fiction shelf should have 1 book", 1, fictionResults.size)
        assertEquals("Should return fiction book", fictionBook, fictionResults[0])

        assertEquals("Sci-Fi shelf should have 1 book", 1, scifiResults.size)
        assertEquals("Should return sci-fi book", scifiBook, scifiResults[0])
    }

    @Test
    fun `handles purchased books correctly`() = runTest {
        // Given
        val shelfId = "purchased-shelf"
        val purchasedBook = TestBookBuilder.purchasedBook()
        val unpurchasedBook = TestBookBuilder()
            .withId("unpurchased-book")
            .withPurchased(false)
            .build()

        mockRepository.configureBook(purchasedBook)
        mockRepository.configureBook(unpurchasedBook)
        mockRepository.configureShelfWithBooks(shelfId, listOf("purchased-book", "unpurchased-book"))

        // When
        val result = useCase.execute(shelfId).first()

        // Then
        assertEquals("Should have 2 books", 2, result.size)
        val purchased = result.find { it.id == "purchased-book" }
        val unpurchased = result.find { it.id == "unpurchased-book" }

        assertTrue("Purchased book should be marked as purchased", purchased?.purchased == true)
        assertTrue("Unpurchased book should not be marked as purchased", unpurchased?.purchased == false)
    }

    @Test
    fun `returns empty list for non-existent shelf`() = runTest {
        // Given
        val nonExistentShelfId = "does-not-exist"

        // When
        val result = useCase.execute(nonExistentShelfId).first()

        // Then
        assertTrue("Should return empty list for non-existent shelf", result.isEmpty())
    }

    @Test
    fun `handles repository configuration correctly`() = runTest {
        // Given - test that empty configuration returns empty results
        val shelfId = "unconfigured-shelf"
        // mockRepository not configured for this shelf

        // When
        val result = useCase.execute(shelfId).first()

        // Then
        assertTrue("Should return empty list for unconfigured shelf", result.isEmpty())
    }
}
