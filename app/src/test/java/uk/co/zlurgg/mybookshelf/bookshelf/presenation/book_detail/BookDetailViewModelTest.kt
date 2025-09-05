package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail

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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

@OptIn(ExperimentalCoroutinesApi::class)

@RunWith(RobolectricTestRunner::class)
class BookDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeRepo : BookshelfRepository {
        var upserted: Book? = null
        var addedPair: Pair<String, Book>? = null
        var removedPair: Pair<String, String>? = null
        var storedBook: Book? = null
        var description: String? = "desc"
        private val booksOnShelf = MutableStateFlow<List<Book>>(emptyList())

        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            return Result.Success(emptyList())
        }

        override suspend fun addBookToShelf(shelfId: String, book: Book) {
            addedPair = shelfId to book
            booksOnShelf.value = booksOnShelf.value + book
        }

        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
            removedPair = shelfId to bookId
            booksOnShelf.value = booksOnShelf.value.filterNot { it.id == bookId }
        }

        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> = booksOnShelf

        override suspend fun upsertBook(book: Book) { upserted = book }

        override suspend fun getBookById(bookId: String): Book? = storedBook

        override suspend fun getBookDescription(workId: String): Result<String?, DataError.Remote> =
            Result.Success(description)
    }

    private fun sampleBook(onShelf: Boolean = false) = Book(
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
        spineColor = 0xFF000000.toInt(),
        onShelf = onShelf,
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
        val repo = FakeRepo().apply { storedBook = sampleBook(onShelf = false) }
        val vm = BookDetailViewModel(repo, bookId = "OLID", shelfId = "S1")
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Initially book is not on shelf, click should add it
        vm.onAction(BookDetailAction.OnAddBookClick(sampleBook(onShelf = false)))
        advanceUntilIdle()
        assertNotNull(repo.addedPair)
        assertEquals("S1", repo.addedPair?.first)
        assertEquals("OLID", repo.addedPair?.second?.id)
        
        // Now book should be on shelf, click should remove it
        vm.onAction(BookDetailAction.OnAddBookClick(sampleBook(onShelf = true)))
        advanceUntilIdle()
        assertNotNull(repo.removedPair)
        assertEquals("S1" to "OLID", repo.removedPair)
        
        job.cancel()
    }
}
