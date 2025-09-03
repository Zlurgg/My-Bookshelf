package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

class BookDetailViewModelTest {

    private class FakeRepo : BookshelfRepository {
        var upserted: Book? = null
        var addedPair: Pair<String, Book>? = null
        var removedPair: Pair<String, String>? = null
        var storedBook: Book? = null
        var description: String? = "desc"

        override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
            return Result.Success(emptyList())
        }

        override suspend fun addBookToShelf(shelfId: String, book: Book) {
            addedPair = shelfId to book
        }

        override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
            removedPair = shelfId to bookId
        }

        override fun getBooksForShelf(shelfId: String) = throw UnsupportedOperationException()

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
        spineColor = Color.Black,
        onShelf = onShelf,
    )

    @Test
    fun loads_book_and_merges_description() = runBlocking {
        val repo = FakeRepo().apply { storedBook = sampleBook() ; description = "MORE" }
        val vm = BookDetailViewModel(repo, bookId = "OLID", shelfId = "S1")
        // Allow onStart to run
        val state = vm.state.value
        assertNotNull(state.book)
        assertEquals("MORE", vm.state.value.book?.description)
    }

    @Test
    fun toggles_add_remove_based_on_onShelf() = runBlocking {
        val repo = FakeRepo().apply { storedBook = sampleBook(onShelf = false) }
        val vm = BookDetailViewModel(repo, bookId = "OLID", shelfId = "S1")
        // simulate button: add when not on shelf
        vm.onAction(BookDetailAction.OnAddBookClick(sampleBook(onShelf = false)))
        assertEquals("S1" to sampleBook(onShelf = true), repo.addedPair)
        // simulate toggle remove
        repo.storedBook = sampleBook(onShelf = true)
        vm.onAction(BookDetailAction.OnAddBookClick(sampleBook(onShelf = true)))
        assertEquals("S1" to "OLID", repo.removedPair)
    }
}
