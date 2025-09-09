package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

@RunWith(RobolectricTestRunner::class)
class BookshelfViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeBookRepository : BookRepository {
        val upserts = mutableListOf<Book>()
        var searchResults = emptyList<Book>()

        override suspend fun getBookById(bookId: String): Book? = null

        override suspend fun upsertBook(book: Book) {
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

        override suspend fun addBookToShelf(shelfId: String, bookId: String) {
            addedBooks.add(shelfId to bookId)
        }

        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
            removedBooks.add(shelfId to bookId)
        }

        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> {
            return shelves.getOrPut(shelfId) { MutableStateFlow(emptyList()) }
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
        
        // Check that book was upserted
        assertEquals(listOf(book), bookRepo.upserts)
    }
}