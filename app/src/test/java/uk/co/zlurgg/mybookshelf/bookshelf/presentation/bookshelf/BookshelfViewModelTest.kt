package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
class BookshelfViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeBookRepository : BookRepository {
        val upserts = mutableListOf<Book>()
        var searchResults = emptyList<Book>()
        var shouldFailUpsert = false

        override suspend fun getBookById(bookId: String): Book? = null

        override suspend fun upsertBook(book: Book) {
            if (shouldFailUpsert) {
                throw RuntimeException("Upsert failed")
            }
            upserts.add(book)
        }

        override suspend fun deleteBook(bookId: String) {}

        override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
            return Result.Success(null)
        }

        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            return Result.Success(searchResults)
        }
    }

    private class FakeBookshelfRepository : BookshelfRepository {
        val shelves = mutableMapOf<String, MutableStateFlow<List<Book>>>()
        val addedBooks = mutableListOf<Pair<String, String>>() // shelfId to bookId
        val removedBooks = mutableListOf<Pair<String, String>>() // shelfId to bookId
        var shouldFailAdd = false
        var shouldFailRemove = false
        var shouldFailLoad = false

        override suspend fun addBookToShelf(shelfId: String, bookId: String) {
            if (shouldFailAdd) {
                throw RuntimeException("Add to shelf failed")
            }
            addedBooks.add(shelfId to bookId)
        }

        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
            if (shouldFailRemove) {
                throw RuntimeException("Remove from shelf failed")
            }
            removedBooks.add(shelfId to bookId)
        }

        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> {
            if (shouldFailLoad) {
                throw RuntimeException("Load books failed")
            }
            return shelves.getOrPut(shelfId) { MutableStateFlow(emptyList()) }
        }

        fun setBooksForShelf(shelfId: String, books: List<Book>) {
            shelves.getOrPut(shelfId) { MutableStateFlow(emptyList()) }.value = books
        }

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
    fun onBookClick_persists_book() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        val book = sampleBook("B1")
        
        vm.onAction(BookshelfAction.OnBookClick(book))
        advanceUntilIdle()
        
        assertEquals(listOf(book), bookRepo.upserts)
    }

    @Test
    fun onBookClick_sets_error_on_repository_failure() = runTest {
        val bookRepo = FakeBookRepository().apply { shouldFailUpsert = true }
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        val book = sampleBook("B1")
        vm.onAction(BookshelfAction.OnBookClick(book))
        advanceUntilIdle()
        
        assertTrue(latestState?.errorMessage?.contains("cache book") == true)
        job.cancel()
    }

    @Test
    fun onAddBookClick_adds_book_to_shelf() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        val book = sampleBook("B1")
        
        vm.onAction(BookshelfAction.OnAddBookClick(book))
        advanceUntilIdle()
        
        assertEquals(listOf(book), bookRepo.upserts)
        assertEquals(listOf("S1" to "B1"), bookshelfRepo.addedBooks)
    }

    @Test
    fun onAddBookClick_sets_error_on_failure() = runTest {
        val bookRepo = FakeBookRepository().apply { shouldFailUpsert = true }
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        val book = sampleBook("B1")
        vm.onAction(BookshelfAction.OnAddBookClick(book))
        advanceUntilIdle()
        
        assertTrue(latestState?.errorMessage?.contains("add book") == true)
        job.cancel()
    }

    @Test
    fun onRemoveBook_removes_book_and_shows_undo() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        val book = sampleBook("B1")
        val existingBooks = listOf(book, sampleBook("B2"))
        bookshelfRepo.setBooksForShelf("S1", existingBooks)
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        vm.onAction(BookshelfAction.OnRemoveBook(book))
        advanceUntilIdle()
        
        assertEquals(listOf("S1" to "B1"), bookshelfRepo.removedBooks)
        assertEquals(book, latestState?.recentlyDeleted)
        assertFalse(latestState?.books?.contains(book) == true)
        job.cancel()
    }

    @Test
    fun onRemoveBook_sets_error_on_repository_failure() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository().apply { shouldFailRemove = true }
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        val book = sampleBook("B1")
        vm.onAction(BookshelfAction.OnRemoveBook(book))
        advanceUntilIdle()
        
        assertTrue(latestState?.errorMessage?.contains("remove book") == true)
        job.cancel()
    }

    @Test
    fun onUndoRemove_restores_book() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        val book = sampleBook("B1")
        val existingBooks = listOf(book, sampleBook("B2"))
        bookshelfRepo.setBooksForShelf("S1", existingBooks)
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        vm.onAction(BookshelfAction.OnRemoveBook(book))
        vm.onAction(BookshelfAction.OnUndoRemove)
        
        assertTrue(latestState?.books?.contains(book) == true)
        assertNull(latestState?.recentlyDeleted)
        job.cancel()
    }

    @Test
    fun onUndoRemove_does_nothing_when_no_deleted_book() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        val originalBooks = latestState?.books ?: emptyList()
        vm.onAction(BookshelfAction.OnUndoRemove)
        
        assertEquals(originalBooks, latestState?.books)
        assertNull(latestState?.recentlyDeleted)
        job.cancel()
    }

    @Test
    fun onToggleTidyMode_toggles_state() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        val initialTidyMode = latestState?.isTidyMode ?: false
        vm.onAction(BookshelfAction.OnToggleTidyMode)
        advanceUntilIdle()
        
        assertEquals(!initialTidyMode, latestState?.isTidyMode)
        job.cancel()
    }

    @Test
    fun onBackClick_does_nothing() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        val stateBefore = latestState?.copy()
        vm.onAction(BookshelfAction.OnBackClick)
        
        assertEquals(stateBefore, latestState)
        job.cancel()
    }

    @Test
    fun init_loads_books_from_repository() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        
        val testBooks = listOf(sampleBook("B1"), sampleBook("B2"))
        bookshelfRepo.setBooksForShelf("S1", testBooks)
        
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        assertEquals(testBooks, latestState?.books)
        assertFalse(latestState?.isLoading == true)
        job.cancel()
    }

    @Test
    fun init_sets_error_on_load_failure() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository().apply { shouldFailLoad = true }
        
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()
        
        assertTrue(latestState?.errorMessage?.contains("load books") == true)
        assertFalse(latestState?.isLoading == true)
        job.cancel()
    }

    @Test
    fun books_flow_updates_state_continuously() = runTest {
        val bookRepo = FakeBookRepository()
        val bookshelfRepo = FakeBookshelfRepository()
        val vm = BookshelfViewModel(
            bookRepository = bookRepo,
            bookshelfRepository = bookshelfRepo,
            shelfId = "S1"
        )
        
        var latestState: BookshelfState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()

        val newBooks = listOf(sampleBook("B1"), sampleBook("B2"))
        bookshelfRepo.setBooksForShelf("S1", newBooks)
        advanceUntilIdle()
        
        assertEquals(newBooks, latestState?.books)
        job.cancel()
    }
}