package uk.co.zlurgg.mybookshelf.bookshelf.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.usecase.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchLibraryBooksUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.GetShelfBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.UpdateShelfTidyModeUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferenceState
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.StubClubOperations
import uk.co.zlurgg.mybookshelf.testutil.mocks.StubSearchPreferences

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
    private val mockSearchLibraryBooks = SimpleSearchLibraryBooksUseCase()
    private val mockGetShelfBooks = SimpleGetShelfBooksUseCase()
    private val mockAddBookToShelf = SimpleAddBookToShelfUseCase()
    private val mockRemoveBookFromShelf = SimpleRemoveBookFromShelfUseCase()
    private val mockUpdateShelfTidyMode = SimpleUpdateShelfTidyModeUseCase()
    private val mockGetShelfById = MockGetShelfByIdUseCase()
    private val stubSearchPreferences = StubSearchPreferences()
    private val stubCheckSignInStatus = object : CheckSignInStatusUseCase {
        override suspend fun invoke(): Boolean = false
    }

    @After
    fun tearDown() {
        mockSearchBooks.reset()
        mockSearchLibraryBooks.reset()
        mockGetShelfBooks.reset()
        mockAddBookToShelf.reset()
        mockRemoveBookFromShelf.reset()
        mockGetShelfById.reset()
        stubSearchPreferences.reset()
    }

    private val stubClubOperations = StubClubOperations()

    private fun createViewModel(shelfId: String = "test-shelf"): BookshelfViewModel {
        val bookshelfUseCases = BookshelfUseCases(
            searchBooks = mockSearchBooks,
            searchLibraryBooks = mockSearchLibraryBooks,
            getShelfBooks = mockGetShelfBooks,
            addBookToShelf = mockAddBookToShelf,
            removeBookFromShelf = mockRemoveBookFromShelf,
            updateShelfTidyMode = mockUpdateShelfTidyMode
        )
        return BookshelfViewModel(
            bookshelfUseCases,
            mockGetShelfById,
            stubClubOperations,
            stubCheckSignInStatus,
            stubSearchPreferences,
            shelfId
        )
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
    fun `search result tap navigates without persisting or dismissing`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Open the search dialog so we can verify it stays open across the tap.
        stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnSearchClick)
        }

        // When
        val stateAfterClick = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnSearchResultBookClick(testBook))
        }

        // Then
        // The previewed book must NOT be persisted to the local DB on click —
        // that would leak it into the Library view. Persistence happens only
        // when the user explicitly adds the book to a shelf (OnAddBookClick).
        assertEquals("Should set navigateToBook", testBook, stateAfterClick?.navigateToBook)
        assertEquals(
            "Tap should not surface an error",
            null,
            stateAfterClick?.bookSearchState?.errorMessage
        )
        // The search dialog must remain visible — preview cache enables preserving
        // the result list across the search → detail → back round trip.
        assertTrue(
            "Tap must not dismiss the search dialog",
            stateAfterClick?.isSearchDialogVisible == true
        )
        stateHelper.cleanup()
    }

    @Test
    fun `OnNavigationHandled clears navigateToBook`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").build()
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Set navigateToBook via click
        stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnSearchResultBookClick(testBook))
        }

        // When
        val stateAfterHandled = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnNavigationHandled)
        }

        // Then
        assertEquals("navigateToBook should be null", null, stateAfterHandled?.navigateToBook)
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
        assertTrue(
            "Should contain operation context",
            stateAfterRemove?.errorMessage?.contains("Failed to remove book from shelf") == true
        )
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
        assertTrue(
            "Should contain operation context",
            stateAfterAdd?.errorMessage?.contains("Failed to add book to shelf") == true
        )
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
        assertTrue(
            "Should contain operation context",
            initialState?.errorMessage?.contains("Failed to load shelf details") == true
        )
        stateHelper.cleanup()
    }

    // Filter retrigger tests live in LibraryViewModelTest which uses StandardTestDispatcher,
    // supporting advanceTimeBy for debounce testing. The search flow logic is identical in both VMs.

    @Test
    fun `cannot uncheck title filter when author is already unchecked`() = runTest(testDispatcher) {
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        stubSearchPreferences.seed(SearchPreferenceState(searchByAuthor = true))
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Uncheck author first (both start checked, so this is allowed)
        stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByAuthor)
        }

        // Try to uncheck title — should be blocked
        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByTitle)
        }

        assertTrue(
            "Title should remain checked when author is unchecked",
            stateAfterToggle!!.bookSearchState.searchByTitle
        )
        stateHelper.cleanup()
    }

    @Test
    fun `cannot uncheck author filter when title is already unchecked`() = runTest(testDispatcher) {
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Uncheck title first
        stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByTitle)
        }

        // Try to uncheck author — should be blocked
        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByAuthor)
        }

        assertTrue(
            "Author should remain checked when title is unchecked",
            stateAfterToggle!!.bookSearchState.searchByAuthor
        )
        stateHelper.cleanup()
    }

    @Test
    fun `can toggle filter when both are checked`() = runTest(testDispatcher) {
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        stubSearchPreferences.seed(SearchPreferenceState(searchByAuthor = true))
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Both start checked — toggling either should work
        val stateAfterTitleToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByTitle)
        }

        assertFalse(
            "Title should be unchecked",
            stateAfterTitleToggle!!.bookSearchState.searchByTitle
        )
        assertTrue(
            "Author should remain checked",
            stateAfterTitleToggle.bookSearchState.searchByAuthor
        )
        stateHelper.cleanup()
    }

    @Test
    fun `existingBookIds updates when books list changes`() = runTest(testDispatcher) {
        mockGetShelfBooks.booksToReturn = listOf(
            TestBookBuilder().withId("book-1").build(),
            TestBookBuilder().withId("book-2").build()
        )
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        val state = stateHelper.getCurrentState()

        assertEquals(
            "existingBookIds should match books on shelf",
            setOf("book-1", "book-2"),
            state!!.bookSearchState.existingBookIds
        )
        stateHelper.cleanup()
    }

    // Subject toggle tests

    @Test
    fun `subject toggle changes state correctly`() = runTest(testDispatcher) {
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchBySubject)
        }

        assertTrue(
            "Subject should be checked after toggle",
            stateAfterToggle!!.bookSearchState.searchBySubject
        )
        stateHelper.cleanup()
    }

    @Test
    fun `library scope toggle flips state and persists preference`() = runTest(testDispatcher) {
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleLibraryScope)
        }

        assertTrue(
            "Library scope should be enabled after toggle",
            stateAfterToggle!!.bookSearchState.libraryScopeEnabled
        )

        val persistedState = stubSearchPreferences.lastUpdatedState
        assertTrue(
            "Persisted library scope should be enabled",
            persistedState!!.libraryScopeEnabled
        )
        stateHelper.cleanup()
    }

    @Test
    fun `safe search toggle persists preference`() = runTest(testDispatcher) {
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSafeSearch)
        }

        assertFalse(
            "Safe search should be disabled after toggle",
            stateAfterToggle!!.bookSearchState.safeSearchEnabled
        )

        // Verify preference was persisted
        val persistedState = stubSearchPreferences.lastUpdatedState
        assertFalse(
            "Persisted safe search should be disabled",
            persistedState!!.safeSearchEnabled
        )
        stateHelper.cleanup()
    }

    @Test
    fun `cannot uncheck title when author and subject both unchecked`() = runTest(testDispatcher) {
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        stubSearchPreferences.seed(SearchPreferenceState(searchByAuthor = true))
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Uncheck author (both title+author checked, subject unchecked by default)
        stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByAuthor)
        }

        // Try to uncheck title — should be blocked since subject is also unchecked
        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByTitle)
        }

        assertTrue(
            "Title should remain checked as the last checked filter",
            stateAfterToggle!!.bookSearchState.searchByTitle
        )
        stateHelper.cleanup()
    }

    @Test
    fun `can uncheck title when subject is checked`() = runTest(testDispatcher) {
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Enable subject first
        stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchBySubject)
        }

        // Uncheck author
        stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByAuthor)
        }

        // Now uncheck title — should work because subject is checked
        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnToggleSearchByTitle)
        }

        assertFalse(
            "Title should be unchecked when subject is still checked",
            stateAfterToggle!!.bookSearchState.searchByTitle
        )
        stateHelper.cleanup()
    }

    // Note: Search error/debounce tests require StandardTestDispatcher + advanceTimeBy.
    // The identical error behavior (results preserved on error) is tested in LibraryViewModelTest.

    // Simplified inline mock implementations for UI testing
    private class SimpleSearchBooksUseCase : SearchBooksUseCase {
        var searchResultsToReturn: List<Book> = emptyList()
        var shouldFail = false
        var lastTitleFilter: String? = null
        var lastAuthorFilter: String? = null
        var lastSubjectFilter: String? = null
        var invocationCount = 0

        override suspend operator fun invoke(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            subjectFilter: String?,
            safeSearchEnabled: Boolean
        ): Result<SearchResult, DataError.Remote> {
            lastTitleFilter = titleFilter
            lastAuthorFilter = authorFilter
            lastSubjectFilter = subjectFilter
            invocationCount++
            return if (shouldFail) {
                Result.Error(DataError.Remote.UNKNOWN)
            } else {
                Result.Success(SearchResult(books = searchResultsToReturn, filteredCount = 0))
            }
        }

        fun reset() {
            searchResultsToReturn = emptyList()
            shouldFail = false
            lastTitleFilter = null
            lastAuthorFilter = null
            lastSubjectFilter = null
            invocationCount = 0
        }
    }

    private class SimpleSearchLibraryBooksUseCase : SearchLibraryBooksUseCase {
        var booksToReturn: List<Book> = emptyList()
        var invocationCount = 0
        var lastQuery: String? = null
        var lastSearchByTitle: Boolean? = null
        var lastSearchByAuthor: Boolean? = null

        override suspend operator fun invoke(
            query: String,
            searchByTitle: Boolean,
            searchByAuthor: Boolean,
        ): Result<List<Book>, DataError.Local> {
            invocationCount++
            lastQuery = query
            lastSearchByTitle = searchByTitle
            lastSearchByAuthor = searchByAuthor
            return Result.Success(booksToReturn)
        }

        fun reset() {
            booksToReturn = emptyList()
            invocationCount = 0
            lastQuery = null
            lastSearchByTitle = null
            lastSearchByAuthor = null
        }
    }

    private class SimpleGetShelfBooksUseCase : GetShelfBooksUseCase {
        var booksToReturn: List<Book> = emptyList()

        override suspend operator fun invoke(shelfId: String): Flow<List<Book>> = flowOf(booksToReturn)

        fun reset() {
            booksToReturn = emptyList()
        }
    }

    private class SimpleAddBookToShelfUseCase : AddBookToShelfUseCase {
        var shouldSucceed = true

        override suspend operator fun invoke(book: Book, shelfId: String): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    private class SimpleRemoveBookFromShelfUseCase : RemoveBookFromShelfUseCase {
        var shouldSucceed = true

        override suspend operator fun invoke(bookId: String, shelfId: String): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    private class SimpleUpdateShelfTidyModeUseCase : UpdateShelfTidyModeUseCase {
        override suspend operator fun invoke(shelfId: String, isTidyMode: Boolean): Result<Unit, DataError> =
            Result.Success(Unit)
    }
}
