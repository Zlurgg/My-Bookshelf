package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.*
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

/**
 * Clean ViewModel test demonstrating proper ViewModel testing patterns:
 * - Focus on testing UI state changes and user interactions
 * - Use StateFlow collection for proper async testing
 * - Test presentation logic without complex mocking
 * - Use InstantTaskExecutorRule for StateFlow testing
 * - Use Robolectric for Android components
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookcaseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `ShowAddDialog action toggles dialog visibility`() = runTest(testDispatcher) {
        // Given - Create with minimal mock UseCases (we're testing UI logic, not business logic)
        val useCases = createMinimalUseCases()
        val viewModel = BookcaseViewModel(useCases)

        var currentState: BookcaseState? = null
        val job = launch(testDispatcher) {
            viewModel.state.collect { currentState = it }
        }
        advanceUntilIdle()

        // When - show dialog
        viewModel.onAction(BookcaseAction.ShowAddDialog(true))
        advanceUntilIdle()

        // Then
        assertTrue("Should show dialog", currentState?.showAddDialog == true)

        // When - hide dialog
        viewModel.onAction(BookcaseAction.ShowAddDialog(false))
        advanceUntilIdle()

        // Then
        assertFalse("Should hide dialog", currentState?.showAddDialog == true)
        job.cancel()
    }

    @Test
    fun `ToggleReorderMode changes reorder state`() = runTest(testDispatcher) {
        // Given
        val useCases = createMinimalUseCases()
        val viewModel = BookcaseViewModel(useCases)

        var currentState: BookcaseState? = null
        val job = launch(testDispatcher) {
            viewModel.state.collect { currentState = it }
        }
        advanceUntilIdle()

        // Initial state should be false
        assertFalse("Should start with reorder mode off", currentState?.isReorderMode == true)

        // When - toggle to enable
        viewModel.onAction(BookcaseAction.ToggleReorderMode)
        advanceUntilIdle()

        // Then
        assertTrue("Should enter reorder mode", currentState?.isReorderMode == true)

        // When - toggle again to disable
        viewModel.onAction(BookcaseAction.ToggleReorderMode)
        advanceUntilIdle()

        // Then
        assertFalse("Should exit reorder mode", currentState?.isReorderMode == true)
        job.cancel()
    }

    @Test
    fun `ResetOperationState clears error and success flags`() = runTest(testDispatcher) {
        // Given
        val useCases = createMinimalUseCases()
        val viewModel = BookcaseViewModel(useCases)

        var currentState: BookcaseState? = null
        val job = launch(testDispatcher) {
            viewModel.state.collect { currentState = it }
        }
        advanceUntilIdle()

        // When - reset operation state
        viewModel.onAction(BookcaseAction.ResetOperationState)
        advanceUntilIdle()

        // Then
        assertFalse("Should clear operation success", currentState?.operationSuccess == true)
        assertTrue("Should clear error message", currentState?.errorMessage == null)
        job.cancel()
    }

    // Helper to create minimal UseCases for UI testing
    // We focus on testing the ViewModel's UI logic, not the UseCase business logic
    private fun createMinimalUseCases() = BookcaseUseCases(
        getAllShelves = object : GetAllShelvesUseCase {
            override suspend fun execute() = kotlinx.coroutines.flow.flowOf(
                uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookcase(
                    id = "test-bookcase",
                    bookshelves = emptyList(),
                    bookCounts = emptyMap()
                )
            )
        },
        createShelf = object : CreateShelfUseCase {
            override suspend fun execute(
                name: String,
                style: ShelfStyle,
                existingShelves: List<uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf>
            ) = uk.co.zlurgg.mybookshelf.core.domain.result.Result.Success(
                uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf(
                    id = "test-id",
                    name = name,
                    books = emptyList(),
                    shelfStyle = style,
                    position = 0
                )
            )
        },
        deleteShelf = object : DeleteShelfUseCase {
            override suspend fun execute(shelfId: String) =
                uk.co.zlurgg.mybookshelf.core.domain.result.Result.Success(Unit)
            override suspend fun restore(shelf: uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf) =
                uk.co.zlurgg.mybookshelf.core.domain.result.Result.Success(Unit)
        },
        reorderShelves = object : ReorderShelvesUseCase {
            override suspend fun execute(
                shelfToMove: uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf,
                newPosition: Int,
                currentShelves: List<uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf>
            ) = uk.co.zlurgg.mybookshelf.core.domain.result.Result.Success(currentShelves)
        },
        getShelfById = object : GetShelfByIdUseCase {
            override suspend fun execute(shelfId: String) =
                uk.co.zlurgg.mybookshelf.core.domain.result.Result.Success(null)
        }
    )
}