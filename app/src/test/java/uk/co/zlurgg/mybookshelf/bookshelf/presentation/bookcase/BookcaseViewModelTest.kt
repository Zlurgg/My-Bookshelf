package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.testutil.helpers.UseCaseTestHelper
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper

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
    private val useCaseHelper = UseCaseTestHelper()

    @After
    fun tearDown() {
        useCaseHelper.resetAll()
    }

    @Test
    fun `ShowAddDialog action toggles dialog visibility`() = runTest(testDispatcher) {
        // Given
        val viewModel = BookcaseViewModel(useCaseHelper.createBookcaseUseCases())
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
        val viewModel = BookcaseViewModel(useCaseHelper.createBookcaseUseCases())
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
        val viewModel = BookcaseViewModel(useCaseHelper.createBookcaseUseCases())
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