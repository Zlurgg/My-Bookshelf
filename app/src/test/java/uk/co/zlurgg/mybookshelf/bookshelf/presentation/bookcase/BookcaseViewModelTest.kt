package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetAllShelvesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockCreateShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockDeleteShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockReorderShelvesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetShelfByIdUseCase

/**
 * ViewModel test demonstrating UI state testing with simplified inline mocks.
 * Tests focus on presentation logic and state changes, not business logic.
 * Business logic is tested in UseCase layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookcaseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createViewModel(): BookcaseViewModel {
        val useCases = BookcaseUseCases(
            getAllShelves = MockGetAllShelvesUseCase(),
            createShelf = MockCreateShelfUseCase(),
            deleteShelf = MockDeleteShelfUseCase(),
            reorderShelves = MockReorderShelvesUseCase(),
            getShelfById = MockGetShelfByIdUseCase()
        )
        return BookcaseViewModel(useCases)
    }

    @Test
    fun `ShowAddDialog action toggles dialog visibility`() = runTest(testDispatcher) {
        // Given
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When - show dialog
        val stateAfterShow = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowAddDialog(true))
        }

        // Then
        assertTrue("Should show dialog", stateAfterShow?.showAddDialog == true)

        // When - hide dialog
        val stateAfterHide = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowAddDialog(false))
        }

        // Then
        assertFalse("Should hide dialog", stateAfterHide?.showAddDialog == true)
        stateHelper.cleanup()
    }

    @Test
    fun `ToggleReorderMode changes reorder state`() = runTest(testDispatcher) {
        // Given
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Initial state should be false
        val initialState = stateHelper.awaitState()
        assertFalse("Should start with reorder mode off", initialState?.isReorderMode == true)

        // When - toggle to enable
        val stateAfterEnable = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ToggleReorderMode)
        }

        // Then
        assertTrue("Should enter reorder mode", stateAfterEnable?.isReorderMode == true)

        // When - toggle again to disable
        val stateAfterDisable = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ToggleReorderMode)
        }

        // Then
        assertFalse("Should exit reorder mode", stateAfterDisable?.isReorderMode == true)
        stateHelper.cleanup()
    }

    @Test
    fun `ResetOperationState clears error and success flags`() = runTest(testDispatcher) {
        // Given
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When - reset operation state
        val stateAfterReset = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ResetOperationState)
        }

        // Then
        assertFalse("Should clear operation success", stateAfterReset?.operationSuccess == true)
        assertTrue("Should clear error message", stateAfterReset?.errorMessage == null)
        stateHelper.cleanup()
    }
}