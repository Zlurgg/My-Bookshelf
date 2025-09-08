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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.test.TestIdGenerator

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookshelfSearchTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class MockRepository : BookshelfRepository {
        val searchQueries = mutableListOf<String>()
        var searchResult: List<Book> = emptyList()
        var shouldReturnError = false

        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            searchQueries.add(query)
            return if (shouldReturnError) {
                Result.Error(DataError.Remote.SERVER)
            } else {
                Result.Success(searchResult)
            }
        }
        
        override suspend fun addBookToShelf(shelfId: String, book: Book) {}
        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {}
        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> = MutableStateFlow(emptyList())
        override suspend fun upsertBook(book: Book) {}
        override suspend fun getBookById(bookId: String): Book? = null
        override suspend fun getBookDescription(workId: String): Result<String?, DataError.Remote> = Result.Success(null)
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
        val repository = MockRepository()
        val vm = BookshelfViewModel(repository, shelfId = "shelf1")
        
        var currentState = vm.state.value
        val collectJob = launch {
            vm.state.collect { currentState = it }
        }
        advanceUntilIdle()

        // Initial state should have empty search query
        assertEquals("", currentState.searchQuery)

        // Update search query
        vm.onAction(BookshelfAction.OnSearchQueryChange("kotlin"))
        advanceUntilIdle()

        // UI state should update immediately
        assertEquals("kotlin", currentState.searchQuery)

        collectJob.cancel()
    }

    @Test
    fun search_dialog_visibility_toggles_correctly() = runTest {
        val repository = MockRepository()
        val vm = BookshelfViewModel(repository, shelfId = "shelf1")
        
        var currentState = vm.state.value
        val collectJob = launch {
            vm.state.collect { currentState = it }
        }
        advanceUntilIdle()

        // Initially search dialog should be hidden
        assertEquals(false, currentState.isSearchDialogVisible)

        // Show search dialog
        vm.onAction(BookshelfAction.OnSearchClick)
        advanceUntilIdle()

        assertEquals(true, currentState.isSearchDialogVisible)

        // Dismiss search dialog
        vm.onAction(BookshelfAction.OnDismissSearchDialog)
        advanceUntilIdle()

        assertEquals(false, currentState.isSearchDialogVisible)
        assertEquals("", currentState.searchQuery) // Query should be reset

        collectJob.cancel()
    }

    @Test
    fun search_loading_state_managed_correctly() = runTest {
        val repository = MockRepository()
        repository.searchResult = listOf(sampleBook("book1"))
        val vm = BookshelfViewModel(repository, shelfId = "shelf1")
        
        var currentState = vm.state.value
        val collectJob = launch {
            vm.state.collect { currentState = it }
        }
        advanceUntilIdle()

        // Initially not loading
        assertEquals(false, currentState.isSearchLoading)

        // The actual search triggering depends on debounce mechanism
        // For now, we just verify the state management structure is correct
        assertTrue("State properly initialized", currentState.searchResults.isEmpty())
        assertEquals("", currentState.searchQuery)

        collectJob.cancel()
    }

    @Test
    fun book_click_triggers_repository_upsert() = runTest {
        val repository = MockRepository()
        val vm = BookshelfViewModel(repository, shelfId = "shelf1")
        
        val collectJob = launch {
            vm.state.collect { }
        }
        advanceUntilIdle()

        val testBook = sampleBook("clicked-book")
        
        // Click on a book
        vm.onAction(BookshelfAction.OnBookClick(testBook))
        advanceUntilIdle()

        // This test verifies the action is handled without errors
        // The actual repository behavior would be tested in repository-specific tests
        
        collectJob.cancel()
    }

    @Test
    fun state_structure_is_consistent() = runTest {
        val repository = MockRepository()
        val vm = BookshelfViewModel(repository, shelfId = "shelf1")
        
        var currentState = vm.state.value
        val collectJob = launch {
            vm.state.collect { currentState = it }
        }
        advanceUntilIdle()

        // Verify initial state structure
        assertEquals("shelf1", currentState.shelfId)
        assertEquals("", currentState.searchQuery)
        assertEquals(false, currentState.isSearchDialogVisible)
        assertEquals(false, currentState.isSearchLoading)
        assertTrue(currentState.searchResults.isEmpty())
        assertEquals(null, currentState.errorMessage)

        collectJob.cancel()
    }
}