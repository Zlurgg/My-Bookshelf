package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

class BookshelfDebounceTest {

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
    fun typing_is_debounced_only_last_query_triggers() = runBlocking {
        val repo = FakeRepo()
        val vm = BookshelfViewModel(repo, shelfId = "S1")

        // Rapid typing
        vm.onAction(BookshelfAction.OnSearchQueryChange("ha"))
        vm.onAction(BookshelfAction.OnSearchQueryChange("har"))
        vm.onAction(BookshelfAction.OnSearchQueryChange("harry"))

        // Immediately after typing, no search should have been executed yet
        assertEquals(0, repo.searchCalls)

        // Wait past debounce window
        delay(600)

        // Exactly one search, with the latest query
        assertEquals(1, repo.searchCalls)
        assertEquals("harry", repo.lastQuery)

        // Dismiss dialog should reset loading state and cancel further searches
        vm.onAction(BookshelfAction.OnDismissSearchDialog)
        // Type again; we will not wait, so still zero extra calls
        vm.onAction(BookshelfAction.OnSearchQueryChange("x"))
        assertEquals(1, repo.searchCalls)
    }
}
