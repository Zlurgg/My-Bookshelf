package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

class DeleteShelfUseCaseTest {

    private val mockRepository = MockBookcaseRepository()
    private val mockClubOperations = object : ClubOperations {
        override suspend fun createBookClub(shelfId: String, shelfName: String) = throw NotImplementedError()
        override suspend fun lookupBookClub(codeOrUrl: String) = throw NotImplementedError()
        override suspend fun joinBookClub() = throw NotImplementedError()
        override suspend fun joinBookClub(code: String) = throw NotImplementedError()
        override fun clearLookupState() = throw NotImplementedError()
        override fun generateInviteLink(clubCode: String, shelfName: String) = throw NotImplementedError()
        override suspend fun syncBooksFromClub(clubCode: String, localShelfId: String) = throw NotImplementedError()
        override suspend fun leaveBookClub(shelfId: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun validateMemberships() = throw NotImplementedError()
        override suspend fun deleteBookClub(clubCode: String): Result<Unit, DataError.Sync> = Result.Success(Unit)
        override suspend fun syncBookToClub(clubCode: String, book: Book) = throw NotImplementedError()
        override suspend fun removeBookFromClub(clubCode: String, bookId: String) = throw NotImplementedError()
        override suspend fun updateClubStyle(clubCode: String, styleName: String) = throw NotImplementedError()
        override suspend fun clearAllMemberships(): Result<Unit, DataError.Local> = Result.Success(Unit)
        override suspend fun renameBookClub(clubCode: String, newName: String) = throw NotImplementedError()
        override suspend fun getClubsCreatedByUser(userId: String) = throw NotImplementedError()
        override suspend fun getClubMembershipsForUser(userId: String) = throw NotImplementedError()
        override suspend fun removeUserFromClub(clubCode: String, userId: String) = throw NotImplementedError()
    }
    private val useCase = DeleteShelfUseCaseImpl(mockRepository, mockClubOperations)

    @After
    fun tearDown() {
        mockRepository.reset()
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
        val result = useCase(shelfId)

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
        val result = useCase(nonExistentShelfId)

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
        val result = useCase(emptyShelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call removeShelf once", 1, mockRepository.removeShelfCallCount)
        assertEquals("Should pass empty ID to repository", emptyShelfId, mockRepository.lastRemovedShelfId)
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
        // Note: getShelfById returns error first, so removeShelf may not be called
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
        val result = useCase(shelfId)

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
        mockRepository.errorToReturn = DataError.Local.UNKNOWN

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
        val deleteResult = useCase(originalShelf.id)

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
        val result = useCase(shelfId)

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
        val result = useCase(shelfId)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call hardDeleteShelf", mockRepository.hardDeleteShelfCalled)
        assertFalse("Should NOT call removeShelf (soft delete)", mockRepository.removeShelfCalled)
        assertEquals("Should hard delete correct shelf", shelfId, mockRepository.lastHardDeletedShelfId)
        assertFalse("Shelf should be removed from repository", mockRepository.hasShelf(shelfId))
    }
}
