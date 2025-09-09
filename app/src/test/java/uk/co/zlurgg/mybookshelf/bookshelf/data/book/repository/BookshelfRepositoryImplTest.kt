package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.test.TestIdGenerator
import uk.co.zlurgg.mybookshelf.test.TestTimeProvider

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

        override suspend fun deleteBook(id: String) {
            books.remove(id)
            // update any shelf flows where this book was present
            crossRefs.groupBy { it.shelfId }.forEach { (shelf, list) ->
                val ids = list.map { it.bookId }.toSet()
                val current = books.values.filter { it.id in ids }
                shelfFlowMap.getOrPut(shelf) { MutableStateFlow(emptyList()) }.value = current
            }
        }

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

    private fun sampleBook(id: String = TestIdGenerator.generateBookId("OL")) = Book(
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
    fun addBookToShelf_links_and_flows() = runBlocking {
        val dao = FakeDao()
        val timeProvider = TestTimeProvider(1000L)
        val repo = BookshelfRepositoryImpl(dao, timeProvider)
        val shelfId = "s1"
        val book = sampleBook("OLFLOW")
        
        // First save the book to the database
        dao.upsert(book.toBookEntity())
        
        // Then add it to the shelf
        repo.addBookToShelf(shelfId, book.id)
        
        val list = repo.getBooksForShelf(shelfId).first()
        assertEquals(1, list.size)
        assertEquals("OLFLOW", list.first().id)
    }
    
    @Test
    fun removeBookFromShelf_removes_link() = runBlocking {
        val dao = FakeDao()
        val timeProvider = TestTimeProvider(1000L)
        val repo = BookshelfRepositoryImpl(dao, timeProvider)
        val shelfId = "s1"
        val book = sampleBook("OLREMOVE")
        
        // Save book and add to shelf
        dao.upsert(book.toBookEntity())
        repo.addBookToShelf(shelfId, book.id)
        
        // Verify it's there
        var list = repo.getBooksForShelf(shelfId).first()
        assertEquals(1, list.size)
        
        // Remove from shelf
        repo.removeBookFromShelf(shelfId, book.id)
        
        // Verify it's gone
        list = repo.getBooksForShelf(shelfId).first()
        assertEquals(0, list.size)
    }
}

// Extension to convert Book to BookEntity for testing
private fun Book.toBookEntity() = BookEntity(
    id = id,
    title = title,
    authors = authors,
    imageUrl = imageUrl,
    description = description,
    languages = languages,
    firstPublishYear = firstPublishYear,
    ratingsAverage = averageRating,
    ratingsCount = ratingCount,
    numEditions = numEditions,
    purchased = purchased,
    affiliateLink = affiliateLink,
    spineColor = spineColor,
    numPagesMedian = numPages
)