package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchedBookDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class BookshelfRepositoryImplTest {

    private class FakeDao : BookshelfDao {
        val books = LinkedHashMap<String, BookEntity>()
        val crossRefs = mutableListOf<BookshelfBookCrossRef>()
        val shelfFlowMap = mutableMapOf<String, MutableStateFlow<List<BookEntity>>>()

        override suspend fun upsert(book: BookEntity) {
            books[book.id] = book
            // update any shelf flows where this book is present
            crossRefs.groupBy { it.shelfId }.forEach { (shelf, list) ->
                val ids = list.map { it.bookId }.toSet()
                val current = books.values.filter { it.id in ids }
                shelfFlowMap.getOrPut(shelf) { MutableStateFlow(emptyList()) }.value = current
            }
        }

        override suspend fun getBookById(id: String): BookEntity? = books[id]

        override suspend fun upsertShelf(shelf: BookshelfEntity) { /* not used */ }

        override fun getAllShelves(): Flow<List<BookshelfEntity>> = MutableStateFlow(emptyList())

        override suspend fun deleteShelf(id: String) { /* not used */ }

        override suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef) {
            crossRefs.removeAll { it.shelfId == crossRef.shelfId && it.bookId == crossRef.bookId }
            crossRefs.add(crossRef)
            val ids = crossRefs.filter { it.shelfId == crossRef.shelfId }.map { it.bookId }.toSet()
            val list = books.values.filter { it.id in ids }
            shelfFlowMap.getOrPut(crossRef.shelfId) { MutableStateFlow(emptyList()) }.value = list
        }

        override suspend fun deleteCrossRef(shelfId: String, bookId: String) {
            crossRefs.removeAll { it.shelfId == shelfId && it.bookId == bookId }
            val ids = crossRefs.filter { it.shelfId == shelfId }.map { it.bookId }.toSet()
            val list = books.values.filter { it.id in ids }
            shelfFlowMap.getOrPut(shelfId) { MutableStateFlow(emptyList()) }.value = list
        }

        override suspend fun deleteAllCrossRefsForShelf(shelfId: String) {
            crossRefs.removeAll { it.shelfId == shelfId }
            shelfFlowMap.getOrPut(shelfId) { MutableStateFlow(emptyList()) }.value = emptyList()
        }

        override fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>> =
            shelfFlowMap.getOrPut(shelfId) { MutableStateFlow(emptyList()) }

        override fun getBookCountForShelf(shelfId: String): Flow<Int> =
            shelfFlowMap.getOrPut(shelfId) { MutableStateFlow(emptyList()) }.map { it.size }

        override fun isBookInAnyShelf(bookId: String): Flow<Boolean> = MutableStateFlow(false)
        override fun getShelvesForBook(bookId: String): Flow<List<String>> = MutableStateFlow(emptyList())
    }

    private class FakeRemote : RemoteBookDataSource {
        override suspend fun searchBooks(query: String, resultLimit: Int?): Result<SearchResponseDto, DataError.Remote> {
            return Result.Success(
                SearchResponseDto(
                    results = listOf(
                        SearchedBookDto(
                            id = "/works/OL123W",
                            title = "Test Book",
                            coverKey = "OL123M",
                            coverAlternativeKey = 123,
                            authorNames = listOf("Author"),
                            authorKeys = listOf("A1"),
                            ratingsAverage = 4.0,
                            ratingsCount = 10,
                            firstPublishYear = 2000,
                            languages = listOf("eng"),
                            numPagesMedian = 321,
                            numEditions = 2
                        )
                    )
                )
            )
        }

        override suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote> {
            return Result.Success(BookWorkDto(description = "A description for $bookWorkId"))
        }
    }

    private fun sampleBook(id: String = "OL123W") = Book(
        id = id,
        title = "Title",
        authors = listOf("Author"),
        imageUrl = "http://example.com/cover.jpg",
        description = null,
        languages = listOf("eng"),
        firstPublishYear = "2000",
        averageRating = 4.0,
        ratingCount = 10,
        numPages = 300,
        numEditions = 1,
        purchased = false,
        affiliateLink = "",
        spineColor = 0xFF000000.toInt()
    )

    @Test
    fun upsert_and_getBookById_roundtrip() = runBlocking {
        val repo = BookshelfRepositoryImpl(FakeRemote(), FakeDao())
        val book = sampleBook("OLX")
        repo.upsertBook(book)
        val loaded = repo.getBookById("OLX")
        assertNotNull(loaded)
        assertEquals(book.id, loaded!!.id)
        assertEquals(book.title, loaded.title)
    }

    @Test
    fun getBookDescription_maps_value() = runBlocking {
        val repo = BookshelfRepositoryImpl(FakeRemote(), FakeDao())
        val result = repo.getBookDescription("OL123W")
        when (result) {
            is Result.Success -> assertEquals("A description for OL123W", result.data)
            is Result.Error -> throw AssertionError("Expected success")
        }
    }

    @Test
    fun addBookToShelf_links_and_flows() = runBlocking {
        val dao = FakeDao()
        val repo = BookshelfRepositoryImpl(FakeRemote(), dao)
        val shelfId = "s1"
        val book = sampleBook("OLFLOW")
        repo.addBookToShelf(shelfId, book)
        val list = repo.getBooksForShelf(shelfId).first()
        assertEquals(1, list.size)
        assertEquals("OLFLOW", list.first().id)
    }
}
