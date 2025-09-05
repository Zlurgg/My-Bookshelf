package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

@RunWith(RobolectricTestRunner::class)
class BookshelfViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeRepo : BookshelfRepository {
        val upserts = mutableListOf<Book>()
        val shelves = mutableMapOf<String, MutableStateFlow<List<Book>>>()
        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> = Result.Success(emptyList())
        override suspend fun addBookToShelf(shelfId: String, book: Book) {}
        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {}
        override fun getBooksForShelf(shelfId: String): Flow<List<Book>> = shelves.getOrPut(shelfId) { MutableStateFlow(emptyList()) }
        override suspend fun upsertBook(book: Book) { upserts.add(book) }
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
        spineColor = 0xFF000000.toInt()
    )

    @Test
    fun onBookClick_persists_book() = runTest {
        val repo = FakeRepo()
        val vm = BookshelfViewModel(repo, shelfId = "S1")
        val book = sampleBook("B1")
        vm.onAction(BookshelfAction.OnBookClick(book))
        // allow coroutine to run (runBlocking ensures sequential)
        assertEquals(listOf(book), repo.upserts)
    }
}
