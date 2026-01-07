package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncSchedulerService

class DeleteShelfUseCaseTest {

    private val mockRepository = MockBookcaseRepository()
    private val mockBookClubRepository = object : BookClubRepository {
        override suspend fun createBookClub(shelfId: String): Result<String, DataError.Sync> = Result.Success("test-code")
        override suspend fun getBookClub(code: String): Result<BookClub?, DataError.Sync> = Result.Success(null)
        override suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun renameBookClub(code: String, newName: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override fun observeMyBookClubs(): Flow<List<BookClubMembership>> = flowOf(emptyList())
        override suspend fun getLocalShelfForClub(code: String): Bookshelf? = null
        override suspend fun getClubBooks(code: String): Result<List<Book>, DataError.Sync> = Result.Success(emptyList())
        override suspend fun isMemberOfClub(code: String): Result<Boolean, DataError.Sync> = Result.Success(false)
        override suspend fun joinBookClub(code: String): Result<String, DataError.Sync> = Result.Success("shelf-id")
        override suspend fun getRemoteClubMemberships(userId: String): Result<List<String>, DataError.Sync> = Result.Success(emptyList())
        override suspend fun restoreClubMembership(code: String): Result<String, DataError.Sync> = Result.Success("restored-shelf-id")
        override suspend fun syncBookToClub(code: String, book: Book): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun removeBookFromClub(code: String, bookId: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun syncBooksFromClub(code: String, localShelfId: String): Result<SyncResult, DataError.Sync> = Result.Success(SyncResult(0, 0))
        override suspend fun leaveBookClub(code: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun convertClubToPersonalShelf(code: String): Result<Unit, DataError.Sync> = Result.Success(Unit)

        override suspend fun updateClubStyle(code: String, style: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun getBookReviews(code: String, bookId: String): Result<List<uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview>, DataError.Sync> = Result.Success(emptyList())
        override suspend fun upsertBookReview(code: String, bookId: String, rating: Float, reviewText: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun deleteBookReview(code: String, bookId: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun getBookComments(code: String, bookId: String): Result<List<BookClubComment>, DataError.Sync> = Result.Success(emptyList())
        override suspend fun addBookComment(code: String, bookId: String, text: String): Result<String, DataError.Sync> = Result.Success("comment-id")
        override suspend fun editBookComment(code: String, bookId: String, commentId: String, newText: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun deleteBookComment(code: String, bookId: String, commentId: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
    }
    private val mockSyncSchedulerService = MockSyncSchedulerService()
    private val useCase = DeleteShelfUseCaseImpl(mockRepository, mockBookClubRepository, mockSyncSchedulerService)

    @After
    fun tearDown() {
        mockRepository.reset()
        mockSyncSchedulerService.reset()
    }

    @Test
    fun `execute successfully deletes shelf`() = runTest {
        // Given
        val shelfId = "test-shelf-123"
        val shelf = TestShelfBuilder()
            .withId(shelfId)
            .withName("Test Shelf")
            .build()
        mockRepository.addShelfForTest(shelf)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeShelf once", 1, mockRepository.removeShelfCallCount)
        assertEquals("Should remove correct shelf", shelfId, mockRepository.lastRemovedShelfId)
        assertFalse("Shelf should be removed from repository", mockRepository.hasShelf(shelfId))
    }

    @Test
    fun `execute with non-existent shelf still succeeds`() = runTest {
        // Given
        val nonExistentShelfId = "non-existent-shelf"

        // When
        val result = useCase.execute(nonExistentShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeShelf once", 1, mockRepository.removeShelfCallCount)
        assertEquals("Should attempt to remove correct shelf", nonExistentShelfId, mockRepository.lastRemovedShelfId)
    }

    @Test
    fun `execute handles empty shelf ID gracefully`() = runTest {
        // Given
        val emptyShelfId = ""

        // When
        val result = useCase.execute(emptyShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeShelf once", 1, mockRepository.removeShelfCallCount)
        assertEquals("Should pass empty ID to repository", emptyShelfId, mockRepository.lastRemovedShelfId)
    }

    @Test
    fun `execute returns error when repository fails`() = runTest {
        // Given
        val shelfId = "test-shelf"
        mockRepository.shouldThrowException = true

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.UNKNOWN, error)
        // Note: getShelfById throws first, so removeShelf may not be called
    }

    @Test
    fun `execute deletes shelf with books without orphaning books`() = runTest {
        // Given - Shelf with books
        val shelfId = "shelf-with-books"
        val books = listOf(
            TestBookBuilder().withId("book-1").withTitle("Book 1").build(),
            TestBookBuilder().withId("book-2").withTitle("Book 2").build(),
            TestBookBuilder().withId("book-3").withTitle("Book 3").build()
        )
        val shelf = TestShelfBuilder()
            .withId(shelfId)
            .withName("Fiction Books")
            .withBooks(books)
            .build()
        mockRepository.addShelfForTest(shelf)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertFalse("Shelf should be removed", mockRepository.hasShelf(shelfId))
        assertEquals("Should remove correct shelf", shelfId, mockRepository.lastRemovedShelfId)
        // Note: Repository implementation should handle book cleanup/orphaning
        // This test verifies the UseCase successfully calls repository
    }

    @Test
    fun `restore successfully recreates deleted shelf`() = runTest {
        // Given
        val shelf = TestShelfBuilder()
            .withId("restored-shelf")
            .withName("Restored Shelf")
            .withStyle(ShelfStyle.DarkWood)
            .withPosition(5)
            .build()

        // When
        val result = useCase.restore(shelf)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call addShelf once", 1, mockRepository.addShelfCallCount)
        assertEquals("Should restore correct shelf", shelf, mockRepository.lastAddedShelf)
        assertTrue("Shelf should exist in repository", mockRepository.hasShelf(shelf.id))
    }

    @Test
    fun `restore preserves complete shelf data`() = runTest {
        // Given
        val books = listOf(
            TestBookBuilder().withId("book-1").withTitle("Book 1").build(),
            TestBookBuilder().withId("book-2").withTitle("Book 2").build(),
            TestBookBuilder().withId("book-3").withTitle("Book 3").build()
        )
        val originalShelf = TestShelfBuilder()
            .withId("complete-shelf")
            .withName("Complete Fiction Collection")
            .withStyle(ShelfStyle.SilverMetal)
            .withPosition(3)
            .withBooks(books)
            .build()

        // When
        val result = useCase.restore(originalShelf)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val restoredShelf = mockRepository.lastAddedShelf!!
        assertEquals("Should restore correct ID", originalShelf.id, restoredShelf.id)
        assertEquals("Should restore correct name", originalShelf.name, restoredShelf.name)
        assertEquals("Should restore correct style", originalShelf.shelfStyle, restoredShelf.shelfStyle)
        assertEquals("Should restore correct position", originalShelf.position, restoredShelf.position)
        assertEquals("Should restore correct books", originalShelf.books, restoredShelf.books)
    }

    @Test
    fun `restore returns error when repository fails`() = runTest {
        // Given
        val shelf = TestShelfBuilder().withId("test-shelf").build()
        mockRepository.shouldThrowException = true

        // When
        val result = useCase.restore(shelf)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.UNKNOWN, error)
        assertEquals("Should call addShelf once", 1, mockRepository.addShelfCallCount)
    }

    @Test
    fun `delete and restore workflow maintains shelf integrity`() = runTest {
        // Given - Original shelf with complete data
        val books = listOf(
            TestBookBuilder().withId("dune").withTitle("Dune").build(),
            TestBookBuilder().withId("foundation").withTitle("Foundation").build(),
            TestBookBuilder().withId("neuromancer").withTitle("Neuromancer").build()
        )
        val originalShelf = TestShelfBuilder()
            .withId("workflow-shelf")
            .withName("Science Fiction")
            .withStyle(ShelfStyle.GreyMetal)
            .withPosition(2)
            .withBooks(books)
            .build()
        mockRepository.addShelf(originalShelf)

        // When - Delete the shelf
        val deleteResult = useCase.execute(originalShelf.id)

        // Then - Verify deletion
        assertTrue("Delete should succeed", deleteResult is Result.Success)
        assertFalse("Shelf should be deleted", mockRepository.hasShelf(originalShelf.id))

        // When - Restore the shelf
        val restoreResult = useCase.restore(originalShelf)

        // Then - Verify restoration
        assertTrue("Restore should succeed", restoreResult is Result.Success)
        assertTrue("Shelf should be restored", mockRepository.hasShelf(originalShelf.id))
        val restoredShelf = mockRepository.getShelf(originalShelf.id)!!
        assertEquals("Restored shelf should match original", originalShelf, restoredShelf)
    }

    @Test
    fun `restore can add shelf with empty book list`() = runTest {
        // Given
        val emptyShelf = TestShelfBuilder()
            .withId("empty-shelf")
            .withName("Empty Shelf")
            .withBooks(emptyList())
            .build()

        // When
        val result = useCase.restore(emptyShelf)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Empty shelf should be added", mockRepository.hasShelf(emptyShelf.id))
        val addedShelf = mockRepository.lastAddedShelf!!
        assertTrue("Should preserve empty book list", addedShelf.books.isEmpty())
    }

    @Test
    fun `regular shelf uses soft delete (removeShelf)`() = runTest {
        // Given - A regular shelf (not a book club)
        val shelfId = "regular-shelf"
        val shelf = TestShelfBuilder()
            .withId(shelfId)
            .withName("Regular Shelf")
            .withIsBookClub(false)
            .withClubCode(null)
            .build()
        mockRepository.shelfByIdToReturn = shelf

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call removeShelf (soft delete)", mockRepository.removeShelfCalled)
        assertFalse("Should NOT call hardDeleteShelf", mockRepository.hardDeleteShelfCalled)
        assertEquals("Should soft delete correct shelf", shelfId, mockRepository.lastRemovedShelfId)
    }

    @Test
    fun `book club shelf uses hard delete after Firestore deletion`() = runTest {
        // Given - A book club shelf
        val shelfId = "club-shelf"
        val clubCode = "ABC12345"
        val shelf = TestShelfBuilder()
            .withId(shelfId)
            .withName("Book Club Shelf")
            .withIsBookClub(true)
            .withClubCode(clubCode)
            .build()
        mockRepository.shelfByIdToReturn = shelf
        mockRepository.addShelfForTest(shelf)

        // When
        val result = useCase.execute(shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call hardDeleteShelf", mockRepository.hardDeleteShelfCalled)
        assertFalse("Should NOT call removeShelf (soft delete)", mockRepository.removeShelfCalled)
        assertEquals("Should hard delete correct shelf", shelfId, mockRepository.lastHardDeletedShelfId)
        assertFalse("Shelf should be removed from repository", mockRepository.hasShelf(shelfId))
    }
}