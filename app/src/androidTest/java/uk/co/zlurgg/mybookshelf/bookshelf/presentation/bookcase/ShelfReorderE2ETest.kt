package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator

/**
 * E2E test for shelf reordering workflow.
 * Tests complete flow: ViewModel → UseCase → Repository → Database
 *
 * This is a large-scope test (Google's 10% E2E test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class ShelfReorderE2ETest {

    private lateinit var database: BookshelfDatabase
    private lateinit var viewModel: BookcaseViewModel
    private lateinit var repository: BookcaseRepositoryImpl

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-shelf-${counter++}"
    }

    @Before
    fun setup() {
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
            getShelfById = GetShelfByIdUseCaseImpl(repository)
        )

        // Create test shelves in database with specific order
        runTest {
            val shelf1 = Bookshelf("shelf-1", "First", emptyList(), ShelfStyle.DarkWood, 0)
            val shelf2 = Bookshelf("shelf-2", "Second", emptyList(), ShelfStyle.SilverMetal, 1)
            val shelf3 = Bookshelf("shelf-3", "Third", emptyList(), ShelfStyle.WhiteMetal, 2)
            val shelf4 = Bookshelf("shelf-4", "Fourth", emptyList(), ShelfStyle.GreyMetal, 3)
            repository.addShelf(shelf1)
            repository.addShelf(shelf2)
            repository.addShelf(shelf3)
            repository.addShelf(shelf4)
        }

        // Setup ViewModel with full dependency chain
        viewModel = BookcaseViewModel(useCases)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun reorderShelvesMoveFromTopToBottom() = runTest {
        // Given - Four shelves in order
        val initialState = viewModel.state.first()
        assertEquals(4, initialState.bookshelves.size)
        assertEquals("First", initialState.bookshelves[0].name)

        // When - User moves first shelf to bottom (position 3)
        val shelfToMove = initialState.bookshelves[0]
        viewModel.onAction(BookcaseAction.OnReorderShelf(bookshelf = shelfToMove, newPosition = 3))

        // Then - State should reflect new order
        val state = viewModel.state.first()
        assertEquals(4, state.bookshelves.size)
        assertEquals("Second", state.bookshelves[0].name)
        assertEquals("Third", state.bookshelves[1].name)
        assertEquals("Fourth", state.bookshelves[2].name)
        assertEquals("First", state.bookshelves[3].name)
        assertTrue(state.operationSuccess)
        assertNull(state.errorMessage)

        // And - Database should persist new positions
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals(4, allShelves.size)
        assertEquals("Second", allShelves[0].name)
        assertEquals(0, allShelves[0].position)
        assertEquals("First", allShelves[3].name)
        assertEquals(3, allShelves[3].position)
    }

    @Test
    fun reorderShelvesMoveFromBottomToTop() = runTest {
        // Given - Four shelves in order
        val initialState = viewModel.state.first()
        assertEquals(4, initialState.bookshelves.size)
        assertEquals("Fourth", initialState.bookshelves[3].name)

        // When - User moves last shelf to top (position 0)
        val shelfToMove = initialState.bookshelves[3]
        viewModel.onAction(BookcaseAction.OnReorderShelf(bookshelf = shelfToMove, newPosition = 0))

        // Then - State should reflect new order
        val state = viewModel.state.first()
        assertEquals(4, state.bookshelves.size)
        assertEquals("Fourth", state.bookshelves[0].name)
        assertEquals("First", state.bookshelves[1].name)
        assertEquals("Second", state.bookshelves[2].name)
        assertEquals("Third", state.bookshelves[3].name)

        // And - Database should persist new positions
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals("Fourth", allShelves[0].name)
        assertEquals(0, allShelves[0].position)
        assertEquals("Third", allShelves[3].name)
        assertEquals(3, allShelves[3].position)
    }

    @Test
    fun reorderShelvesMoveUpByOnePosition() = runTest {
        // Given - Four shelves in order
        val initialState = viewModel.state.first()
        assertEquals("Third", initialState.bookshelves[2].name)

        // When - User moves third shelf up by one position
        val shelfToMove = initialState.bookshelves[2]
        viewModel.onAction(BookcaseAction.OnReorderShelf(bookshelf = shelfToMove, newPosition = 1))

        // Then - State should reflect new order
        val state = viewModel.state.first()
        assertEquals("First", state.bookshelves[0].name)
        assertEquals("Third", state.bookshelves[1].name)
        assertEquals("Second", state.bookshelves[2].name)
        assertEquals("Fourth", state.bookshelves[3].name)

        // And - Database should persist new positions
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals("Third", allShelves[1].name)
        assertEquals(1, allShelves[1].position)
        assertEquals("Second", allShelves[2].name)
        assertEquals(2, allShelves[2].position)
    }

    @Test
    fun reorderShelvesMultipleReorders() = runTest {
        // Given - Four shelves in order
        val initialState = viewModel.state.first()
        assertEquals(4, initialState.bookshelves.size)

        // When - User performs multiple reorders
        val firstShelf = initialState.bookshelves[0]
        viewModel.onAction(BookcaseAction.OnReorderShelf(bookshelf = firstShelf, newPosition = 2)) // First → Third position

        val stateAfterFirst = viewModel.state.first()
        val fourthShelf = stateAfterFirst.bookshelves[3]
        viewModel.onAction(BookcaseAction.OnReorderShelf(bookshelf = fourthShelf, newPosition = 1)) // Fourth → Second position

        // Then - State should reflect final order
        val state = viewModel.state.first()
        assertEquals("Second", state.bookshelves[0].name)
        assertEquals("Fourth", state.bookshelves[1].name)
        assertEquals("First", state.bookshelves[2].name)
        assertEquals("Third", state.bookshelves[3].name)

        // And - Database should persist final positions
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals(4, allShelves.size)
        assertEquals("Second", allShelves[0].name)
        assertEquals(0, allShelves[0].position)
        assertEquals("Fourth", allShelves[1].name)
        assertEquals(1, allShelves[1].position)
    }

    @Test
    fun reorderShelvesNoOpWhenSamePosition() = runTest {
        // Given - Four shelves in order
        val initialState = viewModel.state.first()
        val originalOrder = initialState.bookshelves.map { it.name }

        // When - User "moves" shelf to same position
        val shelfToMove = initialState.bookshelves[1]
        viewModel.onAction(BookcaseAction.OnReorderShelf(bookshelf = shelfToMove, newPosition = 1))

        // Then - Order should remain unchanged
        val state = viewModel.state.first()
        val newOrder = state.bookshelves.map { it.name }
        assertEquals(originalOrder, newOrder)

        // And - Database positions should remain unchanged
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals("First", allShelves[0].name)
        assertEquals(0, allShelves[0].position)
        assertEquals("Second", allShelves[1].name)
        assertEquals(1, allShelves[1].position)
    }

    @Test
    fun reorderShelvesAfterToggleMode() = runTest {
        // Given - Reorder mode is off
        val initialState = viewModel.state.first()
        assertFalse(initialState.isReorderMode)

        // When - User toggles reorder mode and reorders
        viewModel.onAction(BookcaseAction.ToggleReorderMode)
        val modeState = viewModel.state.first()
        assertTrue(modeState.isReorderMode)

        val shelfToMove = modeState.bookshelves[0]
        viewModel.onAction(BookcaseAction.OnReorderShelf(bookshelf = shelfToMove, newPosition = 2))

        // Then - Reorder should succeed regardless of mode
        val state = viewModel.state.first()
        assertEquals("Second", state.bookshelves[0].name)
        assertEquals("Third", state.bookshelves[1].name)
        assertEquals("First", state.bookshelves[2].name)

        // And - Mode should still be active
        assertTrue(state.isReorderMode)
    }

    @Test
    fun reorderOperationCanBeReset() = runTest {
        // Given - Successful reorder
        val initialState = viewModel.state.first()
        val shelfToMove = initialState.bookshelves[0]
        viewModel.onAction(BookcaseAction.OnReorderShelf(bookshelf = shelfToMove, newPosition = 2))

        val successState = viewModel.state.first()
        assertTrue(successState.operationSuccess)

        // When - User resets operation state
        viewModel.onAction(BookcaseAction.ResetOperationState)

        // Then - Operation success should be reset
        val resetState = viewModel.state.first()
        assertFalse(resetState.operationSuccess)
        assertNull(resetState.errorMessage)
    }
}
