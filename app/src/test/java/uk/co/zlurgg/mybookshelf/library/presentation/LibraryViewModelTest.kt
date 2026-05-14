package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetAllLibraryBooksUseCase
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

    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(
            libraryUseCases = LibraryUseCases(getAllLibraryBooks = getAllLibraryBooks),
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

    private class GetAllLibraryBooksUseCaseStub(
        private val bookRepository: MockBookRepository
    ) : GetAllLibraryBooksUseCase {
        override fun invoke() = bookRepository.getAllPersonalBooks()
    }
}
