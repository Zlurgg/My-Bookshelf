package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookshelfIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class TestRepository : BookshelfRepository {
        val searchCalls = mutableListOf<String>()
        var searchResults: List<Book> = emptyList()

        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            searchCalls.add(query)
            return Result.Success(searchResults)
        }
        
        override suspend fun addBookToShelf(shelfId: String, book: Book) {}
        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {}
        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> = MutableStateFlow(emptyList())
        override suspend fun upsertBook(book: Book) {}
        override suspend fun getBookById(bookId: String): Book? = null
        override suspend fun getBookDescription(workId: String): Result<String?, DataError.Remote> = Result.Success(null)
    }

    private fun sampleBook(id: String = "ID") = Book(
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
        spineColor = Color.Blue,
        onShelf = false,
    )

    @Test
    fun search_integration_basic_functionality() = runTest {
        val repository = TestRepository()
        repository.searchResults = listOf(sampleBook("search-result"))
        val vm = BookshelfViewModel(repository, shelfId = "test-shelf")
        
        var currentState = vm.state.value
        val collectJob = launch {
            vm.state.collect { currentState = it }
        }
        advanceUntilIdle()

        // Verify initial state
        assertEquals("test-shelf", currentState.shelfId)
        assertEquals("", currentState.searchQuery)
        assertEquals(false, currentState.isSearchDialogVisible)

        // Open search dialog
        vm.onAction(BookshelfAction.OnSearchClick)
        advanceUntilIdle()
        assertEquals(true, currentState.isSearchDialogVisible)

        // Update search query
        vm.onAction(BookshelfAction.OnSearchQueryChange("test query"))
        advanceUntilIdle()
        assertEquals("test query", currentState.searchQuery)

        // Close search dialog
        vm.onAction(BookshelfAction.OnDismissSearchDialog)
        advanceUntilIdle()
        assertEquals(false, currentState.isSearchDialogVisible)
        assertEquals("", currentState.searchQuery) // Should be reset

        collectJob.cancel()
    }

    @Test
    fun viewmodel_handles_actions_without_errors() = runTest {
        val repository = TestRepository()
        val vm = BookshelfViewModel(repository, shelfId = "test-shelf")
        
        val collectJob = launch {
            vm.state.collect { }
        }
        advanceUntilIdle()

        // Test all actions to ensure they don't throw exceptions
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("test"))
        vm.onAction(BookshelfAction.OnBookClick(sampleBook()))
        vm.onAction(BookshelfAction.OnAddBookClick(sampleBook()))
        vm.onAction(BookshelfAction.OnDismissSearchDialog)
        vm.onAction(BookshelfAction.OnBackClick)
        advanceUntilIdle()

        // If we get here without exceptions, the test passes
        assertEquals("Actions handled successfully", "test-shelf", vm.state.value.shelfId)

        collectJob.cancel()
    }
}
