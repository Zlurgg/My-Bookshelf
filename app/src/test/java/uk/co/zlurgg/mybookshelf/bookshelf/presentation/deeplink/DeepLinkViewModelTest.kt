package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.ImportResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper

/**
 * ViewModel test demonstrating UI state testing with simplified inline mocks.
 * Tests focus on presentation logic and state changes, not business logic.
 * Business logic is tested in UseCase layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DeepLinkViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Simplified inline mock for ViewModel UI testing
    private val mockDeepLinkImport = SimpleDeepLinkImportUseCase()

    @After
    fun tearDown() {
        mockDeepLinkImport.reset()
    }

    private fun createViewModel(): DeepLinkViewModel {
        return DeepLinkViewModel(mockDeepLinkImport)
    }

    @Test
    fun `import success updates state correctly`() = runTest(testDispatcher) {
        // Given
        mockDeepLinkImport.importResultToReturn = ImportResult.Success
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterImport = stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.ImportFromToken("valid-token"))
        }

        // Then
        assertTrue("Should set import successful flag", stateAfterImport?.importSuccessful == true)
        assertFalse("Should clear loading flag", stateAfterImport?.isLoading == true)
        assertNull("Should not have error", stateAfterImport?.error)
        stateHelper.cleanup()
    }

    @Test
    fun `import error updates error message`() = runTest(testDispatcher) {
        // Given
        mockDeepLinkImport.shouldReturnError = true
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterImport = stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.ImportFromToken("invalid-token"))
        }

        // Then
        assertNotNull("Should set error message", stateAfterImport?.error)
        assertFalse("Should clear loading flag", stateAfterImport?.isLoading == true)
        assertFalse("Should not set success flag", stateAfterImport?.importSuccessful == true)
        stateHelper.cleanup()
    }

    @Test
    fun `name conflict updates nameConflict state`() = runTest(testDispatcher) {
        // Given
        val conflictName = "Existing Shelf"
        val jsonData = "{\"test\":\"data\"}"
        mockDeepLinkImport.importResultToReturn = ImportResult.NameConflict(conflictName, jsonData)
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterImport = stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.ImportFromToken("conflict-token"))
        }

        // Then
        assertNotNull("Should set name conflict data", stateAfterImport?.nameConflict)
        assertEquals("Should have correct existing name", conflictName, stateAfterImport?.nameConflict?.existingName)
        assertEquals("Should have correct JSON data", jsonData, stateAfterImport?.nameConflict?.jsonData)
        assertFalse("Should clear loading flag", stateAfterImport?.isLoading == true)
        stateHelper.cleanup()
    }

    @Test
    fun `resolve name conflict with custom name succeeds`() = runTest(testDispatcher) {
        // Given
        val jsonData = "{\"test\":\"data\"}"
        val newName = "New Shelf Name"
        mockDeepLinkImport.customNameResultToReturn = Result.Success(Unit)
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterResolve = stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.ResolveNameConflictWithNewName(jsonData, newName))
        }

        // Then
        assertTrue("Should set import successful flag", stateAfterResolve?.importSuccessful == true)
        assertFalse("Should clear loading flag", stateAfterResolve?.isLoading == true)
        assertNull("Should clear name conflict", stateAfterResolve?.nameConflict)
        stateHelper.cleanup()
    }

    @Test
    fun `dismiss error clears error message`() = runTest(testDispatcher) {
        // Given
        mockDeepLinkImport.shouldReturnError = true
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Set error first
        stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.ImportFromToken("invalid-token"))
        }

        // When
        val stateAfterDismiss = stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.DismissError)
        }

        // Then
        assertNull("Should clear error message", stateAfterDismiss?.error)
        stateHelper.cleanup()
    }

    @Test
    fun `dismiss success clears success flag`() = runTest(testDispatcher) {
        // Given
        mockDeepLinkImport.importResultToReturn = ImportResult.Success
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Set success first
        stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.ImportFromToken("valid-token"))
        }

        // When
        val stateAfterDismiss = stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.DismissSuccess)
        }

        // Then
        assertFalse("Should clear success flag", stateAfterDismiss?.importSuccessful == true)
        stateHelper.cleanup()
    }

    @Test
    fun `dismiss name conflict clears nameConflict state`() = runTest(testDispatcher) {
        // Given
        mockDeepLinkImport.importResultToReturn = ImportResult.NameConflict("Test", "{}")
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Set conflict first
        stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.ImportFromToken("conflict-token"))
        }

        // When
        val stateAfterDismiss = stateHelper.executeAndGetState {
            viewModel.onAction(DeepLinkAction.DismissNameConflict)
        }

        // Then
        assertNull("Should clear name conflict", stateAfterDismiss?.nameConflict)
        stateHelper.cleanup()
    }

    // Simplified inline mock implementation for UI testing
    private class SimpleDeepLinkImportUseCase : DeepLinkImportUseCase {
        var importResultToReturn: ImportResult = ImportResult.Success
        var customNameResultToReturn: Result<Unit, DataError.Local> = Result.Success(Unit)
        var shouldReturnError = false

        override suspend fun importBookshelfFromToken(token: String): Result<ImportResult, DataError.Local> {
            return if (shouldReturnError) {
                Result.Error(DataError.Local.UNKNOWN)
            } else {
                Result.Success(importResultToReturn)
            }
        }

        override suspend fun importBookshelfWithCustomName(
            jsonData: String,
            customName: String
        ): Result<Unit, DataError.Local> {
            return customNameResultToReturn
        }

        fun reset() {
            importResultToReturn = ImportResult.Success
            customNameResultToReturn = Result.Success(Unit)
            shouldReturnError = false
        }
    }
}
