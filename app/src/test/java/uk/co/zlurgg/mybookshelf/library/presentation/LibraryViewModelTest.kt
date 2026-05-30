package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.book.domain.usecase.CacheSearchPreviewsUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchResult
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.preferences.SearchPreferenceState
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.library.domain.usecase.DeleteBooksFromLibraryUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetAllLibraryBooksUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetNonRemovableBookIdsUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.LibraryUseCases
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.StubSearchPreferences

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockBookRepository = MockBookRepository()
    private lateinit var dataStore: DataStore<Preferences>

    private val getAllLibraryBooks: GetAllLibraryBooksUseCase =
        GetAllLibraryBooksUseCaseStub(mockBookRepository)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataStore = PreferenceDataStoreFactory.create {
            java.io.File.createTempFile("test_prefs", ".preferences_pb")
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val stubSearchBooks = StubSearchBooksUseCase()
    private val stubCacheSearchPreviews = StubCacheSearchPreviewsUseCase()
    private val stubUpsertBook = StubUpsertBookUseCase()
    private val stubDeleteBooks = StubDeleteBooksUseCase()
    private val stubGetNonRemovableBookIds = StubGetNonRemovableBookIdsUseCase()
    private val stubSearchPreferences = StubSearchPreferences()

    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(
            libraryUseCases = LibraryUseCases(
                getAllLibraryBooks = getAllLibraryBooks,
                searchBooks = stubSearchBooks,
                cacheSearchPreviews = stubCacheSearchPreviews,
                upsertBook = stubUpsertBook,
                deleteBooks = stubDeleteBooks,
                getNonRemovableBookIds = stubGetNonRemovableBookIds
            ),
            dataStore = dataStore,
            searchPreferences = stubSearchPreferences,
        )
    }

    @Test
    fun `initial state shows loading then books`() = runTest(testDispatcher) {
        val books = listOf(
            TestBookBuilder().withId("1").withTitle("Book A").build(),
            TestBookBuilder().withId("2").withTitle("Book B").build(),
        )
        mockBookRepository.setPersonalBooks(books)

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        val state = stateHelper.getCurrentState()
        assertFalse(state!!.isLoading)
        assertEquals(2, state.allBooks.size)
        assertEquals(2, state.filteredBooks.size)
        stateHelper.cleanup()
    }

    @Test
    fun `search filters by title`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("1").withTitle("Kotlin in Action").build(),
                TestBookBuilder().withId("2").withTitle("Java Concurrency").build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState() // wait for init

        viewModel.onAction(LibraryAction.OnSearchQueryChange("Kotlin"))
        val state = stateHelper.getCurrentState()

        assertEquals(1, state!!.filteredBooks.size)
        assertEquals("Kotlin in Action", state.filteredBooks[0].title)
        stateHelper.cleanup()
    }

    @Test
    fun `search filters by author`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("1").withTitle("Book A").withAuthors(listOf("Smith")).build(),
                TestBookBuilder().withId("2").withTitle("Book B").withAuthors(listOf("Jones")).build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchQueryChange("Jones"))
        val state = stateHelper.getCurrentState()

        assertEquals(1, state!!.filteredBooks.size)
        assertEquals("Book B", state.filteredBooks[0].title)
        stateHelper.cleanup()
    }

    @Test
    fun `sort by title AZ`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("1").withTitle("Zebra").build(),
                TestBookBuilder().withId("2").withTitle("Apple").build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSortOptionSelected(LibrarySortOption.TITLE_AZ))
        val state = stateHelper.getCurrentState()

        assertEquals("Apple", state!!.filteredBooks[0].title)
        assertEquals("Zebra", state.filteredBooks[1].title)
        stateHelper.cleanup()
    }

    @Test
    fun `sort by recently added`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("1").withTitle("Old").withDateAdded(1000L).build(),
                TestBookBuilder().withId("2").withTitle("New").withDateAdded(2000L).build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSortOptionSelected(LibrarySortOption.RECENTLY_ADDED))
        val state = stateHelper.getCurrentState()

        assertEquals("New", state!!.filteredBooks[0].title)
        assertEquals("Old", state.filteredBooks[1].title)
        stateHelper.cleanup()
    }

    @Test
    fun `sort by author AZ`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("1").withTitle("Book 1").withAuthors(listOf("Zelda")).build(),
                TestBookBuilder().withId("2").withTitle("Book 2").withAuthors(listOf("Alice")).build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSortOptionSelected(LibrarySortOption.AUTHOR_AZ))
        val state = stateHelper.getCurrentState()

        assertEquals("Book 2", state!!.filteredBooks[0].title)
        assertEquals("Book 1", state.filteredBooks[1].title)
        stateHelper.cleanup()
    }

    @Test
    fun `reading status filter`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("1").withTitle("Want")
                    .withReadingStatus(ReadingStatus.NOT_READ).build(),
                TestBookBuilder().withId("2").withTitle("Reading")
                    .withReadingStatus(ReadingStatus.READING).build(),
                TestBookBuilder().withId("3").withTitle("Done")
                    .withReadingStatus(ReadingStatus.FINISHED).build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnReadingStatusSelected(ReadingStatus.FINISHED))
        val state = stateHelper.getCurrentState()

        assertEquals(1, state!!.filteredBooks.size)
        assertEquals("Done", state.filteredBooks[0].title)
        stateHelper.cleanup()
    }

    @Test
    fun `null reading status shows all books`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("1").withReadingStatus(ReadingStatus.NOT_READ).build(),
                TestBookBuilder().withId("2").withReadingStatus(ReadingStatus.FINISHED).build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnReadingStatusSelected(ReadingStatus.FINISHED))
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnReadingStatusSelected(null))
        val state = stateHelper.getCurrentState()

        assertEquals(2, state!!.filteredBooks.size)
        stateHelper.cleanup()
    }

    @Test
    fun `combined search and status filter`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("1").withTitle("Kotlin")
                    .withReadingStatus(ReadingStatus.FINISHED).build(),
                TestBookBuilder().withId("2").withTitle("Kotlin Guide")
                    .withReadingStatus(ReadingStatus.NOT_READ).build(),
                TestBookBuilder().withId("3").withTitle("Java")
                    .withReadingStatus(ReadingStatus.FINISHED).build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchQueryChange("Kotlin"))
        viewModel.onAction(LibraryAction.OnReadingStatusSelected(ReadingStatus.FINISHED))
        val state = stateHelper.getCurrentState()

        assertEquals(1, state!!.filteredBooks.size)
        assertEquals("Kotlin", state.filteredBooks[0].title)
        stateHelper.cleanup()
    }

    @Test
    fun `empty library shows no books`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(emptyList())

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        val state = stateHelper.getCurrentState()
        assertFalse(state!!.isLoading)
        assertTrue(state.allBooks.isEmpty())
        assertTrue(state.filteredBooks.isEmpty())
        stateHelper.cleanup()
    }

    // ========================================================================
    // Remote Search Dialog Tests
    // ========================================================================

    @Test
    fun `search dialog visibility toggles correctly`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        val shown = stateHelper.getCurrentState()
        assertTrue("Should show search dialog", shown!!.isSearchDialogVisible)

        viewModel.onAction(LibraryAction.OnDismissSearchDialog)
        val hidden = stateHelper.getCurrentState()
        assertFalse("Should hide search dialog", hidden!!.isSearchDialogVisible)
        stateHelper.cleanup()
    }

    @Test
    fun `dismiss resets bookSearchState to defaults`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Open dialog and type something
        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        stateHelper.getCurrentState()

        // Dismiss
        viewModel.onAction(LibraryAction.OnDismissSearchDialog)
        val state = stateHelper.getCurrentState()

        assertEquals("Query should be reset", "", state!!.bookSearchState.query)
        assertTrue("Results should be empty", state.bookSearchState.results.isEmpty())
        assertFalse("Should not be loading", state.bookSearchState.isLoading)
        assertFalse("Should not have searched", state.bookSearchState.hasSearched)
        assertTrue("errorMessage should be null", state.bookSearchState.errorMessage == null)
        stateHelper.cleanup()
    }

    @Test
    fun `remote search populates bookSearchState results after debounce`() = runTest(testDispatcher) {
        val searchResults = listOf(
            TestBookBuilder().withId("s1").withTitle("Search Result 1").build(),
            TestBookBuilder().withId("s2").withTitle("Search Result 2").build()
        )
        stubSearchBooks.searchResultsToReturn = searchResults

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("test"))

        // Advance past debounce (300ms)
        advanceTimeBy(350)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()

        assertEquals("Should have 2 search results", 2, state!!.bookSearchState.results.size)
        assertTrue("Should have searched", state.bookSearchState.hasSearched)
        assertFalse("Should not be loading", state.bookSearchState.isLoading)
        stateHelper.cleanup()
    }

    @Test
    fun `remote search does not trigger for query under 2 chars`() = runTest(testDispatcher) {
        stubSearchBooks.searchResultsToReturn = listOf(
            TestBookBuilder().withId("s1").withTitle("Result").build()
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("a"))

        advanceTimeBy(350)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()

        assertTrue("Results should be empty for short query", state!!.bookSearchState.results.isEmpty())
        assertFalse("Should not have searched", state.bookSearchState.hasSearched)
        stateHelper.cleanup()
    }

    @Test
    fun `search error sets bookSearchState errorMessage`() = runTest(testDispatcher) {
        stubSearchBooks.shouldFail = true

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))

        advanceTimeBy(350)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()

        assertTrue("Should have error message", state!!.bookSearchState.errorMessage != null)
        assertTrue("Should have searched", state.bookSearchState.hasSearched)
        assertFalse("Should not be loading", state.bookSearchState.isLoading)
        stateHelper.cleanup()
    }

    @Test
    fun `add book to library calls upsertBook`() = runTest(testDispatcher) {
        val book = TestBookBuilder().withId("new-book").withTitle("New Book").build()

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnAddBookToLibrary(book))
        advanceUntilIdle()

        assertTrue("Should have called upsert", stubUpsertBook.lastUpsertedBook?.id == "new-book")
        stateHelper.cleanup()
    }

    @Test
    fun `add book error sets bookSearchState errorMessage`() = runTest(testDispatcher) {
        stubUpsertBook.shouldSucceed = false
        val book = TestBookBuilder().withId("new-book").build()

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnAddBookToLibrary(book))
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()

        assertTrue("Should have error message", state!!.bookSearchState.errorMessage != null)
        assertTrue(
            "Should contain operation context",
            state.bookSearchState.errorMessage!!.contains("add book to library")
        )
        stateHelper.cleanup()
    }

    @Test
    fun `search result tap navigates without persisting or dismissing`() = runTest(testDispatcher) {
        val book = TestBookBuilder().withId("clicked-book").withTitle("Clicked Book").build()

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Open the search dialog so we can verify it stays open across the tap.
        viewModel.onAction(LibraryAction.OnSearchClick)
        advanceUntilIdle()

        viewModel.onAction(LibraryAction.OnSearchResultBookClick(book))
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()

        // The previewed book must NOT be written to the local DB on click —
        // that would leak it into the Library view. Persistence happens only
        // when the user explicitly adds the book (OnAddBookToLibrary).
        assertNull(
            "Tap must not upsert into the local DB",
            stubUpsertBook.lastUpsertedBook
        )
        assertEquals("Should set navigateToBook", "clicked-book", state!!.navigateToBook?.id)
        assertNull(
            "Tap should not surface an error",
            state.bookSearchState.errorMessage
        )
        // The search dialog must remain visible — preview cache enables preserving
        // the result list across the search → detail → back round trip.
        assertTrue(
            "Tap must not dismiss the search dialog",
            state.isSearchDialogVisible
        )
        stateHelper.cleanup()
    }

    @Test
    fun `OnNavigationHandled clears navigateToBook`() = runTest(testDispatcher) {
        val book = TestBookBuilder().withId("nav-book").build()

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchResultBookClick(book))
        advanceUntilIdle()

        viewModel.onAction(LibraryAction.OnNavigationHandled)
        val state = stateHelper.getCurrentState()

        assertNull("navigateToBook should be cleared", state!!.navigateToBook)
        stateHelper.cleanup()
    }

    // ========================================================================
    // Selection Mode Tests
    // ========================================================================

    @Test
    fun `toggle selection mode sets isSelectionMode and clears selectedBookIds`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val stateHelper = viewModel.state.testHelper(this)
            stateHelper.getCurrentState()

            viewModel.onAction(LibraryAction.OnToggleSelectionMode)
            val state = stateHelper.getCurrentState()

            assertTrue("Should be in selection mode", state!!.isSelectionMode)
            assertTrue("Selected IDs should be empty", state.selectedBookIds.isEmpty())
            stateHelper.cleanup()
        }

    @Test
    fun `exiting selection mode clears selected IDs preserves filter state`() =
        runTest(testDispatcher) {
            mockBookRepository.setPersonalBooks(
                listOf(TestBookBuilder().withId("1").build())
            )

            val viewModel = createViewModel()
            val stateHelper = viewModel.state.testHelper(this)
            stateHelper.getCurrentState()

            viewModel.onAction(
                LibraryAction.OnSortOptionSelected(LibrarySortOption.TITLE_AZ)
            )
            viewModel.onAction(LibraryAction.OnToggleSelectionMode)
            viewModel.onAction(LibraryAction.OnToggleBookSelection("1"))
            stateHelper.getCurrentState()

            viewModel.onAction(LibraryAction.OnToggleSelectionMode)
            val state = stateHelper.getCurrentState()

            assertFalse("Should exit selection mode", state!!.isSelectionMode)
            assertTrue("Selected IDs should be cleared", state.selectedBookIds.isEmpty())
            assertEquals(
                "Sort option should be preserved",
                LibrarySortOption.TITLE_AZ,
                state.sortOption
            )
            stateHelper.cleanup()
        }

    @Test
    fun `deletableBooks excludes non-removable books`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("ok-1").build(),
                TestBookBuilder().withId("club-1").build(),
                TestBookBuilder().withId("ok-2").build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        stubGetNonRemovableBookIds.nonRemovableIds.value = setOf("club-1")
        val state = stateHelper.getCurrentState()

        val deletableIds = state!!.deletableBooks.map { it.id }
        assertEquals(2, deletableIds.size)
        assertTrue("Should contain ok-1", deletableIds.contains("ok-1"))
        assertTrue("Should contain ok-2", deletableIds.contains("ok-2"))
        assertFalse("Should not contain club-1", deletableIds.contains("club-1"))
        stateHelper.cleanup()
    }

    @Test
    fun `selectedBookIds pruned when nonRemovableBookIds updates`() =
        runTest(testDispatcher) {
            mockBookRepository.setPersonalBooks(
                listOf(
                    TestBookBuilder().withId("book-1").build(),
                    TestBookBuilder().withId("book-2").build(),
                )
            )

            val viewModel = createViewModel()
            val stateHelper = viewModel.state.testHelper(this)
            stateHelper.getCurrentState()

            viewModel.onAction(LibraryAction.OnToggleSelectionMode)
            viewModel.onAction(LibraryAction.OnToggleBookSelection("book-1"))
            viewModel.onAction(LibraryAction.OnToggleBookSelection("book-2"))
            stateHelper.getCurrentState()

            stubGetNonRemovableBookIds.nonRemovableIds.value = setOf("book-2")
            val state = stateHelper.getCurrentState()

            assertEquals(
                "Only book-1 should remain selected",
                setOf("book-1"),
                state!!.selectedBookIds
            )
            stateHelper.cleanup()
        }

    @Test
    fun `select all uses deletableBooks IDs not allBooks`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("ok-1").build(),
                TestBookBuilder().withId("club-1").build(),
            )
        )
        stubGetNonRemovableBookIds.nonRemovableIds.value = setOf("club-1")

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnToggleSelectionMode)
        viewModel.onAction(LibraryAction.OnSelectAll)
        val state = stateHelper.getCurrentState()

        assertEquals("Should only select deletable book", setOf("ok-1"), state!!.selectedBookIds)
        stateHelper.cleanup()
    }

    @Test
    fun `toggle book selection adds and removes`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(TestBookBuilder().withId("book-1").build())
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnToggleSelectionMode)
        viewModel.onAction(LibraryAction.OnToggleBookSelection("book-1"))
        var state = stateHelper.getCurrentState()
        assertTrue("Should be selected", state!!.selectedBookIds.contains("book-1"))

        viewModel.onAction(LibraryAction.OnToggleBookSelection("book-1"))
        state = stateHelper.getCurrentState()
        assertFalse("Should be deselected", state!!.selectedBookIds.contains("book-1"))
        stateHelper.cleanup()
    }

    @Test
    fun `deselect all removes only visible selections`() = runTest(testDispatcher) {
        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("book-1").withTitle("Kotlin").build(),
                TestBookBuilder().withId("book-2").withTitle("Java").build(),
            )
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Select both books
        viewModel.onAction(LibraryAction.OnToggleSelectionMode)
        viewModel.onAction(LibraryAction.OnToggleBookSelection("book-1"))
        viewModel.onAction(LibraryAction.OnToggleBookSelection("book-2"))
        stateHelper.getCurrentState()

        // Filter to only show "Kotlin" — book-2 is now hidden
        viewModel.onAction(LibraryAction.OnSearchQueryChange("Kotlin"))
        advanceTimeBy(350)
        stateHelper.getCurrentState()

        // Deselect all — should only deselect visible (book-1)
        viewModel.onAction(LibraryAction.OnDeselectAll)
        val state = stateHelper.getCurrentState()

        assertEquals(
            "Hidden selection should be preserved",
            setOf("book-2"),
            state!!.selectedBookIds
        )
        stateHelper.cleanup()
    }

    @Test
    fun `confirm delete calls use case and exits selection mode`() =
        runTest(testDispatcher) {
            mockBookRepository.setPersonalBooks(
                listOf(
                    TestBookBuilder().withId("book-1").build(),
                    TestBookBuilder().withId("book-2").build(),
                )
            )

            val viewModel = createViewModel()
            val stateHelper = viewModel.state.testHelper(this)
            stateHelper.getCurrentState()

            viewModel.onAction(LibraryAction.OnToggleSelectionMode)
            viewModel.onAction(LibraryAction.OnToggleBookSelection("book-1"))
            viewModel.onAction(LibraryAction.OnToggleBookSelection("book-2"))
            viewModel.onAction(LibraryAction.OnDeleteSelectedClick)
            viewModel.onAction(LibraryAction.OnConfirmDelete)
            val state = stateHelper.getCurrentState()

            assertEquals(
                "Should have deleted both books",
                listOf("book-1", "book-2"),
                stubDeleteBooks.lastDeletedBookIds.sorted()
            )
            assertFalse("Should exit selection mode", state!!.isSelectionMode)
            assertTrue("Selected IDs should be cleared", state.selectedBookIds.isEmpty())
            assertFalse("Dialog should be dismissed", state.showDeleteConfirmation)
            stateHelper.cleanup()
        }

    @Test
    fun `delete error sets errorMessage via ErrorFormatter`() = runTest(testDispatcher) {
        stubDeleteBooks.shouldSucceed = false
        mockBookRepository.setPersonalBooks(
            listOf(TestBookBuilder().withId("book-1").build())
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnToggleSelectionMode)
        viewModel.onAction(LibraryAction.OnToggleBookSelection("book-1"))
        viewModel.onAction(LibraryAction.OnConfirmDelete)
        val state = stateHelper.getCurrentState()

        assertTrue("Should have error message", state!!.errorMessage != null)
        assertTrue(
            "Should contain operation context",
            state.errorMessage!!.contains("delete books")
        )
        assertFalse("Dialog should be dismissed", state.showDeleteConfirmation)
        stateHelper.cleanup()
    }

    @Test
    fun `OnDismissError clears errorMessage`() = runTest(testDispatcher) {
        stubDeleteBooks.shouldSucceed = false
        mockBookRepository.setPersonalBooks(
            listOf(TestBookBuilder().withId("book-1").build())
        )

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnToggleSelectionMode)
        viewModel.onAction(LibraryAction.OnToggleBookSelection("book-1"))
        viewModel.onAction(LibraryAction.OnConfirmDelete)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnDismissError)
        val state = stateHelper.getCurrentState()

        assertNull("errorMessage should be cleared", state!!.errorMessage)
        stateHelper.cleanup()
    }

    @Test
    fun `existingBookIds updates when allBooks changes`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        mockBookRepository.setPersonalBooks(
            listOf(
                TestBookBuilder().withId("book-1").build(),
                TestBookBuilder().withId("book-2").build(),
            )
        )
        val state = stateHelper.getCurrentState()

        assertEquals(
            "existingBookIds should match allBooks",
            setOf("book-1", "book-2"),
            state!!.bookSearchState.existingBookIds
        )
        stateHelper.cleanup()
    }

    @Test
    fun `toggle title filter retriggers remote search with updated results`() = runTest(testDispatcher) {
        val initialResults = listOf(TestBookBuilder().withId("r1").withTitle("Result 1").build())
        stubSearchBooks.searchResultsToReturn = initialResults
        stubSearchPreferences.seed(SearchPreferenceState(searchByAuthor = true))

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Perform initial search
        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()
        val countAfterInitial = stubSearchBooks.invocationCount

        // Toggle title OFF → only author remains → search uses authorFilter
        viewModel.onAction(LibraryAction.OnToggleSearchByTitle)
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()

        assertTrue(
            "Search should be invoked again after title toggle",
            stubSearchBooks.invocationCount > countAfterInitial
        )
        assertEquals("kotlin", stubSearchBooks.lastAuthorFilter)
        stateHelper.cleanup()
    }

    @Test
    fun `toggle author filter retriggers remote search with updated results`() = runTest(testDispatcher) {
        val initialResults = listOf(TestBookBuilder().withId("r1").withTitle("Result 1").build())
        stubSearchBooks.searchResultsToReturn = initialResults
        stubSearchPreferences.seed(SearchPreferenceState(searchByAuthor = true))

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Perform initial search
        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()
        val countAfterInitial = stubSearchBooks.invocationCount

        // Toggle author OFF → only title remains → search uses titleFilter
        viewModel.onAction(LibraryAction.OnToggleSearchByAuthor)
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()

        assertTrue(
            "Search should be invoked again after author toggle",
            stubSearchBooks.invocationCount > countAfterInitial
        )
        assertEquals("kotlin", stubSearchBooks.lastTitleFilter)
        stateHelper.cleanup()
    }

    @Test
    fun `cannot uncheck title filter when author is already unchecked`() = runTest(testDispatcher) {
        stubSearchPreferences.seed(SearchPreferenceState(searchByAuthor = true))
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Uncheck author first (both start checked)
        viewModel.onAction(LibraryAction.OnToggleSearchByAuthor)
        stateHelper.getCurrentState()

        // Try to uncheck title — should be blocked
        viewModel.onAction(LibraryAction.OnToggleSearchByTitle)
        val state = stateHelper.getCurrentState()

        assertTrue(
            "Title should remain checked when author is unchecked",
            state!!.bookSearchState.searchByTitle
        )
        stateHelper.cleanup()
    }

    @Test
    fun `cannot uncheck author filter when title is already unchecked`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Uncheck title first
        viewModel.onAction(LibraryAction.OnToggleSearchByTitle)
        stateHelper.getCurrentState()

        // Try to uncheck author — should be blocked
        viewModel.onAction(LibraryAction.OnToggleSearchByAuthor)
        val state = stateHelper.getCurrentState()

        assertTrue(
            "Author should remain checked when title is unchecked",
            state!!.bookSearchState.searchByAuthor
        )
        stateHelper.cleanup()
    }

    @Test
    fun `can toggle filter when both are checked`() = runTest(testDispatcher) {
        stubSearchPreferences.seed(SearchPreferenceState(searchByAuthor = true))
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Both start checked — toggling either should work
        viewModel.onAction(LibraryAction.OnToggleSearchByTitle)
        val state = stateHelper.getCurrentState()

        assertFalse("Title should be unchecked", state!!.bookSearchState.searchByTitle)
        assertTrue("Author should remain checked", state.bookSearchState.searchByAuthor)
        stateHelper.cleanup()
    }

    // ========================================================================
    // C1 — Pagination (load more)
    // ========================================================================

    @Test
    fun `OnLoadMore advances nextStartIndex by rawPageSize, not books size`() = runTest(testDispatcher) {
        // The provider-asymmetric bug: Google's startIndex points into the
        // UNFILTERED stream. Advancing by books.size on a page where the
        // language filter dropped 20 of 40 would re-fetch rows 20..59.
        val page1 = (1..18).map { TestBookBuilder().withId("p1-$it").build() }
        stubSearchBooks.searchResultsToReturn = page1
        stubSearchBooks.defaultRawPageSize = 40 // pre-filter count
        stubSearchBooks.defaultPageSize = 40

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("test"))
        advanceTimeBy(350)
        advanceUntilIdle()
        val afterPage1 = stateHelper.getCurrentState()!!
        assertEquals(
            "nextStartIndex should advance by rawPageSize (40), not books.size (18)",
            40,
            afterPage1.bookSearchState.nextStartIndex,
        )
        assertTrue("canLoadMore should be true when rawPageSize == pageSize", afterPage1.bookSearchState.canLoadMore)

        viewModel.onAction(LibraryAction.OnLoadMore)
        advanceUntilIdle()

        assertEquals("Page 2 must request startIndex = 40", 40, stubSearchBooks.lastStartIndex)
        stateHelper.cleanup()
    }

    @Test
    fun `OnLoadMore dedupes accumulated results by id when provider returns overlap`() = runTest(testDispatcher) {
        val page1 = listOf(
            TestBookBuilder().withId("a").build(),
            TestBookBuilder().withId("b").build(),
        )
        val page2 = listOf(
            // Google occasionally returns the same volume across adjacent pages.
            TestBookBuilder().withId("b").build(),
            TestBookBuilder().withId("c").build(),
        )
        stubSearchBooks.searchResultsToReturn = page1
        stubSearchBooks.page2ResultsToReturn = page2
        stubSearchBooks.page2RawPageSize = 2
        stubSearchBooks.defaultRawPageSize = 2
        stubSearchBooks.defaultPageSize = 2

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnLoadMore)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()!!

        assertEquals(
            "Duplicate id 'b' must be dropped — final ids: a, b, c",
            listOf("a", "b", "c"),
            state.bookSearchState.results.map { it.id },
        )
        stateHelper.cleanup()
    }

    @Test
    fun `OnLoadMore is silent when query is below MIN length (defensive guard)`() = runTest(testDispatcher) {
        // Edge case: user deleted query below MIN_SEARCH_QUERY_LENGTH but taps
        // load-more before the 300ms debounce fires the below-min reset.
        // canLoadMore is still true from the prior page; firing would burn a
        // request the imminent reset would invalidate.
        stubSearchBooks.searchResultsToReturn = listOf(TestBookBuilder().withId("a").build())
        stubSearchBooks.defaultRawPageSize = 1
        stubSearchBooks.defaultPageSize = 1

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Establish state with canLoadMore (set up via a normal search at MIN length first).
        stubSearchBooks.defaultRawPageSize = 40
        stubSearchBooks.defaultPageSize = 40
        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()
        val invocationCountBefore = stubSearchBooks.invocationCount

        // User deletes query down to 1 char then taps load-more before debounce fires.
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("k"))
        viewModel.onAction(LibraryAction.OnLoadMore)
        advanceUntilIdle()

        assertEquals(
            "OnLoadMore must not fire a request when query is below MIN length",
            invocationCountBefore,
            stubSearchBooks.invocationCount,
        )
        stateHelper.cleanup()
    }

    @Test
    fun `load-more error preserves accumulated results and surfaces errorMessage`() = runTest(testDispatcher) {
        val page1 = listOf(
            TestBookBuilder().withId("a").build(),
            TestBookBuilder().withId("b").build(),
        )
        stubSearchBooks.searchResultsToReturn = page1
        stubSearchBooks.defaultRawPageSize = 40
        stubSearchBooks.defaultPageSize = 40
        stubSearchBooks.failOnAppend = true

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnLoadMore)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()!!

        assertEquals(
            "Page-1 results must be preserved across load-more error",
            listOf("a", "b"),
            state.bookSearchState.results.map { it.id },
        )
        assertTrue("Error message must be surfaced", state.bookSearchState.errorMessage != null)
        assertFalse("isLoadingMore must be cleared", state.bookSearchState.isLoadingMore)
        stateHelper.cleanup()
    }

    @Test
    fun `canLoadMore false when rawPageSize less than pageSize (partial last page)`() = runTest(testDispatcher) {
        // End-of-results signal: when the provider returns fewer raw items than
        // we asked for, there are no more pages.
        stubSearchBooks.searchResultsToReturn = listOf(TestBookBuilder().withId("a").build())
        stubSearchBooks.defaultRawPageSize = 7 // partial page
        stubSearchBooks.defaultPageSize = 40

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()!!

        assertFalse(
            "canLoadMore must be false when rawPageSize < pageSize",
            state.bookSearchState.canLoadMore,
        )
        stateHelper.cleanup()
    }

    @Test
    fun `canLoadMore stays true after a filter-killed page (rawPageSize equals pageSize)`() = runTest(testDispatcher) {
        // Filter-killed page test: the safe-search filter could drop every book
        // in the page, but the PROVIDER still has more. rawPageSize == pageSize
        // means "full page from the provider" regardless of how many books
        // survived the post-filter. Predicate must NOT use books.isEmpty.
        stubSearchBooks.searchResultsToReturn = emptyList() // all filtered out
        stubSearchBooks.defaultRawPageSize = 40 // provider returned full page
        stubSearchBooks.defaultPageSize = 40

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()!!

        assertTrue(
            "Filter-killed page must NOT prematurely terminate pagination",
            state.bookSearchState.canLoadMore,
        )
        stateHelper.cleanup()
    }

    @Test
    fun `canLoadMore true when page 1 fell back to OL and returned a full 100 rows`() = runTest(testDispatcher) {
        // Cross-provider page-size propagation: the data source reports the page
        // size IT asked for (Google 40, OL 100). The VM must compare against the
        // returned pageSize, not a hard-coded constant.
        val page1 = (1..100).map { TestBookBuilder().withId("ol-$it").build() }
        stubSearchBooks.searchResultsToReturn = page1
        stubSearchBooks.defaultRawPageSize = 100
        stubSearchBooks.defaultPageSize = 100 // OL fallback page

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()!!

        // 100 == 100 → canLoadMore. A hard-coded Google-40 constant would
        // misclassify this 100-row OL response.
        assertTrue(
            "canLoadMore must compare against the per-response pageSize",
            state.bookSearchState.canLoadMore,
        )
        stateHelper.cleanup()
    }

    @Test
    fun `cacheSearchPreviews called with accumulated list on load more`() = runTest(testDispatcher) {
        // Repo's cacheSearchPreviews clears-then-writes — passing only the
        // page-2 batch would invalidate page-1 entries for tap-into-detail.
        val page1 = listOf(
            TestBookBuilder().withId("a").build(),
            TestBookBuilder().withId("b").build(),
        )
        val page2 = listOf(
            TestBookBuilder().withId("c").build(),
            TestBookBuilder().withId("d").build(),
        )
        stubSearchBooks.searchResultsToReturn = page1
        stubSearchBooks.page2ResultsToReturn = page2
        stubSearchBooks.page2RawPageSize = 2
        stubSearchBooks.defaultRawPageSize = 2
        stubSearchBooks.defaultPageSize = 2

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnLoadMore)
        advanceUntilIdle()

        assertEquals(
            "After load-more, cache must hold the accumulated list (page1 + page2)",
            listOf("a", "b", "c", "d"),
            stubCacheSearchPreviews.lastCachedBooks.map { it.id },
        )
        stateHelper.cleanup()
    }

    @Test
    fun `new query during in-flight load-more cancels via merged collectLatest`() = runTest(testDispatcher) {
        val page1 = listOf(TestBookBuilder().withId("a").build())
        stubSearchBooks.searchResultsToReturn = page1
        stubSearchBooks.defaultRawPageSize = 40
        stubSearchBooks.defaultPageSize = 40

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        // Get page 1 results.
        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()

        // Make load-more suspend indefinitely; new query must cancel it.
        stubSearchBooks.suspendUntilCancelled = true
        viewModel.onAction(LibraryAction.OnLoadMore)
        advanceUntilIdle() // load-more is now suspended

        // Re-arm so the fresh-search succeeds.
        stubSearchBooks.suspendUntilCancelled = false
        val freshResults = listOf(TestBookBuilder().withId("fresh-1").build())
        stubSearchBooks.searchResultsToReturn = freshResults

        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("rust"))
        advanceTimeBy(350)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()!!

        assertEquals(
            "Fresh search must REPLACE results — load-more was cancelled mid-flight",
            listOf("fresh-1"),
            state.bookSearchState.results.map { it.id },
        )
        assertFalse("isLoadingMore must be cleared", state.bookSearchState.isLoadingMore)
        stateHelper.cleanup()
    }

    @Test
    fun `filter toggle during in-flight load-more cancels via merged collectLatest`() = runTest(testDispatcher) {
        val page1 = listOf(TestBookBuilder().withId("a").build())
        stubSearchBooks.searchResultsToReturn = page1
        stubSearchBooks.defaultRawPageSize = 40
        stubSearchBooks.defaultPageSize = 40

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchClick)
        viewModel.onAction(LibraryAction.OnRemoteSearchQueryChange("kotlin"))
        advanceTimeBy(350)
        advanceUntilIdle()
        stateHelper.getCurrentState()

        stubSearchBooks.suspendUntilCancelled = true
        viewModel.onAction(LibraryAction.OnLoadMore)
        advanceUntilIdle()

        // Toggle subject filter — must cancel the suspended load-more and re-run page 1.
        stubSearchBooks.suspendUntilCancelled = false
        stubSearchBooks.searchResultsToReturn = listOf(TestBookBuilder().withId("fresh-1").build())

        viewModel.onAction(LibraryAction.OnToggleSearchBySubject)
        advanceTimeBy(350)
        advanceUntilIdle()
        val state = stateHelper.getCurrentState()!!

        assertEquals(
            "Filter retrigger must cancel the suspended load-more and re-run page 1",
            listOf("fresh-1"),
            state.bookSearchState.results.map { it.id },
        )
        assertFalse(state.bookSearchState.isLoadingMore)
        assertEquals(
            "nextStartIndex resets on fresh search",
            40, // rawPageSize from the fresh page-1 response
            state.bookSearchState.nextStartIndex,
        )
        stateHelper.cleanup()
    }

    private class GetAllLibraryBooksUseCaseStub(
        private val bookRepository: MockBookRepository
    ) : GetAllLibraryBooksUseCase {
        override fun invoke() = bookRepository.getAllPersonalBooks()
    }

    private class StubSearchBooksUseCase : SearchBooksUseCase {
        var searchResultsToReturn: List<Book> = emptyList()

        // For pagination: returned when startIndex != null.
        var page2ResultsToReturn: List<Book>? = null
        var page2RawPageSize: Int = 0
        var defaultRawPageSize: Int = 0
        var defaultPageSize: Int = 1000
        var shouldFail = false
        var failOnAppend = false
        var suspendUntilCancelled = false
        var lastTitleFilter: String? = null
        var lastAuthorFilter: String? = null
        var lastSubjectFilter: String? = null
        var lastStartIndex: Int? = null
        var invocationCount = 0

        override suspend fun invoke(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            subjectFilter: String?,
            safeSearchEnabled: Boolean,
            startIndex: Int?,
        ): Result<SearchResult, DataError.Remote> {
            lastTitleFilter = titleFilter
            lastAuthorFilter = authorFilter
            lastSubjectFilter = subjectFilter
            lastStartIndex = startIndex
            invocationCount++

            if (suspendUntilCancelled) {
                kotlinx.coroutines.awaitCancellation()
            }
            if (failOnAppend && startIndex != null) {
                return Result.Error(DataError.Remote.UNKNOWN)
            }
            return if (shouldFail) {
                Result.Error(DataError.Remote.UNKNOWN)
            } else {
                val books = if (startIndex != null && page2ResultsToReturn != null) {
                    page2ResultsToReturn!!
                } else {
                    searchResultsToReturn
                }
                val rawSize = if (startIndex != null && page2ResultsToReturn != null) {
                    page2RawPageSize
                } else {
                    defaultRawPageSize.takeIf { it > 0 } ?: books.size
                }
                Result.Success(
                    SearchResult(
                        books = books,
                        filteredCount = 0,
                        rawPageSize = rawSize,
                        pageSize = defaultPageSize,
                    )
                )
            }
        }
    }

    private class StubCacheSearchPreviewsUseCase : CacheSearchPreviewsUseCase {
        var invocationCount = 0
        var lastCachedBooks: List<Book> = emptyList()

        override fun invoke(books: List<Book>) {
            invocationCount++
            lastCachedBooks = books
        }
    }

    private class StubUpsertBookUseCase : UpsertBookUseCase {
        var shouldSucceed = true
        var lastUpsertedBook: Book? = null

        override suspend fun invoke(book: Book): Result<Unit, DataError.Local> {
            lastUpsertedBook = book
            return if (shouldSucceed) {
                Result.Success(Unit)
            } else {
                Result.Error(DataError.Local.UNKNOWN)
            }
        }
    }

    private class StubDeleteBooksUseCase : DeleteBooksFromLibraryUseCase {
        var shouldSucceed = true
        var lastDeletedBookIds: List<String> = emptyList()

        override suspend fun invoke(bookIds: List<String>): Result<Unit, DataError.Local> {
            lastDeletedBookIds = bookIds
            return if (shouldSucceed) {
                Result.Success(Unit)
            } else {
                Result.Error(DataError.Local.UNKNOWN)
            }
        }
    }

    private class StubGetNonRemovableBookIdsUseCase : GetNonRemovableBookIdsUseCase {
        val nonRemovableIds = MutableStateFlow<Set<String>>(emptySet())

        override fun invoke(): Flow<Set<String>> = nonRemovableIds
    }
}
