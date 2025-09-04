package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
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
class BookshelfDebounceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeRepo : BookshelfRepository {
        var searchCalls = 0
        var lastQuery: String? = null
        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            searchCalls += 1
            lastQuery = query
            return Result.Success(emptyList())
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
        title = "t",
        authors = listOf("a"),
        imageUrl = "http://",
        description = null,
        languages = listOf("eng"),
        firstPublishYear = "2000",
        averageRating = 4.0,
        ratingCount = 5,
        numPages = 100,
        numEditions = 1,
        purchased = false,
        affiliateLink = "",
        spineColor = Color.Black,
        onShelf = false,
    )

    @Test
    fun typing_is_debounced_only_last_query_triggers() = runTest {
        val repo = FakeRepo()
        val vm = BookshelfViewModel(repo, shelfId = "S1")
        
        // Allow ViewModel initialization to complete
        advanceUntilIdle()

        // Rapid typing - need to ensure queries are >= 2 chars
        vm.onAction(BookshelfAction.OnSearchQueryChange("ha"))
        advanceUntilIdle()
        vm.onAction(BookshelfAction.OnSearchQueryChange("har"))
        advanceUntilIdle()  
        vm.onAction(BookshelfAction.OnSearchQueryChange("harry"))
        advanceUntilIdle()

        // Immediately after typing, no search should have been executed yet due to debounce
        assertEquals(0, repo.searchCalls)

        // Wait past debounce window (450ms + buffer)
        advanceTimeBy(500)
        advanceUntilIdle()

        // Exactly one search should have been triggered with the latest query
        assertEquals(1, repo.searchCalls)
        assertEquals("harry", repo.lastQuery)
    }
}
