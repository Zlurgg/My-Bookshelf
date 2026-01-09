package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookshelfRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncSchedulerService

class RemoveBookFromShelfUseCaseTest {

    private val mockBookshelfRepository = MockBookshelfRepository()
    private val mockBookcaseRepository = MockBookcaseRepository()
    private val mockBookClubRepository = MockBookClubRepository()
    private val mockSyncSchedulerService = MockSyncSchedulerService()
    private val useCase = RemoveBookFromShelfUseCaseImpl(
        mockBookshelfRepository,
        mockBookcaseRepository,
        mockBookClubRepository,
        mockSyncSchedulerService
    )

    @After
    fun tearDown() {
        mockBookshelfRepository.reset()
        mockBookcaseRepository.reset()
        mockBookClubRepository.reset()
        mockSyncSchedulerService.reset()
    }

    @Test
    fun `execute successfully removes book from shelf`() = runTest {
        // Given
        val bookId = "book-123"
        val shelfId = "shelf-456"

        // When
        val result = useCase.execute(bookId, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeBookFromShelf once", 1, mockBookshelfRepository.removeBookFromShelfCallCount)
        assertEquals("Should remove from correct shelf", shelfId, mockBookshelfRepository.lastRemovedShelfId)
        assertEquals("Should remove correct book", bookId, mockBookshelfRepository.lastRemovedBookId)
    }

    @Test
    fun `execute removes book from configured shelf relationship`() = runTest {
        // Given
        val bookId = "test-book"
        val shelfId = "fiction-shelf"
        mockBookshelfRepository.configureShelfWithBooks(shelfId, listOf(bookId, "other-book"))

        // When
        val result = useCase.execute(bookId, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val relations = mockBookshelfRepository.getShelfBookRelations()
        val shelfBooks = relations[shelfId] ?: emptySet()
        assertFalse("Book should be removed from shelf", shelfBooks.contains(bookId))
        assertTrue("Other books should remain", shelfBooks.contains("other-book"))
    }

    @Test
    fun `execute handles removing non-existent book from shelf gracefully`() = runTest {
        // Given
        val nonExistentBookId = "non-existent-book"
        val shelfId = "test-shelf"
        mockBookshelfRepository.configureShelfWithBooks(shelfId, listOf("existing-book"))

        // When
        val result = useCase.execute(nonExistentBookId, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeBookFromShelf once", 1, mockBookshelfRepository.removeBookFromShelfCallCount)
        assertEquals("Should attempt to remove from correct shelf", shelfId, mockBookshelfRepository.lastRemovedShelfId)
        assertEquals(
            "Should attempt to remove correct book",
            nonExistentBookId,
            mockBookshelfRepository.lastRemovedBookId
        )
    }

    @Test
    fun `execute handles removing book from non-existent shelf gracefully`() = runTest {
        // Given
        val bookId = "test-book"
        val nonExistentShelfId = "non-existent-shelf"

        // When
        val result = useCase.execute(bookId, nonExistentShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeBookFromShelf once", 1, mockBookshelfRepository.removeBookFromShelfCallCount)
        assertEquals(
            "Should attempt to remove from correct shelf",
            nonExistentShelfId,
            mockBookshelfRepository.lastRemovedShelfId
        )
        assertEquals("Should attempt to remove correct book", bookId, mockBookshelfRepository.lastRemovedBookId)
    }

    @Test
    fun `execute handles empty book ID gracefully`() = runTest {
        // Given
        val emptyBookId = ""
        val shelfId = "test-shelf"

        // When
        val result = useCase.execute(emptyBookId, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeBookFromShelf once", 1, mockBookshelfRepository.removeBookFromShelfCallCount)
        assertEquals("Should pass empty book ID", emptyBookId, mockBookshelfRepository.lastRemovedBookId)
        assertEquals("Should pass correct shelf ID", shelfId, mockBookshelfRepository.lastRemovedShelfId)
    }

    @Test
    fun `execute handles empty shelf ID gracefully`() = runTest {
        // Given
        val bookId = "test-book"
        val emptyShelfId = ""

        // When
        val result = useCase.execute(bookId, emptyShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeBookFromShelf once", 1, mockBookshelfRepository.removeBookFromShelfCallCount)
        assertEquals("Should pass correct book ID", bookId, mockBookshelfRepository.lastRemovedBookId)
        assertEquals("Should pass empty shelf ID", emptyShelfId, mockBookshelfRepository.lastRemovedShelfId)
    }

    @Test
    fun `execute returns error when repository fails`() = runTest {
        // Given
        val bookId = "test-book"
        val shelfId = "test-shelf"
        mockBookshelfRepository.errorToReturn = DataError.Local.UNKNOWN

        // When
        val result = useCase.execute(bookId, shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.UNKNOWN, error)
        assertEquals("Should call removeBookFromShelf once", 1, mockBookshelfRepository.removeBookFromShelfCallCount)
    }

    @Test
    fun `execute works with complex book and shelf IDs`() = runTest {
        // Given
        val complexBookId = "complex-book-id-with-dashes-123"
        val complexShelfId = "complex_shelf_id_with_underscores_456"

        // When
        val result = useCase.execute(complexBookId, complexShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should handle complex book ID", complexBookId, mockBookshelfRepository.lastRemovedBookId)
        assertEquals("Should handle complex shelf ID", complexShelfId, mockBookshelfRepository.lastRemovedShelfId)
    }

    @Test
    fun `execute removes book from multiple shelves independently`() = runTest {
        // Given
        val bookId = "shared-book"
        val firstShelf = "fiction-shelf"
        val secondShelf = "favorites-shelf"

        // Configure book in multiple shelves
        mockBookshelfRepository.configureShelfWithBooks(firstShelf, listOf(bookId, "other-book-1"))
        mockBookshelfRepository.configureShelfWithBooks(secondShelf, listOf(bookId, "other-book-2"))

        // When - Remove from first shelf only
        val result = useCase.execute(bookId, firstShelf)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val relations = mockBookshelfRepository.getShelfBookRelations()

        // Should be removed from first shelf
        val firstShelfBooks = relations[firstShelf] ?: emptySet()
        assertFalse("Book should be removed from first shelf", firstShelfBooks.contains(bookId))
        assertTrue("Other book should remain in first shelf", firstShelfBooks.contains("other-book-1"))

        // Should remain in second shelf (mock doesn't automatically remove from all shelves)
        val secondShelfBooks = relations[secondShelf] ?: emptySet()
        assertTrue("Book should still be in second shelf", secondShelfBooks.contains(bookId))
    }

    @Test
    fun `execute preserves shelf integrity when removing book`() = runTest {
        // Given
        val bookToRemove = "book-to-remove"
        val remainingBooks = listOf("book-1", "book-2", "book-3")
        val shelfId = "test-shelf"

        val allBooks = remainingBooks + bookToRemove
        mockBookshelfRepository.configureShelfWithBooks(shelfId, allBooks)

        // When
        val result = useCase.execute(bookToRemove, shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val relations = mockBookshelfRepository.getShelfBookRelations()
        val shelfBooks = relations[shelfId] ?: emptySet()

        assertFalse("Target book should be removed", shelfBooks.contains(bookToRemove))
        remainingBooks.forEach { bookId ->
            assertTrue("Remaining book $bookId should still be present", shelfBooks.contains(bookId))
        }
        assertEquals("Should have correct number of remaining books", remainingBooks.size, shelfBooks.size)
    }
}
