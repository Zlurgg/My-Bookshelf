package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class BookshelfSearchTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class MockBookRepository : BookRepository {
        val searchQueries = mutableListOf<String>()
        var searchResult: List<Book> = emptyList()
        var shouldReturnError = false

        override suspend fun getBookById(bookId: String): Book? = null

        override suspend fun upsertBook(book: Book) {}

        override suspend fun deleteBook(bookId: String) {}

        override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
            return Result.Success(null)
        }

        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            searchQueries.add(query)
            return if (shouldReturnError) {
                Result.Error(DataError.Remote.REQUEST_TIMEOUT)
            } else {
                Result.Success(searchResult)
            }
        }
    }

    private class MockBookshelfRepository : BookshelfRepository {
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
        affiliateLink = "",
        spineColor = 0xFF0000FF.toInt()
    )

    @Test
    fun search_query_updates_ui_state_immediately() = runTest {
        val bookRepository = MockBookRepository()
        val bookshelfRepository = MockBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = "shelf1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Open search dialog and enter query
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("test query"))
        
        // State should update immediately (before debounce)
        assertEquals("test query", latestState?.searchQuery)
        assertTrue(latestState?.isSearchDialogVisible == true)
        
        job.cancel()
    }

    @Test
    fun search_triggers_repository_after_debounce() = runTest {
        val bookRepository = MockBookRepository().apply {
            searchResult = listOf(sampleBook("result1"))
        }
        val bookshelfRepository = MockBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository, 
            shelfId = "shelf1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Open search dialog and enter query
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("test"))
        
        // Wait for debounce period
        advanceUntilIdle()
        
        // Should have called search
        assertEquals(listOf("test"), bookRepository.searchQueries)
        assertEquals(listOf(sampleBook("result1")), latestState?.searchResults)
        
        job.cancel()
    }

    @Test
    fun short_queries_do_not_trigger_search() = runTest {
        val bookRepository = MockBookRepository()
        val bookshelfRepository = MockBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = "shelf1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Enter short query
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("a"))
        
        advanceUntilIdle()
        
        // Should not have triggered search
        assertTrue(bookRepository.searchQueries.isEmpty())
        
        job.cancel()
    }

    @Test
    fun search_error_updates_state() = runTest {
        val bookRepository = MockBookRepository().apply {
            shouldReturnError = true
        }
        val bookshelfRepository = MockBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = "shelf1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Trigger search
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("test"))
        
        advanceUntilIdle()
        
        // Should show error
        assertTrue(latestState?.errorMessage != null)
        assertTrue(latestState?.searchResults?.isEmpty() == true)
        
        job.cancel()
    }

    @Test
    fun dismiss_search_dialog_resets_state() = runTest {
        val bookRepository = MockBookRepository()
        val bookshelfRepository = MockBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = "shelf1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Set up search state
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("test"))
        
        // Dismiss dialog
        vm.onAction(BookshelfAction.OnDismissSearchDialog)
        
        // State should be reset
        assertEquals("", latestState?.searchQuery)
        assertEquals(false, latestState?.isSearchDialogVisible)
        assertTrue(latestState?.searchResults?.isEmpty() == true)
        
        job.cancel()
    }
}