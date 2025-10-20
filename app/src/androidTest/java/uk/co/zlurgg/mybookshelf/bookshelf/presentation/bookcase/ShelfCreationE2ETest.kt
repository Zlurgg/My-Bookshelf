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
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.CreateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DeleteShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetAllShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetShelfByIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ReorderShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.RenameShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.UpdateShelfStyleUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.HandleTutorialAccessUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * E2E test for shelf creation workflow.
 * Tests complete flow: ViewModel → UseCase → Repository → Database
 *
 * This is a large-scope test (Google's 10% E2E test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class ShelfCreationE2ETest {

    private lateinit var database: BookshelfDatabase
    private lateinit var viewModel: BookcaseViewModel

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-shelf-${counter++}"
    }

    private val testTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1000L
    }

    // Stub external dependencies only
    private val stubRemoteDataSource = object : RemoteBookDataSource {
        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            sort: String?
        ): Result<SearchResponseDto, DataError.Remote> {
            throw NotImplementedError("Not used in E2E tests")
        }

        override suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote> {
            throw NotImplementedError("Not used in E2E tests")
        }
    }

    @Before
    fun setup() = runBlocking {
        // Setup real database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        // Setup repositories
        val bookcaseRepository = BookcaseRepositoryImpl(database.bookshelfDao)
        val bookshelfRepository = BookshelfRepositoryImpl(database.bookshelfDao, testTimeProvider)
        val bookRepository = BookRepositoryImpl(stubRemoteDataSource, database.bookshelfDao)

        // Setup tutorial use cases
        val getOrCreateTutorialBook = GetOrCreateTutorialBookUseCaseImpl(
            bookRepository,
            bookshelfRepository,
            testTimeProvider
        )

        val getOrCreateTutorialShelf = GetOrCreateTutorialShelfUseCaseImpl(
            bookcaseRepository,
            testIdGenerator,
            getOrCreateTutorialBook
        )

        val handleTutorialAccess = HandleTutorialAccessUseCaseImpl(
            bookcaseRepository,
            getOrCreateTutorialShelf,
            getOrCreateTutorialBook
        )

        // Setup use cases
        val bookcaseUseCases = BookcaseUseCases(
            getAllShelves = GetAllShelvesUseCaseImpl(bookcaseRepository),
            createShelf = CreateShelfUseCaseImpl(bookcaseRepository, testIdGenerator, getOrCreateTutorialBook),
            deleteShelf = DeleteShelfUseCaseImpl(bookcaseRepository),
            reorderShelves = ReorderShelvesUseCaseImpl(bookcaseRepository),
            getShelfById = GetShelfByIdUseCaseImpl(bookcaseRepository),
            renameShelf = RenameShelfUseCaseImpl(bookcaseRepository),
            updateShelfStyle = UpdateShelfStyleUseCaseImpl(bookcaseRepository)
        )

        // Setup ViewModel with full dependency chain
        viewModel = BookcaseViewModel(bookcaseUseCases, handleTutorialAccess)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createShelfUpdatesStateAndPersistsToDatabase() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - Initial state with no shelves
        val initialState = viewModel.state.first()
        assertEquals(0, initialState.bookshelves.size)
        assertFalse(initialState.showAddDialog)

        // When - User creates a new shelf
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Fiction", ShelfStyle.DarkWood))
        delay(500) // Allow async operation to complete

        // Then - ViewModel state should update
        val updatedState = viewModel.state.first()
        assertEquals(1, updatedState.bookshelves.size)
        assertEquals("Fiction", updatedState.bookshelves[0].name)
        assertEquals(ShelfStyle.DarkWood, updatedState.bookshelves[0].shelfStyle)
        assertTrue(updatedState.operationSuccess)
        assertNull(updatedState.errorMessage)

        // And - Shelf should persist in database
        val persistedShelf = database.bookshelfDao.getShelfById("test-shelf-0")
        assertEquals("Fiction", persistedShelf?.name)
        assertEquals("DarkWood", persistedShelf?.shelfMaterial)

        job.cancel()
    }

    @Test
    fun createMultipleShelvesAssignsCorrectPositions() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - No existing shelves
        val initialState = viewModel.state.first()
        assertEquals(0, initialState.bookshelves.size)

        // When - User creates three shelves
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Fiction", ShelfStyle.DarkWood))
        delay(500) // Allow async operation to complete
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Nonfiction", ShelfStyle.SilverMetal))
        delay(500) // Allow async operation to complete
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Science", ShelfStyle.WhiteMetal))
        delay(500) // Allow async operation to complete

        // Then - All shelves should be in state
        val state = viewModel.state.first()
        assertEquals(3, state.bookshelves.size)

        // And - Positions should be assigned correctly (0, 1, 2)
        assertEquals(0, state.bookshelves[0].position)
        assertEquals(1, state.bookshelves[1].position)
        assertEquals(2, state.bookshelves[2].position)

        // And - Order should match in database
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals(3, allShelves.size)
        assertEquals("Fiction", allShelves[0].name)
        assertEquals("Nonfiction", allShelves[1].name)
        assertEquals("Science", allShelves[2].name)

        job.cancel()
    }

    @Test
    fun createShelfWithBlankNameShowsError() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - Initial state
        val initialState = viewModel.state.first()
        assertEquals(0, initialState.bookshelves.size)

        // When - User tries to create shelf with blank name
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("   ", ShelfStyle.DarkWood))
        delay(500) // Allow async operation to complete

        // Then - Error should be shown
        val state = viewModel.state.first()
        assertEquals(0, state.bookshelves.size)
        assertFalse(state.operationSuccess)
        assertTrue(state.errorMessage!!.contains("blank") || state.errorMessage.contains("empty"))

        // And - No shelf should be in database
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals(0, allShelves.size)

        job.cancel()
    }

    @Test
    fun addDialogVisibilityToggles() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - Dialog initially hidden
        val initialState = viewModel.state.first()
        assertFalse(initialState.showAddDialog)

        // When - User opens add dialog
        viewModel.onAction(BookcaseAction.ShowAddDialog(true))
        delay(500) // Allow async operation to complete

        // Then - Dialog should be visible
        val openedState = viewModel.state.first()
        assertTrue(openedState.showAddDialog)

        // When - User closes dialog
        viewModel.onAction(BookcaseAction.ShowAddDialog(false))
        delay(500) // Allow async operation to complete

        // Then - Dialog should be hidden
        val closedState = viewModel.state.first()
        assertFalse(closedState.showAddDialog)

        job.cancel()
    }

    @Test
    fun operationSuccessCanBeReset() = runBlocking {
        // Setup state collection
        val job = launch { viewModel.state.collect {} }

        // Given - Shelf created successfully
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Fiction", ShelfStyle.DarkWood))
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
