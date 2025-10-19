package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockCreateShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockDeleteShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetAllShelvesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRenameShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.onboarding.GetOrCreateTutorialShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockReorderShelvesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockUpdateShelfStyleUseCase

/**
 * ViewModel test demonstrating UI state testing with simplified inline mocks.
 * Tests focus on presentation logic and state changes, not business logic.
 * Business logic is tested in UseCase layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookshelfViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Simplified inline mocks for ViewModel UI testing
    private val mockSearchBooks = SimpleSearchBooksUseCase()
    private val mockGetShelfBooks = SimpleGetShelfBooksUseCase()
    private val mockAddBookToShelf = SimpleAddBookToShelfUseCase()
    private val mockRemoveBookFromShelf = SimpleRemoveBookFromShelfUseCase()
    private val mockUpsertBook = SimpleUpsertBookUseCase()
    private val mockShareBookshelf = SimpleShareBookshelfUseCase()
    private val mockGetShelfById = MockGetShelfByIdUseCase()

    @After
    fun tearDown() {
        mockSearchBooks.reset()
        mockGetShelfBooks.reset()
        mockAddBookToShelf.reset()
        mockRemoveBookFromShelf.reset()
        mockUpsertBook.reset()
        mockShareBookshelf.reset()
        mockGetShelfById.reset()
    }

    private fun createViewModel(shelfId: String = "test-shelf"): BookshelfViewModel {
        val bookshelfUseCases = BookshelfUseCases(
            searchBooks = mockSearchBooks,
            getShelfBooks = mockGetShelfBooks,
            addBookToShelf = mockAddBookToShelf,
            removeBookFromShelf = mockRemoveBookFromShelf,
            upsertBook = mockUpsertBook,
            shareBookshelf = mockShareBookshelf
        )
        val bookcaseUseCases = BookcaseUseCases(
            getAllShelves = MockGetAllShelvesUseCase(),
            createShelf = MockCreateShelfUseCase(),
            deleteShelf = MockDeleteShelfUseCase(),
            reorderShelves = MockReorderShelvesUseCase(),
            getShelfById = mockGetShelfById,
            renameShelf = MockRenameShelfUseCase(),
            updateShelfStyle = MockUpdateShelfStyleUseCase(),
            getOrCreateTutorialShelf = object : GetOrCreateTutorialShelfUseCase {
                override suspend fun execute(): Result<String, DataError.Local> {
                    return Result.Success("tutorial-shelf-id")
                }
            }
        )
        return BookshelfViewModel(bookshelfUseCases, bookcaseUseCases, shelfId)
    }

    @Test
    fun `share shelf success updates state correctly`() = runTest(testDispatcher) {
        // Given
        mockShareBookshelf.shouldSucceed = true
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterShare = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnShareShelf)
        }

        // Then - Share sheet opened, no success dialog shown
        assertFalse("Should clear loading flag", stateAfterShare?.isShareLoading == true)
        stateHelper.cleanup()
    }

    @Test
    fun `share shelf error updates error message`() = runTest(testDispatcher) {
        // Given
        mockShareBookshelf.shouldSucceed = false
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterShare = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnShareShelf)
        }

        // Then
        assertNotNull("Should set error message", stateAfterShare?.errorMessage)
        assertFalse("Should clear loading flag", stateAfterShare?.isShareLoading == true)
        stateHelper.cleanup()
    }

    @Test
    fun `remove book updates state with recently deleted`() = runTest(testDispatcher) {
        // Given
        val bookToRemove = TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        mockGetShelfBooks.booksToReturn = listOf(bookToRemove)
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterRemove = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnRemoveBook(bookToRemove))
        }

        // Then
        assertEquals("Should set recently deleted book", bookToRemove, stateAfterRemove?.recentlyDeleted)
        assertTrue("Should remove book from list", stateAfterRemove?.books?.none { it.id == "book-1" } == true)
        stateHelper.cleanup()
    }

    @Test
    fun `undo remove restores book to list`() = runTest(testDispatcher) {
        // Given
        val book = TestBookBuilder().withId("book-1").build()
        mockGetShelfBooks.booksToReturn = listOf(book)
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Remove book first
        stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnRemoveBook(book))
        }

        // When - undo remove
        val stateAfterUndo = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnUndoRemove)
        }

        // Then
        assertTrue("Should restore book to list", stateAfterUndo?.books?.any { it.id == "book-1" } == true)
        assertTrue("Should clear recently deleted", stateAfterUndo?.recentlyDeleted == null)
        stateHelper.cleanup()
    }

    @Test
    fun `search dialog visibility toggles correctly`() = runTest(testDispatcher) {
        // Given
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When - show dialog
        val stateAfterShow = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnSearchClick)
        }

        // Then
        assertTrue("Should show search dialog", stateAfterShow?.isSearchDialogVisible == true)

        // When - dismiss dialog
        val stateAfterDismiss = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnDismissSearchDialog)
        }

        // Then
        assertFalse("Should hide search dialog", stateAfterDismiss?.isSearchDialogVisible == true)
        stateHelper.cleanup()
    }

    @Test
    fun `cache book handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").build()
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        mockUpsertBook.shouldSucceed = false

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterClick = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnBookClick(testBook))
        }

        // Then
        assertTrue("Should set error message", stateAfterClick?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterClick?.errorMessage?.contains("Failed to cache book") == true)
        stateHelper.cleanup()
    }

    @Test
    fun `remove book handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").build()
        mockGetShelfBooks.booksToReturn = listOf(testBook)
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        mockRemoveBookFromShelf.shouldSucceed = false

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterRemove = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnRemoveBook(testBook))
        }

        // Then
        assertTrue("Should set error message", stateAfterRemove?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterRemove?.errorMessage?.contains("Failed to remove book from shelf") == true)
        stateHelper.cleanup()
    }

    @Test
    fun `add book to shelf handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").build()
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        mockAddBookToShelf.shouldSucceed = false

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterAdd = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnAddBookClick(testBook))
        }

        // Then
        assertTrue("Should set error message", stateAfterAdd?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterAdd?.errorMessage?.contains("Failed to add book to shelf") == true)
        assertFalse("Should clear loading flag", stateAfterAdd?.isLoading == true)
        stateHelper.cleanup()
    }

    @Test
    fun `load shelf details handles error correctly`() = runTest(testDispatcher) {
        // Given
        mockGetShelfById.shouldReturnError = true

        // When
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        val initialState = stateHelper.awaitState()

        // Then
        assertTrue("Should set error message", initialState?.errorMessage != null)
        assertTrue("Should contain operation context",
            initialState?.errorMessage?.contains("Failed to load shelf details") == true)
        stateHelper.cleanup()
    }

    // Note: Search error handling test skipped due to complexity of testing debounced coroutines.
    // The error handling code path is validated by other ViewModel error tests and UseCase tests.

    // Simplified inline mock implementations for UI testing
    private class SimpleSearchBooksUseCase : SearchBooksUseCase {
        var searchResultsToReturn: List<Book> = emptyList()
        var shouldFail = false

        override suspend fun execute(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?
        ): Result<List<Book>, DataError.Remote> =
            if (shouldFail) Result.Error(DataError.Remote.UNKNOWN) else Result.Success(searchResultsToReturn)

        fun reset() {
            searchResultsToReturn = emptyList()
            shouldFail = false
        }
    }

    private class SimpleGetShelfBooksUseCase : GetShelfBooksUseCase {
        var booksToReturn: List<Book> = emptyList()

        override suspend fun execute(shelfId: String): Flow<List<Book>> = flowOf(booksToReturn)

        fun reset() {
            booksToReturn = emptyList()
        }
    }

    private class SimpleAddBookToShelfUseCase : AddBookToShelfUseCase {
        var shouldSucceed = true

        override suspend fun execute(book: Book, shelfId: String): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    private class SimpleRemoveBookFromShelfUseCase : RemoveBookFromShelfUseCase {
        var shouldSucceed = true

        override suspend fun execute(bookId: String, shelfId: String): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    private class SimpleUpsertBookUseCase : UpsertBookUseCase {
        var shouldSucceed = true

        override suspend fun execute(book: Book): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    private class SimpleShareBookshelfUseCase : ShareBookshelfUseCase {
        var shouldSucceed = true

        override suspend fun execute(shelfId: String): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }
}
