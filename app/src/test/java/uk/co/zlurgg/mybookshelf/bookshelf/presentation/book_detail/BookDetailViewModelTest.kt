package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.test.*
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.BookDetailUseCases

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    companion object {
        private const val TEST_BOOK_ID = "OLID"
        private const val TEST_SHELF_ID = "S1"
    }

    private class FakeBookRepository : BookRepository {
        var upserted: Book? = null
        var storedBook: Book? = null
        var description: String? = "desc"

        override suspend fun getBookById(bookId: String): Book? = storedBook

        override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
            return Result.Success(description)
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

        fun setIsOnShelf(value: Boolean) {
            isOnShelfFlow.value = value
        }

        fun setInLibrary(value: Boolean) {
            inLibraryFlow.value = value
        }

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
        id = TEST_BOOK_ID,
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
        
        val bookDetailUseCases = BookDetailUseCases(
            getBookDetails = FakeGetBookDetailsUseCase(),
            addBookToShelf = FakeAddBookToShelfUseCase(),
            removeBookFromShelf = FakeRemoveBookFromShelfUseCase(),
            upsertBook = FakeUpsertBookUseCase(bookRepo),
            toggleBookPurchase = FakeToggleBookPurchaseUseCase()
        )
        val vm = BookDetailViewModel(
            bookDetailUseCases = bookDetailUseCases,
            bookId = TEST_BOOK_ID,
            shelfId = TEST_SHELF_ID
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
        
        val bookDetailUseCases = BookDetailUseCases(
            getBookDetails = FakeGetBookDetailsUseCase(),
            addBookToShelf = FakeAddBookToShelfUseCase(),
            removeBookFromShelf = FakeRemoveBookFromShelfUseCase(),
            upsertBook = FakeUpsertBookUseCase(bookRepo),
            toggleBookPurchase = FakeToggleBookPurchaseUseCase()
        )
        val vm = BookDetailViewModel(
            bookDetailUseCases = bookDetailUseCases,
            bookId = TEST_BOOK_ID,
            shelfId = TEST_SHELF_ID
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
        assertEquals(TEST_SHELF_ID, bookshelfRepo.addedPair?.first) // shelfId
        assertEquals(TEST_BOOK_ID, bookshelfRepo.addedPair?.second) // bookId
        
        job.cancel()
    }

    @Test
    fun onRemoveBookClick_removes_book_from_shelf() = runTest {
        val bookRepo = FakeBookRepository().apply { 
            storedBook = sampleBook() 
        }
        val bookshelfRepo = FakeBookshelfRepository().apply {
            setIsOnShelf(true)
        }
        
        val bookDetailUseCases = BookDetailUseCases(
            getBookDetails = FakeGetBookDetailsUseCase(),
            addBookToShelf = FakeAddBookToShelfUseCase(),
            removeBookFromShelf = FakeRemoveBookFromShelfUseCase(),
            upsertBook = FakeUpsertBookUseCase(bookRepo),
            toggleBookPurchase = FakeToggleBookPurchaseUseCase()
        )
        val vm = BookDetailViewModel(
            bookDetailUseCases = bookDetailUseCases,
            bookId = TEST_BOOK_ID,
            shelfId = TEST_SHELF_ID
        )
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Remove book
        vm.onAction(BookDetailAction.OnRemoveBookClick(sampleBook()))
        advanceUntilIdle()
        
        assertNotNull(bookshelfRepo.removedPair)
        assertEquals(TEST_SHELF_ID, bookshelfRepo.removedPair?.first)
        assertEquals(TEST_BOOK_ID, bookshelfRepo.removedPair?.second)
        
        job.cancel()
    }

    @Test
    fun onRateBookDetailClick_updates_book_rating() = runTest {
        val bookRepo = FakeBookRepository().apply { 
            storedBook = sampleBook() 
        }
        val bookshelfRepo = FakeBookshelfRepository()
        
        val bookDetailUseCases = BookDetailUseCases(
            getBookDetails = FakeGetBookDetailsUseCase(),
            addBookToShelf = FakeAddBookToShelfUseCase(),
            removeBookFromShelf = FakeRemoveBookFromShelfUseCase(),
            upsertBook = FakeUpsertBookUseCase(bookRepo),
            toggleBookPurchase = FakeToggleBookPurchaseUseCase()
        )
        val vm = BookDetailViewModel(
            bookDetailUseCases = bookDetailUseCases,
            bookId = TEST_BOOK_ID,
            shelfId = TEST_SHELF_ID
        )
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Rate book with 5 stars
        vm.onAction(BookDetailAction.OnRateBookDetailClick(5))
        advanceUntilIdle()
        
        // Should upsert book with updated rating
        assertNotNull(bookRepo.upserted)
        
        job.cancel()
    }

    @Test
    fun onPurchaseClick_marks_book_as_purchased() = runTest {
        val bookRepo = FakeBookRepository().apply { 
            storedBook = sampleBook() 
        }
        val bookshelfRepo = FakeBookshelfRepository()
        
        val bookDetailUseCases = BookDetailUseCases(
            getBookDetails = FakeGetBookDetailsUseCase(),
            addBookToShelf = FakeAddBookToShelfUseCase(),
            removeBookFromShelf = FakeRemoveBookFromShelfUseCase(),
            upsertBook = FakeUpsertBookUseCase(bookRepo),
            toggleBookPurchase = FakeToggleBookPurchaseUseCase()
        )
        val vm = BookDetailViewModel(
            bookDetailUseCases = bookDetailUseCases,
            bookId = TEST_BOOK_ID,
            shelfId = TEST_SHELF_ID
        )
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        // Mark as purchased
        vm.onAction(BookDetailAction.OnPurchaseClick)
        advanceUntilIdle()
        
        // Should upsert book with purchased = true
        assertNotNull(bookRepo.upserted)
        assertTrue(bookRepo.upserted?.purchased == true)
        
        job.cancel()
    }

    @Test
    fun onBackClick_does_nothing_locally() = runTest {
        val bookRepo = FakeBookRepository().apply { 
            storedBook = sampleBook() 
        }
        val bookshelfRepo = FakeBookshelfRepository()
        
        val bookDetailUseCases = BookDetailUseCases(
            getBookDetails = FakeGetBookDetailsUseCase(),
            addBookToShelf = FakeAddBookToShelfUseCase(),
            removeBookFromShelf = FakeRemoveBookFromShelfUseCase(),
            upsertBook = FakeUpsertBookUseCase(bookRepo),
            toggleBookPurchase = FakeToggleBookPurchaseUseCase()
        )
        val vm = BookDetailViewModel(
            bookDetailUseCases = bookDetailUseCases,
            bookId = TEST_BOOK_ID,
            shelfId = TEST_SHELF_ID
        )
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        val stateBefore = latestState?.copy()
        
        // Back click should not change local state (handled by navigation)
        vm.onAction(BookDetailAction.OnBackClick)
        
        assertEquals(stateBefore, latestState)
        
        job.cancel()
    }

    @Test
    fun book_not_found_shows_error_state() = runTest {
        val bookRepo = FakeBookRepository().apply { 
            storedBook = null // Book not found
        }
        val bookshelfRepo = FakeBookshelfRepository()
        
        val bookDetailUseCases = BookDetailUseCases(
            getBookDetails = FakeGetBookDetailsUseCase(),
            addBookToShelf = FakeAddBookToShelfUseCase(),
            removeBookFromShelf = FakeRemoveBookFromShelfUseCase(),
            upsertBook = FakeUpsertBookUseCase(bookRepo),
            toggleBookPurchase = FakeToggleBookPurchaseUseCase()
        )
        val vm = BookDetailViewModel(
            bookDetailUseCases = bookDetailUseCases,
            bookId = "NOT_FOUND",
            shelfId = "S1"
        )
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        assertNull(latestState?.book)
        assertFalse(latestState?.isLoading == true)
        
        job.cancel()
    }

    @Test
    fun description_failure_uses_book_without_description() = runTest {
        val bookRepo = FakeBookRepository().apply { 
            storedBook = sampleBook()
            description = null // Description fetch fails
        }
        val bookshelfRepo = FakeBookshelfRepository()
        
        val bookDetailUseCases = BookDetailUseCases(
            getBookDetails = FakeGetBookDetailsUseCase(),
            addBookToShelf = FakeAddBookToShelfUseCase(),
            removeBookFromShelf = FakeRemoveBookFromShelfUseCase(),
            upsertBook = FakeUpsertBookUseCase(bookRepo),
            toggleBookPurchase = FakeToggleBookPurchaseUseCase()
        )
        val vm = BookDetailViewModel(
            bookDetailUseCases = bookDetailUseCases,
            bookId = TEST_BOOK_ID,
            shelfId = TEST_SHELF_ID
        )
        
        // Collect state to trigger onStart
        var latestState: BookDetailState? = null
        val job = launch {
            vm.state.collect { latestState = it }
        }
        
        advanceUntilIdle()
        
        assertNotNull(latestState?.book)
        assertNull(latestState?.book?.description)
        
        job.cancel()
    }
}