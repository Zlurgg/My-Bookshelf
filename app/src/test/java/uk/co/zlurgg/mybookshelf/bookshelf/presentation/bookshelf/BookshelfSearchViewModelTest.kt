package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.test.TestIdGenerator

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookshelfSearchViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    companion object {
        private const val TEST_SHELF_ID = "shelf1"
        private const val DEBOUNCE_DELAY_MS = 450L
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBookRepository : BookRepository {
        val searchQueries = mutableListOf<String>()
        var searchResult: List<Book> = emptyList()
        var shouldReturnError = false

        override suspend fun getBookById(bookId: String): Book? = null

        override suspend fun upsertBook(book: Book) {}

        override suspend fun deleteBook(bookId: String) {}

        override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
            return Result.Success(null)
        }

        override suspend fun searchBooks(
            query: String,
            sortBy: uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort,
            language: String?,
            authorFilter: String?,
            titleFilter: String?
        ): Result<List<Book>, DataError.Remote> {
            searchQueries.add(query)
            return if (shouldReturnError) {
                Result.Error(DataError.Remote.REQUEST_TIMEOUT)
            } else {
                Result.Success(searchResult)
            }
        }
    }

    private class FakeBookshelfRepository : BookshelfRepository {
        override suspend fun addBookToShelf(shelfId: String, bookId: String) {}

        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {}

        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> =
            MutableStateFlow(emptyList())

        override fun isBookInAnyShelf(bookId: String): Flow<Boolean> {
            return MutableStateFlow(false)
        }

        override fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean> {
            return MutableStateFlow(false)
        }

        override fun getShelvesForBook(bookId: String): Flow<List<String>> {
            return MutableStateFlow(emptyList())
        }
    }

    private fun sampleBook(id: String = TestIdGenerator.generateBookId()) = Book(
        id = id,
        title = "Sample Book",
        authors = listOf("Author"),
        imageUrl = "http://example.com/cover.jpg",
        description = null,
        languages = listOf("eng"),
        firstPublishYear = "2000",
        averageRating = 4.0,
        ratingCount = 5,
        numPages = 200,
        numEditions = 1,
        purchased = false,
        spineColor = 0xFF0000FF.toInt()
    )

    @Test
    fun onSearchClick_opens_dialog() = runTest {
        val bookRepository = FakeBookRepository()
        val bookshelfRepository = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = TEST_SHELF_ID
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        vm.onAction(BookshelfAction.OnSearchClick)
        advanceUntilIdle()
        
        assertTrue(latestState?.isSearchDialogVisible == true)
        job.cancel()
    }

    @Test
    fun onDismissSearchDialog_resets_state() = runTest {
        val bookRepository = FakeBookRepository()
        val bookshelfRepository = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = TEST_SHELF_ID
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        // Set up search state
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("test"))
        
        // Dismiss dialog
        vm.onAction(BookshelfAction.OnDismissSearchDialog)
        advanceUntilIdle()
        
        // State should be reset
        assertEquals("", latestState?.searchQuery)
        assertEquals(false, latestState?.isSearchDialogVisible)
        assertTrue(latestState?.searchResults?.isEmpty() == true)
        
        job.cancel()
    }

    @Test
    fun onSearchQueryChange_updates_ui_immediately() = runTest {
        val bookRepository = FakeBookRepository()
        val bookshelfRepository = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = TEST_SHELF_ID
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        // Open search dialog and enter query
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("test query"))
        advanceUntilIdle()
        
        // State should update immediately (before debounce)
        assertEquals("test query", latestState?.searchQuery)
        assertTrue(latestState?.isSearchDialogVisible == true)
        
        job.cancel()
    }

    @Test
    fun search_triggers_after_debounce() = runTest {
        val bookRepository = FakeBookRepository().apply {
            searchResult = listOf(sampleBook("result1"))
        }
        val bookshelfRepository = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository, 
            shelfId = TEST_SHELF_ID
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        // Open search dialog and enter query
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("test"))
        
        // Wait for debounce period
        testDispatcher.scheduler.advanceTimeBy(DEBOUNCE_DELAY_MS)
        testDispatcher.scheduler.runCurrent()
        
        // Should have called search
        assertEquals(listOf("test"), bookRepository.searchQueries)
        assertEquals(1, latestState?.searchResults?.size)
        assertEquals("result1", latestState?.searchResults?.first()?.id)
        
        job.cancel()
    }

    @Test
    fun short_queries_ignored() = runTest {
        val bookRepository = FakeBookRepository()
        val bookshelfRepository = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = TEST_SHELF_ID
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        // Enter short query
        vm.onAction(BookshelfAction.OnSearchClick)
        advanceUntilIdle()
        vm.onAction(BookshelfAction.OnSearchQueryChange("a"))
        
        advanceUntilIdle()
        
        // Should not have triggered search
        assertTrue(bookRepository.searchQueries.isEmpty())
        
        job.cancel()
    }

    @Test
    fun rapid_queries_debounced_properly() = runTest {
        val bookRepository = FakeBookRepository()
        val bookshelfRepository = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = TEST_SHELF_ID
        )

        // Collect state to trigger initialization
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()

        // Open search and rapidly type
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("h"))
        vm.onAction(BookshelfAction.OnSearchQueryChange("he"))
        vm.onAction(BookshelfAction.OnSearchQueryChange("hel"))
        vm.onAction(BookshelfAction.OnSearchQueryChange("hello"))

        // Wait for debounce to complete
        testDispatcher.scheduler.advanceTimeBy(DEBOUNCE_DELAY_MS)
        testDispatcher.scheduler.runCurrent()

        // Should only search for the final query
        assertEquals(listOf("hello"), bookRepository.searchQueries)

        job.cancel()
    }

    @Test
    fun search_results_update_state() = runTest {
        val bookRepository = FakeBookRepository().apply {
            searchResult = listOf(sampleBook("result1"), sampleBook("result2"))
        }
        val bookshelfRepository = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = TEST_SHELF_ID
        )

        // Collect state to trigger initialization
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()

        // Trigger search
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("books"))

        // Wait for debounce and search to complete
        testDispatcher.scheduler.advanceTimeBy(DEBOUNCE_DELAY_MS)
        testDispatcher.scheduler.runCurrent()

        // Should update search results
        assertEquals(2, latestState?.searchResults?.size)
        assertEquals("result1", latestState?.searchResults?.first()?.id)

        job.cancel()
    }

    @Test
    fun search_error_handling() = runTest {
        val bookRepository = FakeBookRepository().apply {
            shouldReturnError = true
        }
        val bookshelfRepository = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = TEST_SHELF_ID
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        // Trigger search
        vm.onAction(BookshelfAction.OnSearchClick)
        advanceUntilIdle()
        vm.onAction(BookshelfAction.OnSearchQueryChange("test"))
        
        // Wait for debounce period
        testDispatcher.scheduler.advanceTimeBy(DEBOUNCE_DELAY_MS)
        testDispatcher.scheduler.runCurrent()
        
        // Should show error
        assertTrue(latestState?.errorMessage != null)
        assertTrue(latestState?.searchResults?.isEmpty() == true)
        
        job.cancel()
    }
}