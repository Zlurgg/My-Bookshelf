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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.library.domain.usecase.DeleteBooksFromLibraryUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetAllLibraryBooksUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetNonRemovableBookIdsUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.LibraryUseCases
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

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
    private val stubUpsertBook = StubUpsertBookUseCase()
    private val stubDeleteBooks = StubDeleteBooksUseCase()
    private val stubGetNonRemovableBookIds = StubGetNonRemovableBookIdsUseCase()

    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(
            libraryUseCases = LibraryUseCases(
                getAllLibraryBooks = getAllLibraryBooks,
                searchBooks = stubSearchBooks,
                upsertBook = stubUpsertBook,
                deleteBooks = stubDeleteBooks,
                getNonRemovableBookIds = stubGetNonRemovableBookIds
            ),
            dataStore = dataStore,
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
    fun `search result book click upserts book for caching`() = runTest(testDispatcher) {
        val book = TestBookBuilder().withId("clicked-book").withTitle("Clicked Book").build()

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        stateHelper.getCurrentState()

        viewModel.onAction(LibraryAction.OnSearchResultBookClick(book))
        advanceUntilIdle()

        assertTrue(
            "Should have cached clicked book",
            stubUpsertBook.lastUpsertedBook?.id == "clicked-book"
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
        var shouldFail = false

        override suspend fun invoke(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?
        ): Result<List<Book>, DataError.Remote> {
            return if (shouldFail) {
                Result.Error(DataError.Remote.UNKNOWN)
            } else {
                Result.Success(searchResultsToReturn)
            }
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
