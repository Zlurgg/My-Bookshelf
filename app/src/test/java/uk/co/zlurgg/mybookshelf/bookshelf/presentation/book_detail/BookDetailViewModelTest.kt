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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeBookRepository : BookRepository {
        var upserted: Book? = null
        var storedBook: Book? = null
        var description: String? = "desc"

        override suspend fun getBookById(bookId: String): Book? = storedBook

        override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
            return Result.Success(description)
        }

        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            return Result.Success(emptyList())
        }

        override suspend fun upsertBook(book: Book) {
            upserted = book
        }

        override suspend fun deleteBook(bookId: String) {}
    }

    private class FakeBookshelfRepository : BookshelfRepository {
        var addedPair: Pair<String, String>? = null // shelfId to bookId
        var removedPair: Pair<String, String>? = null // shelfId to bookId
        private val isOnShelfFlow = MutableStateFlow(false)
        private val inLibraryFlow = MutableStateFlow(false)

        override suspend fun addBookToShelf(shelfId: String, bookId: String) {
            addedPair = shelfId to bookId
            isOnShelfFlow.value = true
        }

        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
            removedPair = shelfId to bookId
            isOnShelfFlow.value = false
        }

        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> {
            return MutableStateFlow(emptyList())
        }

        override fun isBookInAnyShelf(bookId: String): Flow<Boolean> {
            return inLibraryFlow
        }

        override fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean> {
            return isOnShelfFlow
        }

        override fun getShelvesForBook(bookId: String): Flow<List<String>> {
            return MutableStateFlow(emptyList())
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
        spineColor = 0xFF000000.toInt()
    )

    @Test
    fun loads_book_and_merges_description() = runTest {
        val bookRepo = FakeBookRepository().apply { 
            storedBook = sampleBook()
            description = "MORE" 
        }
        val bookshelfRepo = FakeBookshelfRepository()
        
        val vm = BookDetailViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            bookId = "OLID", 
            shelfId = "S1"
        )
        
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
        val bookRepo = FakeBookRepository().apply { 
            storedBook = sampleBook() 
        }
        val bookshelfRepo = FakeBookshelfRepository()
        
        val vm = BookDetailViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            bookId = "OLID", 
            shelfId = "S1"
        )
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Initially book is not on shelf, click should add it
        vm.onAction(BookDetailAction.OnAddBookClick(sampleBook()))
        advanceUntilIdle()
        
        assertNotNull(bookshelfRepo.addedPair)
        assertEquals("S1", bookshelfRepo.addedPair?.first) // shelfId
        assertEquals("OLID", bookshelfRepo.addedPair?.second) // bookId
        
        job.cancel()
    }
}