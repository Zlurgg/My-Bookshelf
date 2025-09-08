package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

@OptIn(ExperimentalCoroutinesApi::class)

@RunWith(RobolectricTestRunner::class)
class BookDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeRepo : BookRepository {
        var upserted: Book? = null
        var addedPair: Pair<String, String>? = null
        var removedPair: Pair<String, String>? = null
        var storedBook: Book? = null
        var description: String? = "desc"
        private val isOnShelfFlow = MutableStateFlow(false)

        override suspend fun getBookById(bookId: String): Book? = storedBook

        override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
            return Result.Success(description)
        }

        override suspend fun upsertBook(book: Book) {
            upserted = book
        }

        override fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean> = isOnShelfFlow

        override suspend fun addBookToShelf(bookId: String, shelfId: String) {
            addedPair = bookId to shelfId
            isOnShelfFlow.value = true
        }

        override suspend fun removeBookFromShelf(bookId: String, shelfId: String) {
            removedPair = bookId to shelfId
            isOnShelfFlow.value = false
        }

        override fun isBookInAnyShelf(bookId: String): Flow<Boolean> = isOnShelfFlow

        override fun getShelvesForBook(bookId: String): Flow<List<String>> {
            return MutableStateFlow(if (isOnShelfFlow.value) listOf("S1") else emptyList())
        }
    }

    private fun sampleBook() = Book(
        id = "OLID",
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
        spineColor = 0xFF000000.toInt()
    )

    @Test
    fun loads_book_and_merges_description() = runTest {
        val repo = FakeRepo().apply { storedBook = sampleBook() ; description = "MORE" }
        val vm = BookDetailViewModel(repo, bookId = "OLID", shelfId = "S1")
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        // Wait for all async operations to complete
        advanceUntilIdle()
        
        assertNotNull(latestState)
        assertNotNull(latestState?.book)
        assertEquals("MORE", latestState?.book?.description)
        
        job.cancel()
    }

    @Test
    fun toggles_add_remove_based_on_onShelf() = runTest {
        val repo = FakeRepo().apply { storedBook = sampleBook() }
        val vm = BookDetailViewModel(repo, bookId = "OLID", shelfId = "S1")
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Initially book is not on shelf, click should add it
        vm.onAction(BookDetailAction.OnAddBookClick(sampleBook()))
        advanceUntilIdle()
        assertNotNull(repo.addedPair)
        assertEquals("OLID", repo.addedPair?.first)
        assertEquals("S1", repo.addedPair?.second)
        
        // Now book should be on shelf, click should remove it
        vm.onAction(BookDetailAction.OnAddBookClick(sampleBook()))
        advanceUntilIdle()
        assertNotNull(repo.removedPair)
        assertEquals("OLID" to "S1", repo.removedPair)
        
        job.cancel()
    }
}
