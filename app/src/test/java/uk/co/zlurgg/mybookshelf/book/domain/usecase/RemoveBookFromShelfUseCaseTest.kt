package uk.co.zlurgg.mybookshelf.book.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookshelfRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.StubClubOperations
import uk.co.zlurgg.mybookshelf.testutil.mocks.StubCurrentUserProvider

class RemoveBookFromShelfUseCaseTest {

    private val mockBookshelfRepository = MockBookshelfRepository()
    private val mockBookcaseRepository = MockBookcaseRepository()
    private val mockClubOperations = StubClubOperations()
    private val stubCurrentUserProvider = StubCurrentUserProvider()
    private val useCase = RemoveBookFromShelfUseCaseImpl(
        mockBookshelfRepository,
        mockBookcaseRepository,
        mockClubOperations,
        stubCurrentUserProvider,
    )

    @After
    fun tearDown() {
        mockBookshelfRepository.reset()
        mockBookcaseRepository.reset()
        stubCurrentUserProvider.userId = null
    }

    @Test
    fun `execute successfully removes book from shelf`() = runTest {
        // Given
        val bookId = "book-123"
        val shelfId = "shelf-456"

        // When
        val result = useCase(bookId, shelfId)

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
        val result = useCase(bookId, shelfId)

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
        val result = useCase(nonExistentBookId, shelfId)

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
        val result = useCase(bookId, nonExistentShelfId)

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
        val result = useCase(emptyBookId, shelfId)

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
        val result = useCase(bookId, emptyShelfId)

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
        val result = useCase(bookId, shelfId)

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
        val result = useCase(complexBookId, complexShelfId)

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
        val result = useCase(bookId, firstShelf)

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
        val result = useCase(bookToRemove, shelfId)

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

    // Club permission tests

    @Test
    fun `owner can remove any book from club shelf`() = runTest {
        // Given
        val ownerId = "owner-123"
        val shelfId = "club-shelf"
        val bookId = "book-1"
        stubCurrentUserProvider.userId = ownerId
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(shelfId)
            .withIsBookClub(true)
            .withClubCode("CLUB1")
            .withClubCreatorId(ownerId)
            .build()
        mockBookshelfRepository.configureAddedByUserId(shelfId, bookId, "other-member")

        // When
        val result = useCase(bookId, shelfId)

        // Then
        assertTrue("Owner should be able to remove any book", result is Result.Success)
    }

    @Test
    fun `member can remove book they added from club shelf`() = runTest {
        // Given
        val memberId = "member-456"
        val shelfId = "club-shelf"
        val bookId = "book-1"
        stubCurrentUserProvider.userId = memberId
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(shelfId)
            .withIsBookClub(true)
            .withClubCode("CLUB1")
            .withClubCreatorId("owner-123")
            .build()
        mockBookshelfRepository.configureAddedByUserId(shelfId, bookId, memberId)

        // When
        val result = useCase(bookId, shelfId)

        // Then
        assertTrue("Member should be able to remove their own book", result is Result.Success)
    }

    @Test
    fun `member cannot remove book another member added`() = runTest {
        // Given
        val memberId = "member-456"
        val shelfId = "club-shelf"
        val bookId = "book-1"
        stubCurrentUserProvider.userId = memberId
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(shelfId)
            .withIsBookClub(true)
            .withClubCode("CLUB1")
            .withClubCreatorId("owner-123")
            .build()
        mockBookshelfRepository.configureAddedByUserId(shelfId, bookId, "other-member")

        // When
        val result = useCase(bookId, shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Local.PERMISSION_DENIED, (result as Result.Error).error)
        assertEquals("Should not call removeBookFromShelf", 0, mockBookshelfRepository.removeBookFromShelfCallCount)
    }

    @Test
    fun `guest cannot remove book from club shelf`() = runTest {
        // Given
        stubCurrentUserProvider.userId = null
        val shelfId = "club-shelf"
        val bookId = "book-1"
        mockBookcaseRepository.shelfByIdToReturn = TestShelfBuilder()
            .withId(shelfId)
            .withIsBookClub(true)
            .withClubCode("CLUB1")
            .withClubCreatorId("owner-123")
            .build()

        // When
        val result = useCase(bookId, shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Local.PERMISSION_DENIED, (result as Result.Error).error)
        assertEquals("Should not call removeBookFromShelf", 0, mockBookshelfRepository.removeBookFromShelfCallCount)
    }
}
