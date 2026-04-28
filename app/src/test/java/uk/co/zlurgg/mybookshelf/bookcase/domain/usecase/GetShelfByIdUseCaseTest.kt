package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

class GetShelfByIdUseCaseTest {

    private val mockRepository = MockBookcaseRepository()
    private val useCase = GetShelfByIdUseCaseImpl(mockRepository)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    @Test
    fun `execute returns shelf when shelf exists`() = runTest {
        // Given
        val expectedShelf = TestShelfBuilder()
            .withId("test-shelf-123")
            .withName("Science Fiction")
            .withStyle(ShelfStyle.DarkWood)
            .withPosition(2)
            .build()

        mockRepository.shelfByIdToReturn = expectedShelf

        // When
        val result = useCase("test-shelf-123")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val returnedShelf = (result as Result.Success).data
        assertEquals("Should return correct shelf", expectedShelf, returnedShelf)
    }

    @Test
    fun `execute returns null when shelf does not exist`() = runTest {
        // Given
        val nonExistentShelfId = "non-existent-shelf"
        mockRepository.shelfByIdToReturn = null

        // When
        val result = useCase(nonExistentShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val returnedShelf = (result as Result.Success).data
        assertNull("Should return null for non-existent shelf", returnedShelf)
    }

    @Test
    fun `execute returns shelf with complete data`() = runTest {
        // Given
        val books = listOf(
            TestBookBuilder().withId("book-1").withTitle("Dune").build(),
            TestBookBuilder().withId("book-2").withTitle("Foundation").build()
        )
        val completeShelf = TestShelfBuilder()
            .withId("complete-shelf")
            .withName("Complete Science Fiction Collection")
            .withStyle(ShelfStyle.SilverMetal)
            .withPosition(5)
            .withBooks(books)
            .build()

        mockRepository.shelfByIdToReturn = completeShelf

        // When
        val result = useCase("complete-shelf")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val returnedShelf = (result as Result.Success).data!!
        assertEquals("Should preserve shelf ID", completeShelf.id, returnedShelf.id)
        assertEquals("Should preserve shelf name", completeShelf.name, returnedShelf.name)
        assertEquals("Should preserve shelf style", completeShelf.shelfStyle, returnedShelf.shelfStyle)
        assertEquals("Should preserve shelf position", completeShelf.position, returnedShelf.position)
        assertEquals("Should preserve shelf books", completeShelf.books, returnedShelf.books)
    }

    @Test
    fun `execute handles empty shelf ID gracefully`() = runTest {
        // Given
        val emptyShelfId = ""
        mockRepository.shelfByIdToReturn = null

        // When
        val result = useCase(emptyShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val returnedShelf = (result as Result.Success).data
        assertNull("Should return null for empty shelf ID", returnedShelf)
    }

    @Test
    fun `execute handles various shelf ID formats`() = runTest {
        // Given
        val specialCharsShelfId = "shelf-with-special_chars.123"
        val specialShelf = TestShelfBuilder()
            .withId(specialCharsShelfId)
            .withName("Special Characters Shelf")
            .build()

        mockRepository.shelfByIdToReturn = specialShelf

        // When
        val result = useCase(specialCharsShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val returnedShelf = (result as Result.Success).data!!
        assertEquals("Should handle special character shelf ID", specialCharsShelfId, returnedShelf.id)
    }

    @Test
    fun `execute returns error when repository fails`() = runTest {
        // Given
        val shelfId = "test-shelf"
        mockRepository.errorToReturn = DataError.Local.UNKNOWN

        // When
        val result = useCase(shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.UNKNOWN, error)
    }

    @Test
    fun `execute returns shelf with different shelf styles`() = runTest {
        // Test each shelf style
        val shelfStyles = ShelfStyle.entries.toTypedArray()

        for ((index, style) in shelfStyles.withIndex()) {
            // Given
            val shelfId = "shelf-style-test-$index"
            val shelf = TestShelfBuilder()
                .withId(shelfId)
                .withName("${style.name} Shelf")
                .withStyle(style)
                .build()

            mockRepository.shelfByIdToReturn = shelf

            // When
            val result = useCase(shelfId)

            // Then
            assertTrue("Should return success for $style", result is Result.Success)
            val returnedShelf = (result as Result.Success).data!!
            assertEquals("Should preserve $style style", style, returnedShelf.shelfStyle)
        }
    }

    @Test
    fun `execute returns shelf with empty book list`() = runTest {
        // Given
        val emptyShelf = TestShelfBuilder()
            .withId("empty-shelf")
            .withName("Empty Shelf")
            .withBooks(emptyList())
            .build()

        mockRepository.shelfByIdToReturn = emptyShelf

        // When
        val result = useCase("empty-shelf")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val returnedShelf = (result as Result.Success).data!!
        assertTrue("Should return shelf with empty book list", returnedShelf.books.isEmpty())
        assertEquals("Should preserve empty shelf name", "Empty Shelf", returnedShelf.name)
    }

    @Test
    fun `execute returns shelf with single book`() = runTest {
        // Given
        val singleBook = TestBookBuilder()
            .withId("lonely-book")
            .withTitle("The Only Book")
            .build()

        val shelfWithOneBook = TestShelfBuilder()
            .withId("single-book-shelf")
            .withName("Minimalist Collection")
            .withBooks(listOf(singleBook))
            .build()

        mockRepository.shelfByIdToReturn = shelfWithOneBook

        // When
        val result = useCase("single-book-shelf")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val returnedShelf = (result as Result.Success).data!!
        assertEquals("Should have exactly one book", 1, returnedShelf.books.size)
        assertEquals("Should preserve the single book", singleBook, returnedShelf.books.first())
    }

    @Test
    fun `execute with different position values`() = runTest {
        // Test different position scenarios
        val testCases = listOf(
            0, // First position
            1, // Second position
            5, // Middle position
            999, // High position
            -1 // Invalid/edge case position
        )

        for ((index, position) in testCases.withIndex()) {
            // Given
            val shelfId = "position-test-$index"
            val shelf = TestShelfBuilder()
                .withId(shelfId)
                .withName("Position $position Shelf")
                .withPosition(position)
                .build()

            mockRepository.shelfByIdToReturn = shelf

            // When
            val result = useCase(shelfId)

            // Then
            assertTrue("Should return success for position $position", result is Result.Success)
            val returnedShelf = (result as Result.Success).data!!
            assertEquals("Should preserve position $position", position, returnedShelf.position)
        }
    }
}
