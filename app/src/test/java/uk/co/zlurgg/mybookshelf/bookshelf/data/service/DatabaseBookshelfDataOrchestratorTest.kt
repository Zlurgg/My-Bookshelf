package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookshelfRepository

/**
 * Test for DatabaseBookshelfDataOrchestrator - Focused on repository coordination.
 * Tests business logic: Multi-repository coordination for export/import operations.
 * Mocks: MockBookcaseRepository, MockBookshelfRepository, MockBookRepository
 */
class DatabaseBookshelfDataOrchestratorTest {

    private lateinit var mockBookcaseRepository: MockBookcaseRepository
    private lateinit var mockBookshelfRepository: MockBookshelfRepository
    private lateinit var mockBookRepository: MockBookRepository
    private lateinit var orchestrator: DatabaseBookshelfDataOrchestrator

    @Before
    fun setup() {
        mockBookcaseRepository = MockBookcaseRepository()
        mockBookshelfRepository = MockBookshelfRepository()
        mockBookRepository = MockBookRepository()
        orchestrator = DatabaseBookshelfDataOrchestrator(
            mockBookcaseRepository,
            mockBookshelfRepository,
            mockBookRepository
        )
    }

    @Test
    fun `loadShelfForExport loads shelf with books successfully`() = runTest {
        // Given
        val shelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Fiction")
            .build()
        val book1 = TestBookBuilder().withId("book-1").withTitle("Book 1").build()
        val book2 = TestBookBuilder().withId("book-2").withTitle("Book 2").build()

        mockBookcaseRepository.configureShelves(listOf(shelf))
        mockBookshelfRepository.configureBooksForShelf("shelf-1", listOf(book1, book2))

        // When
        val result = orchestrator.loadShelfForExport("shelf-1")

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val loadedShelf = (result as Result.Success).data
        assertEquals("Should have correct shelf name", "Fiction", loadedShelf.name)
        assertEquals("Should have 2 books", 2, loadedShelf.books.size)
        assertEquals("Should have book 1", "Book 1", loadedShelf.books[0].title)
        assertEquals("Should have book 2", "Book 2", loadedShelf.books[1].title)
    }

    @Test
    fun `loadShelfForExport returns NOT_FOUND when shelf doesn't exist`() = runTest {
        // Given
        mockBookcaseRepository.configureShelves(emptyList())

        // When
        val result = orchestrator.loadShelfForExport("non-existent-id")

        // Then
        assertTrue("Should fail", result is Result.Error)
        assertEquals(
            "Should return NOT_FOUND error",
            DataError.Local.NOT_FOUND,
            (result as Result.Error).error
        )
    }

    @Test
    fun `loadShelfForExport loads shelf with empty books list`() = runTest {
        // Given
        val emptyShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Empty Shelf")
            .build()

        mockBookcaseRepository.configureShelves(listOf(emptyShelf))
        mockBookshelfRepository.configureBooksForShelf("shelf-1", emptyList())

        // When
        val result = orchestrator.loadShelfForExport("shelf-1")

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val loadedShelf = (result as Result.Success).data
        assertEquals("Should have no books", 0, loadedShelf.books.size)
    }

    @Test
    fun `importShelfToDatabase saves shelf and books successfully`() = runTest {
        // Given
        val book1 = TestBookBuilder().withId("book-1").withTitle("Book 1").build()
        val book2 = TestBookBuilder().withId("book-2").withTitle("Book 2").build()
        val shelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Imported Shelf")
            .withBooks(listOf(book1, book2))
            .build()

        // When
        val result = orchestrator.importShelfToDatabase(shelf)

        // Then
        assertTrue("Should succeed", result is Result.Success)

        // Verify all books were saved
        assertEquals("Should save book 1", book1, mockBookRepository.getStoredBook("book-1"))
        assertEquals("Should save book 2", book2, mockBookRepository.getStoredBook("book-2"))

        // Verify shelf was saved
        assertTrue("Should call addShelf", mockBookcaseRepository.addShelfCalled)

        // Verify book-shelf relationships were created
        val relations = mockBookshelfRepository.getShelfBookRelations()
        assertTrue("Should create shelf-book relations", relations.containsKey("shelf-1"))
        assertEquals("Should link 2 books", 2, relations["shelf-1"]?.size)
        assertTrue("Should link book 1", relations["shelf-1"]?.contains("book-1") == true)
        assertTrue("Should link book 2", relations["shelf-1"]?.contains("book-2") == true)
    }

    @Test
    fun `importShelfToDatabase handles shelf with no books`() = runTest {
        // Given
        val emptyShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Empty Shelf")
            .withBooks(emptyList())
            .build()

        // When
        val result = orchestrator.importShelfToDatabase(emptyShelf)

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertTrue("Should call addShelf", mockBookcaseRepository.addShelfCalled)
        assertEquals("Should not save any books", 0, mockBookRepository.getAllBooks().size)
    }

    @Test
    fun `importShelfToDatabase handles repository error`() = runTest {
        // Given
        val shelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Test Shelf")
            .build()
        mockBookcaseRepository.errorToReturn = DataError.Local.UNKNOWN

        // When
        val result = orchestrator.importShelfToDatabase(shelf)

        // Then
        assertTrue("Should fail", result is Result.Error)
        // Error will always be DataError.Local due to ErrorMapper implementation
    }

    @Test
    fun `importShelfToDatabase upserts books correctly`() = runTest {
        // Given - book already exists in repository
        val existingBook = TestBookBuilder()
            .withId("book-1")
            .withTitle("Old Title")
            .build()
        mockBookRepository.addBook(existingBook)

        val updatedBook = TestBookBuilder()
            .withId("book-1")
            .withTitle("New Title")
            .build()
        val shelf = TestShelfBuilder()
            .withId("shelf-1")
            .withBooks(listOf(updatedBook))
            .build()

        // When
        val result = orchestrator.importShelfToDatabase(shelf)

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val savedBook = mockBookRepository.getStoredBook("book-1")
        assertEquals("Should upsert with new title", "New Title", savedBook?.title)
    }

    @Test
    fun `loadShelfForExport handles multiple shelves correctly`() = runTest {
        // Given
        val shelf1 = TestShelfBuilder().withId("shelf-1").withName("Fiction").build()
        val shelf2 = TestShelfBuilder().withId("shelf-2").withName("Non-Fiction").build()
        val book1 = TestBookBuilder().withId("book-1").withTitle("Fiction Book").build()
        val book2 = TestBookBuilder().withId("book-2").withTitle("Non-Fiction Book").build()

        mockBookcaseRepository.configureShelves(listOf(shelf1, shelf2))
        mockBookshelfRepository.configureBooksForShelf("shelf-1", listOf(book1))
        mockBookshelfRepository.configureBooksForShelf("shelf-2", listOf(book2))

        // When
        val result = orchestrator.loadShelfForExport("shelf-2")

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val loadedShelf = (result as Result.Success).data
        assertEquals("Should load correct shelf", "Non-Fiction", loadedShelf.name)
        assertEquals("Should have 1 book", 1, loadedShelf.books.size)
        assertEquals("Should have correct book", "Non-Fiction Book", loadedShelf.books[0].title)
    }
}
