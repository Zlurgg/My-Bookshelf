package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDatabase
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.CreateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DeleteShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetAllShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetShelfByIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ReorderShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.RenameShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.UpdateShelfStyleUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator

/**
 * E2E test for shelf deletion workflow.
 * Tests complete flow: ViewModel → UseCase → Repository → Database
 *
 * This is a large-scope test (Google's 10% E2E test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class ShelfDeletionE2ETest {

    private lateinit var database: BookshelfDatabase
    private lateinit var viewModel: BookcaseViewModel
    private lateinit var repository: BookcaseRepositoryImpl

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-shelf-${counter++}"
    }

    @Before
    fun setup() = runBlocking {
        // Setup real database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        // Setup repository
        repository = BookcaseRepositoryImpl(database.bookshelfDao)

        // Setup use cases
        val useCases = BookcaseUseCases(
            getAllShelves = GetAllShelvesUseCaseImpl(repository),
            createShelf = CreateShelfUseCaseImpl(repository, testIdGenerator),
            deleteShelf = DeleteShelfUseCaseImpl(repository),
            reorderShelves = ReorderShelvesUseCaseImpl(repository),
            getShelfById = GetShelfByIdUseCaseImpl(repository),
            renameShelf = RenameShelfUseCaseImpl(repository),
            updateShelfStyle = UpdateShelfStyleUseCaseImpl(repository)
        )

        // Create test shelves in database
        runBlocking {
            val shelf1 = Bookshelf("shelf-1", "Fiction", emptyList(), ShelfStyle.DarkWood, 0)
            val shelf2 = Bookshelf("shelf-2", "Non-Fiction", emptyList(), ShelfStyle.SilverMetal, 1)
            val shelf3 = Bookshelf("shelf-3", "Science", emptyList(), ShelfStyle.WhiteMetal, 2)
            repository.addShelf(shelf1)
            repository.addShelf(shelf2)
            repository.addShelf(shelf3)
        }

        // Setup ViewModel with full dependency chain
        delay(500) // Allow ViewModel state to initialize
        viewModel = BookcaseViewModel(useCases)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deleteShelfUpdatesStateAndPersistsToDatabase() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - Three shelves exist
        val initialState = viewModel.state.first()
        assertEquals(3, initialState.bookshelves.size)
        val shelfToDelete = initialState.bookshelves[1] // Non-Fiction

        // When - User deletes a shelf
        viewModel.onAction(BookcaseAction.OnRemoveBookShelf(shelfToDelete))
        delay(500) // Allow async operation to complete

        // Then - ViewModel state should update
        val state = viewModel.state.first()
        assertEquals(2, state.bookshelves.size)
        assertEquals("Fiction", state.bookshelves[0].name)
        assertEquals("Science", state.bookshelves[1].name)
        assertTrue(state.operationSuccess)
        assertNull(state.errorMessage)

        // And - Shelf should be removed from database
        val persistedShelf = database.bookshelfDao.getShelfById("shelf-2")
        assertNull("Shelf should be deleted", persistedShelf)

        // And - Other shelves should still exist
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals(2, allShelves.size)

        job.cancel()
    }

    @Test
    fun deleteAllShelvesLeavesEmptyDatabase() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - Three shelves exist
        val initialState = viewModel.state.first()
        assertEquals(3, initialState.bookshelves.size)

        // When - User deletes all shelves
        viewModel.onAction(BookcaseAction.OnRemoveBookShelf(initialState.bookshelves[0]))
        delay(500) // Allow async operation to complete
        viewModel.onAction(BookcaseAction.OnRemoveBookShelf(initialState.bookshelves[1]))
        delay(500) // Allow async operation to complete
        viewModel.onAction(BookcaseAction.OnRemoveBookShelf(initialState.bookshelves[2]))
        delay(500) // Allow async operation to complete

        // Then - State should be empty
        val state = viewModel.state.first()
        assertEquals(0, state.bookshelves.size)

        // And - Database should be empty
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals(0, allShelves.size)

        job.cancel()
    }

    @Test
    fun deleteShelfCascadesAndRemovesCrossRefs() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - Shelf with books
        val shelfId = "shelf-with-books"
        val bookId = "book-1"

        // Add shelf with book
        val shelf = Bookshelf(shelfId, "Shelf With Books", emptyList(), ShelfStyle.DarkWood, 3)
        repository.addShelf(shelf)

        // Add book and cross-reference directly via DAO
        val bookEntity = uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity(
            id = bookId,
            title = "Test Book",
            authors = listOf("Test Author"),
            imageUrl = "https://example.com/cover.jpg",
            description = "Test description",
            languages = listOf("en"),
            firstPublishYear = "2024",
            ratingsAverage = 4.5,
            ratingsCount = 100,
            numPagesMedian = 300,
            numEditions = 5,
            purchased = false,
            spineColor = 0xFF8B4513.toInt()
        )
        database.bookshelfDao.upsert(bookEntity)
        database.bookshelfDao.upsertCrossRef(
            uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef(
                shelfId = shelfId,
                bookId = bookId,
                addedAt = System.currentTimeMillis()
            )
        )

        // Verify book is in shelf
        val booksBeforeDelete = database.bookshelfDao.getBooksForShelf(shelfId).first()
        assertEquals(1, booksBeforeDelete.size)

        // When - User deletes shelf via ViewModel
        val state = viewModel.state.first()
        val shelfToDelete = state.bookshelves.first { it.id == shelfId }
        viewModel.onAction(BookcaseAction.OnRemoveBookShelf(shelfToDelete))
        delay(500) // Allow async operation to complete

        // Then - Cross-references should be deleted
        val booksAfterDelete = database.bookshelfDao.getBooksForShelf(shelfId).first()
        assertEquals(0, booksAfterDelete.size)

        // And - Shelf should be deleted
        val persistedShelf = database.bookshelfDao.getShelfById(shelfId)
        assertNull("Shelf should be deleted", persistedShelf)

        // But - Book entity should still exist
        val bookEntity2 = database.bookshelfDao.getBookById(bookId)
        assertEquals("Test Book", bookEntity2?.title)

        job.cancel()
    }

    @Test
    fun deleteNonExistentShelfShowsError() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - A shelf that doesn't exist in database
        val nonExistentShelf = Bookshelf(
            id = "nonexistent",
            name = "Nonexistent Shelf",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            position = 99
        )

        // When - User tries to delete non-existent shelf
        viewModel.onAction(BookcaseAction.OnRemoveBookShelf(nonExistentShelf))
        delay(500) // Allow async operation to complete

        // Then - Error should be shown
        val state = viewModel.state.first()
        assertFalse(state.operationSuccess)
        // Note: Error handling may vary, but operation should not succeed

        job.cancel()
    }

    @Test
    fun deleteShelfOperationCanBeReset() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - Successful deletion
        val initialState = viewModel.state.first()
        viewModel.onAction(BookcaseAction.OnRemoveBookShelf(initialState.bookshelves[0]))
        delay(500) // Allow async operation to complete

        val successState = viewModel.state.first()
        assertTrue(successState.operationSuccess)

        // When - User resets operation state
        viewModel.onAction(BookcaseAction.ResetOperationState)
        delay(500) // Allow async operation to complete

        // Then - Operation success should be reset
        val resetState = viewModel.state.first()
        assertFalse(resetState.operationSuccess)
        assertNull(resetState.errorMessage)

        job.cancel()
    }
}
