package uk.co.zlurgg.mybookshelf.book.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.book.data.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * Integration test for BookshelfRepository with real Room database.
 * Tests shelf-book relationship management with actual SQLite.
 *
 * This is a medium-scope test (Google's 20% integration test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class BookshelfRepositoryIntegrationTest {

    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var repository: BookshelfRepositoryImpl
    private val testTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1000L
    }

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MyBookshelfRoomDatabase::class.java
        ).build()

        repository = BookshelfRepositoryImpl(database.bookshelfDao, testTimeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addBookToShelfCreatesRealCrossReference() = runTest {
        // Given - Shelf and book exist in database
        val shelfId = "shelf-1"
        val bookId = "book-1"

        database.bookshelfDao.upsertShelf(
            BookshelfEntity(
                id = shelfId,
                name = "Test Shelf",
                shelfMaterial = "DARK_WOOD",
                position = 0
            )
        )
        database.bookshelfDao.upsert(
            createTestBookEntity(bookId, "Test Book")
        )

        // When - Add book to shelf through repository
        repository.addBookToShelf(shelfId, bookId)

        // Then - Book should appear in shelf's book list
        val booksInShelf = repository.getBooksForShelf(shelfId).first()
        assertEquals(1, booksInShelf.size)
        assertEquals(bookId, booksInShelf[0].id)
        assertEquals("Test Book", booksInShelf[0].title)
    }

    @Test
    fun removeBookFromShelfDeletesCrossReference() = runTest {
        // Given - Book is in shelf
        val shelfId = "shelf-1"
        val bookId = "book-1"

        // Setup database
        database.bookshelfDao.upsertShelf(
            BookshelfEntity(
                id = shelfId,
                name = "Test Shelf",
                shelfMaterial = "DARK_WOOD",
                position = 0
            )
        )
        database.bookshelfDao.upsert(
            createTestBookEntity(bookId, "Test Book")
        )
        repository.addBookToShelf(shelfId, bookId)

        // When - Remove book from shelf
        repository.removeBookFromShelf(shelfId, bookId)

        // Then - Shelf should be empty
        val booksInShelf = repository.getBooksForShelf(shelfId).first()
        assertTrue("Shelf should be empty after removal", booksInShelf.isEmpty())
    }

    @Test
    fun isBookInAnyShelfReturnsTrueWhenBookPresent() = runTest {
        // Given - Book is in shelf
        val shelfId = "shelf-1"
        val bookId = "book-1"

        database.bookshelfDao.upsertShelf(
            BookshelfEntity(
                id = shelfId,
                name = "Test Shelf",
                shelfMaterial = "DARK_WOOD",
                position = 0
            )
        )
        database.bookshelfDao.upsert(
            createTestBookEntity(bookId, "Test Book")
        )
        repository.addBookToShelf(shelfId, bookId)

        // When - Check if book is in any shelf
        val isInShelf = repository.isBookInAnyShelf(bookId).first()

        // Then - Should return true
        assertTrue("Book should be detected in shelf", isInShelf)
    }

    @Test
    fun isBookInAnyShelfReturnsFalseWhenBookNotPresent() = runTest {
        // Given - Book exists but not in any shelf
        val bookId = "book-1"
        database.bookshelfDao.upsert(
            createTestBookEntity(bookId, "Test Book")
        )

        // When - Check if book is in any shelf
        val isInShelf = repository.isBookInAnyShelf(bookId).first()

        // Then - Should return false
        assertFalse("Book should not be detected in any shelf", isInShelf)
    }

    @Test
    fun getBooksForShelfReturnsCorrectBooks() = runTest {
        // Given - Shelf with multiple books
        val shelfId = "shelf-1"
        val book1 = createTestBookEntity("book-1", "Book 1")
        val book2 = createTestBookEntity("book-2", "Book 2")
        val book3 = createTestBookEntity("book-3", "Book 3")

        database.bookshelfDao.upsertShelf(
            BookshelfEntity(
                id = shelfId,
                name = "Test Shelf",
                shelfMaterial = "DARK_WOOD",
                position = 0
            )
        )

        listOf(book1, book2, book3).forEach { book ->
            database.bookshelfDao.upsert(book)
            repository.addBookToShelf(shelfId, book.id)
        }

        // When - Get books for shelf
        val booksInShelf = repository.getBooksForShelf(shelfId).first()

        // Then - Should return 3 books
        assertEquals(3, booksInShelf.size)
    }

    private fun createTestBookEntity(id: String, title: String): BookEntity {
        return BookEntity(
            id = id,
            title = title,
            authors = listOf("Test Author"),
            imageUrl = "https://example.com/cover.jpg",
            description = "Test description",
            languages = listOf("en"),
            firstPublishYear = "2024",
            numPagesMedian = 300,
            purchased = false,
            spineColor = 0xFF8B4513.toInt()
        )
    }
}
