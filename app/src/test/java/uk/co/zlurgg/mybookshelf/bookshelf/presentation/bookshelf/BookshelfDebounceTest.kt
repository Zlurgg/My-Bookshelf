package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.test.TestIdGenerator

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookshelfIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class TestBookRepository : BookRepository {
        val searchCalls = mutableListOf<String>()
        var searchResults: List<Book> = emptyList()

        override suspend fun getBookById(bookId: String): Book? = null

        override suspend fun upsertBook(book: Book) {}

        override suspend fun deleteBook(bookId: String) {}

        override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
            return Result.Success(null)
        }

        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            searchCalls.add(query)
            return Result.Success(searchResults)
        }
    }

    private class TestBookshelfRepository : BookshelfRepository {
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
    fun debounce_filters_rapid_queries() = runTest {
        val bookRepository = TestBookRepository()
        val bookshelfRepository = TestBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = "shelf1"
        )

        // Collect state to trigger initialization
        var latestState: BookshelfState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }

        advanceUntilIdle()

        // Open search and rapidly type
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("h"))
        vm.onAction(BookshelfAction.OnSearchQueryChange("he"))
        vm.onAction(BookshelfAction.OnSearchQueryChange("hel"))
        vm.onAction(BookshelfAction.OnSearchQueryChange("hello"))

        // Wait for debounce to complete
        advanceUntilIdle()

        // Should only search for the final query
        assertEquals(listOf("hello"), bookRepository.searchCalls)

        job.cancel()
    }

    @Test
    fun search_results_update_state() = runTest {
        val bookRepository = TestBookRepository().apply {
            searchResults = listOf(sampleBook("result1"), sampleBook("result2"))
        }
        val bookshelfRepository = TestBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepository,
            bookshelfRepository = bookshelfRepository,
            shelfId = "shelf1"
        )

        // Collect state to trigger initialization
        var latestState: BookshelfState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }

        advanceUntilIdle()

        // Trigger search
        vm.onAction(BookshelfAction.OnSearchClick)
        vm.onAction(BookshelfAction.OnSearchQueryChange("books"))

        // Wait for search to complete
        advanceUntilIdle()

        // Should update search results
        assertEquals(2, latestState?.searchResults?.size)
        assertEquals("result1", latestState?.searchResults?.first()?.id)

        job.cancel()
    }
}