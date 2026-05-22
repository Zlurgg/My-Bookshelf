package uk.co.zlurgg.mybookshelf.core.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity

/**
 * DAO layer tests for BookshelfDao.
 *
 * Tests use in-memory Room database with Robolectric for Android framework.
 * Focus on CRUD operations, queries, and data relationships.
 */
@RunWith(RobolectricTestRunner::class)
class BookshelfDaoTest {

    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var dao: BookshelfDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MyBookshelfRoomDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.bookshelfDao
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Book CRUD Tests

    @Test
    fun `upsert book inserts new book`() = runTest {
        // Given
        val book = createTestBook("book-1", "Test Book")

        // When
        dao.upsert(book)

        // Then
        val retrieved = dao.getBookById("book-1")
        assertNotNull("Should retrieve inserted book", retrieved)
        assertEquals("book-1", retrieved?.id)
        assertEquals("Test Book", retrieved?.title)
    }

    @Test
    fun `upsert book updates existing book`() = runTest {
        // Given
        val originalBook = createTestBook("book-1", "Original Title")
        dao.upsert(originalBook)

        // When
        val updatedBook = originalBook.copy(title = "Updated Title")
        dao.upsert(updatedBook)

        // Then
        val retrieved = dao.getBookById("book-1")
        assertEquals("Updated Title", retrieved?.title)
    }

    @Test
    fun `getBookById returns null for non-existent book`() = runTest {
        // When
        val result = dao.getBookById("non-existent")

        // Then
        assertNull("Should return null for non-existent book", result)
    }

    // Shelf CRUD Tests

    @Test
    fun `upsertShelf inserts new shelf`() = runTest {
        // Given
        val shelf = createTestShelf("shelf-1", "My Shelf", 0)

        // When
        dao.upsertShelf(shelf)

        // Then
        val retrieved = dao.getShelfById("shelf-1")
        assertNotNull("Should retrieve inserted shelf", retrieved)
        assertEquals("shelf-1", retrieved?.id)
        assertEquals("My Shelf", retrieved?.name)
    }

    @Test
    fun `upsertShelf updates existing shelf`() = runTest {
        // Given
        val originalShelf = createTestShelf("shelf-1", "Original Name", 0)
        dao.upsertShelf(originalShelf)

        // When
        val updatedShelf = originalShelf.copy(name = "Updated Name", position = 5)
        dao.upsertShelf(updatedShelf)

        // Then
        val retrieved = dao.getShelfById("shelf-1")
        assertEquals("Updated Name", retrieved?.name)
        assertEquals(5, retrieved?.position)
    }

    @Test
    fun `getShelfById returns null for non-existent shelf`() = runTest {
        // When
        val result = dao.getShelfById("non-existent")

        // Then
        assertNull("Should return null for non-existent shelf", result)
    }

    @Test
    fun `deleteShelf removes shelf from database`() = runTest {
        // Given
        val shelf = createTestShelf("shelf-1", "Test Shelf", 0)
        dao.upsertShelf(shelf)

        // When
        dao.deleteShelf("shelf-1")

        // Then
        val retrieved = dao.getShelfById("shelf-1")
        assertNull("Shelf should be deleted", retrieved)
    }

    // Cross-Reference Tests

    @Test
    fun `upsertCrossRef creates book-shelf relationship`() = runTest {
        // Given
        val book = createTestBook("book-1", "Test Book")
        val shelf = createTestShelf("shelf-1", "Test Shelf", 0)
        dao.upsert(book)
        dao.upsertShelf(shelf)

        // When
        val crossRef = BookshelfBookCrossRef("shelf-1", "book-1", System.currentTimeMillis())
        dao.upsertCrossRef(crossRef)

        // Then
        val booksInShelf = dao.getBooksForShelf("shelf-1").first()
        assertEquals(1, booksInShelf.size)
        assertEquals("book-1", booksInShelf[0].id)
    }

    @Test
    fun `deleteCrossRef removes specific book-shelf relationship`() = runTest {
        // Given
        val book = createTestBook("book-1", "Test Book")
        val shelf = createTestShelf("shelf-1", "Test Shelf", 0)
        dao.upsert(book)
        dao.upsertShelf(shelf)
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", System.currentTimeMillis()))

        // When
        dao.deleteCrossRef("shelf-1", "book-1")

        // Then
        val booksInShelf = dao.getBooksForShelf("shelf-1").first()
        assertTrue("Should have no books after deletion", booksInShelf.isEmpty())
    }

    @Test
    fun `deleteAllCrossRefsForShelf removes all books from shelf`() = runTest {
        // Given
        val book1 = createTestBook("book-1", "Book 1")
        val book2 = createTestBook("book-2", "Book 2")
        val shelf = createTestShelf("shelf-1", "Test Shelf", 0)
        dao.upsert(book1)
        dao.upsert(book2)
        dao.upsertShelf(shelf)
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", System.currentTimeMillis()))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-2", System.currentTimeMillis()))

        // When
        dao.deleteAllCrossRefsForShelf("shelf-1")

        // Then
        val booksInShelf = dao.getBooksForShelf("shelf-1").first()
        assertTrue("Should have no books after deletion", booksInShelf.isEmpty())
    }

    // Query Tests

    @Test
    fun `getBooksForShelf returns books ordered by addedAt descending`() = runTest {
        // Given
        val book1 = createTestBook("book-1", "First Added")
        val book2 = createTestBook("book-2", "Second Added")
        val book3 = createTestBook("book-3", "Third Added")
        val shelf = createTestShelf("shelf-1", "Test Shelf", 0)

        dao.upsert(book1)
        dao.upsert(book2)
        dao.upsert(book3)
        dao.upsertShelf(shelf)

        // Add with increasing timestamps
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", 1000))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-2", 2000))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-3", 3000))

        // When
        val books = dao.getBooksForShelf("shelf-1").first()

        // Then
        assertEquals(3, books.size)
        assertEquals("Third Added", books[0].title) // Most recent first
        assertEquals("Second Added", books[1].title)
        assertEquals("First Added", books[2].title)
    }

    @Test
    fun `getBookCountForShelf returns correct count`() = runTest {
        // Given
        val book1 = createTestBook("book-1", "Book 1")
        val book2 = createTestBook("book-2", "Book 2")
        val shelf = createTestShelf("shelf-1", "Test Shelf", 0)
        dao.upsert(book1)
        dao.upsert(book2)
        dao.upsertShelf(shelf)
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", System.currentTimeMillis()))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-2", System.currentTimeMillis()))

        // When
        val count = dao.getBookCountForShelf("shelf-1").first()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `isBookInAnyShelf returns true when book is in shelf`() = runTest {
        // Given
        val book = createTestBook("book-1", "Test Book")
        val shelf = createTestShelf("shelf-1", "Test Shelf", 0)
        dao.upsert(book)
        dao.upsertShelf(shelf)
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", System.currentTimeMillis()))

        // When
        val isInShelf = dao.isBookInAnyShelf("book-1").first()

        // Then
        assertTrue("Book should be in a shelf", isInShelf)
    }

    @Test
    fun `isBookInAnyShelf returns false when book is not in any shelf`() = runTest {
        // Given
        val book = createTestBook("book-1", "Test Book")
        dao.upsert(book)

        // When
        val isInShelf = dao.isBookInAnyShelf("book-1").first()

        // Then
        assertFalse("Book should not be in any shelf", isInShelf)
    }

    @Test
    fun `getShelvesForBook returns all shelves containing book`() = runTest {
        // Given
        val book = createTestBook("book-1", "Test Book")
        val shelf1 = createTestShelf("shelf-1", "Shelf 1", 0)
        val shelf2 = createTestShelf("shelf-2", "Shelf 2", 1)
        dao.upsert(book)
        dao.upsertShelf(shelf1)
        dao.upsertShelf(shelf2)
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", System.currentTimeMillis()))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-2", "book-1", System.currentTimeMillis()))

        // When
        val shelfIds = dao.getShelvesForBook("book-1").first()

        // Then
        assertEquals(2, shelfIds.size)
        assertTrue("Should contain shelf-1", shelfIds.contains("shelf-1"))
        assertTrue("Should contain shelf-2", shelfIds.contains("shelf-2"))
    }

    // Book Deletion Tests

    @Test
    fun `deleteBooksById removes book entities`() = runTest {
        dao.upsert(createTestBook("book-1", "Book 1"))
        dao.upsert(createTestBook("book-2", "Book 2"))
        dao.upsert(createTestBook("book-3", "Book 3"))

        dao.deleteBooksById(listOf("book-1", "book-3"))

        assertNull(dao.getBookById("book-1"))
        assertNotNull(dao.getBookById("book-2"))
        assertNull(dao.getBookById("book-3"))
    }

    @Test
    fun `deleteBooks transaction removes cross-refs and entities atomically`() = runTest {
        val shelf = createTestShelf("shelf-1", "My Shelf", 0)
        dao.upsertShelf(shelf)
        dao.upsert(createTestBook("book-1", "Book 1"))
        dao.upsert(createTestBook("book-2", "Book 2"))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", addedAt = 1L))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-2", addedAt = 2L))

        dao.deleteBooks(listOf("book-1"))

        assertNull("Book entity should be deleted", dao.getBookById("book-1"))
        assertNotNull("Unrelated book should remain", dao.getBookById("book-2"))
        val booksOnShelf = dao.getBooksForShelf("shelf-1").first()
        assertEquals("Only book-2 should remain on shelf", 1, booksOnShelf.size)
        assertEquals("book-2", booksOnShelf[0].id)
    }

    @Test
    fun `getBookIdsOnClubShelves returns only books on club shelves`() = runTest {
        val personalShelf = createTestShelf("shelf-personal", "Personal", 0)
        val clubShelf = BookshelfEntity(
            id = "shelf-club",
            name = "Book Club",
            shelfMaterial = "DARK_WOOD",
            position = 1,
            isBookClub = true
        )
        dao.upsertShelf(personalShelf)
        dao.upsertShelf(clubShelf)
        dao.upsert(createTestBook("personal-book", "Personal Book"))
        dao.upsert(createTestBook("club-book", "Club Book"))
        dao.upsert(createTestBook("both-book", "Both Book"))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-personal", "personal-book", addedAt = 1L))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-club", "club-book", addedAt = 2L))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-personal", "both-book", addedAt = 3L))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-club", "both-book", addedAt = 4L))

        val clubBookIds = dao.getBookIdsOnClubShelves().first().toSet()

        assertEquals(setOf("club-book", "both-book"), clubBookIds)
        assertFalse(
            "Personal-only book should not be in club list",
            clubBookIds.contains("personal-book")
        )
    }

    @Test
    fun `getBookIdsOnClubShelves returns empty when no club shelves exist`() = runTest {
        val shelf = createTestShelf("shelf-1", "Personal", 0)
        dao.upsertShelf(shelf)
        dao.upsert(createTestBook("book-1", "Book"))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", addedAt = 1L))

        val clubBookIds = dao.getBookIdsOnClubShelves().first()

        assertTrue("Should be empty with no club shelves", clubBookIds.isEmpty())
    }

    @Test
    fun `deleteAllCrossRefsForBooks removes only targeted cross-refs`() = runTest {
        val shelf = createTestShelf("shelf-1", "Shelf", 0)
        dao.upsertShelf(shelf)
        dao.upsert(createTestBook("book-1", "Book 1"))
        dao.upsert(createTestBook("book-2", "Book 2"))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-1", addedAt = 1L))
        dao.upsertCrossRef(BookshelfBookCrossRef("shelf-1", "book-2", addedAt = 2L))

        dao.deleteAllCrossRefsForBooks(listOf("book-1"))

        val count = dao.getBookCountForShelf("shelf-1").first()
        assertEquals("Only book-2 cross-ref should remain", 1, count)
    }

    // Helper methods

    private fun createTestBook(
        id: String,
        title: String,
        description: String = "Test description",
        purchased: Boolean = false
    ): BookEntity {
        return BookEntity(
            id = id,
            title = title,
            description = description,
            imageUrl = "https://example.com/cover.jpg",
            languages = listOf("en"),
            authors = listOf("Test Author"),
            firstPublishYear = "2020",
            numPagesMedian = 300,
            purchased = purchased,
            spineColor = -16711936
        )
    }

    private fun createTestShelf(
        id: String,
        name: String,
        position: Int
    ): BookshelfEntity {
        return BookshelfEntity(
            id = id,
            name = name,
            shelfMaterial = "DARK_WOOD",
            position = position
        )
    }
}
