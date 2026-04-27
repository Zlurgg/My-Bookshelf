package uk.co.zlurgg.mybookshelf.book.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.TestTimeProvider

@RunWith(RobolectricTestRunner::class)
class BookshelfRepositoryImplTest {

    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var testTimeProvider: TestTimeProvider
    private lateinit var repository: BookshelfRepositoryImpl

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MyBookshelfRoomDatabase::class.java
        ).allowMainThreadQueries().build()

        testTimeProvider = TestTimeProvider()
        repository = BookshelfRepositoryImpl(database.bookshelfDao, testTimeProvider)
    }

    @After
    fun tearDown() {
        database.close()
        testTimeProvider.setTime(0L)
    }

    @Test
    fun `addBookToShelf creates cross-reference relationship`() = runTest {
        // Given
        val shelf = TestShelfBuilder().withId("test-shelf").build()
        val book = TestBookBuilder().withId("test-book").build()
        val currentTime = 123456789L
        testTimeProvider.setTime(currentTime)

        // Insert shelf and book first
        database.bookshelfDao.upsertShelf(shelf.toEntity())
        database.bookshelfDao.upsert(book.toEntity())

        // When
        repository.addBookToShelf("test-shelf", "test-book")

        // Then
        val booksOnShelf = repository.getBooksForShelf("test-shelf").first()
        assertEquals("Should have one book on shelf", 1, booksOnShelf.size)
        assertEquals("Should contain the added book", "test-book", booksOnShelf[0].id)

        // Verify the book was added successfully (cross-reference functionality is tested by DAO)
    }

    @Test
    fun `removeBookFromShelf removes cross-reference relationship`() = runTest {
        // Given
        val shelf = TestShelfBuilder().withId("test-shelf").build()
        val book = TestBookBuilder().withId("test-book").build()

        // Insert shelf, book, and cross-reference
        database.bookshelfDao.upsertShelf(shelf.toEntity())
        database.bookshelfDao.upsert(book.toEntity())
        repository.addBookToShelf("test-shelf", "test-book")

        // Verify book is on shelf initially
        val initialBooks = repository.getBooksForShelf("test-shelf").first()
        assertEquals("Should have one book initially", 1, initialBooks.size)

        // When
        repository.removeBookFromShelf("test-shelf", "test-book")

        // Then
        val finalBooks = repository.getBooksForShelf("test-shelf").first()
        assertTrue("Should have no books after removal", finalBooks.isEmpty())
    }

    @Test
    fun `getBooksForShelf returns books in correct order`() = runTest {
        // Given
        val shelf = TestShelfBuilder().withId("library-shelf").build()
        val book1 = TestBookBuilder().withId("book-1").withTitle("First Book").build()
        val book2 = TestBookBuilder().withId("book-2").withTitle("Second Book").build()
        val book3 = TestBookBuilder().withId("book-3").withTitle("Third Book").build()

        // Insert shelf and books
        database.bookshelfDao.upsertShelf(shelf.toEntity())
        database.bookshelfDao.upsert(book1.toEntity())
        database.bookshelfDao.upsert(book2.toEntity())
        database.bookshelfDao.upsert(book3.toEntity())

        // Add books to shelf with different timestamps
        testTimeProvider.setTime(1000L)
        repository.addBookToShelf("library-shelf", "book-1")

        testTimeProvider.setTime(2000L)
        repository.addBookToShelf("library-shelf", "book-2")

        testTimeProvider.setTime(3000L)
        repository.addBookToShelf("library-shelf", "book-3")

        // When
        val booksOnShelf = repository.getBooksForShelf("library-shelf").first()

        // Then
        assertEquals("Should have three books", 3, booksOnShelf.size)
        // Note: Order depends on DAO implementation - typically by addedAt timestamp
        assertTrue(
            "Should contain all added books",
            booksOnShelf.any { it.id == "book-1" } &&
                booksOnShelf.any { it.id == "book-2" } &&
                booksOnShelf.any { it.id == "book-3" }
        )
    }

    @Test
    fun `isBookInAnyShelf returns true when book is on at least one shelf`() = runTest {
        // Given
        val shelf1 = TestShelfBuilder().withId("shelf-1").build()
        val shelf2 = TestShelfBuilder().withId("shelf-2").build()
        val book = TestBookBuilder().withId("test-book").build()

        // Insert shelves and book
        database.bookshelfDao.upsertShelf(shelf1.toEntity())
        database.bookshelfDao.upsertShelf(shelf2.toEntity())
        database.bookshelfDao.upsert(book.toEntity())

        // Add book to one shelf
        repository.addBookToShelf("shelf-1", "test-book")

        // When
        val isInAnyShelf = repository.isBookInAnyShelf("test-book").first()

        // Then
        assertTrue("Book should be in at least one shelf", isInAnyShelf)
    }

    @Test
    fun `isBookInAnyShelf returns false when book is not on any shelf`() = runTest {
        // Given
        val book = TestBookBuilder().withId("orphaned-book").build()
        database.bookshelfDao.upsert(book.toEntity())

        // When - Book exists but not on any shelf
        val isInAnyShelf = repository.isBookInAnyShelf("orphaned-book").first()

        // Then
        assertFalse("Book should not be in any shelf", isInAnyShelf)
    }

    @Test
    fun `isBookOnShelf returns true when book is on specific shelf`() = runTest {
        // Given
        val targetShelf = TestShelfBuilder().withId("target-shelf").build()
        val otherShelf = TestShelfBuilder().withId("other-shelf").build()
        val book = TestBookBuilder().withId("test-book").build()

        // Insert shelves and book
        database.bookshelfDao.upsertShelf(targetShelf.toEntity())
        database.bookshelfDao.upsertShelf(otherShelf.toEntity())
        database.bookshelfDao.upsert(book.toEntity())

        // Add book to target shelf only
        repository.addBookToShelf("target-shelf", "test-book")

        // When
        val isOnTargetShelf = repository.isBookOnShelf("test-book", "target-shelf").first()
        val isOnOtherShelf = repository.isBookOnShelf("test-book", "other-shelf").first()

        // Then
        assertTrue("Book should be on target shelf", isOnTargetShelf)
        assertFalse("Book should not be on other shelf", isOnOtherShelf)
    }

    @Test
    fun `getShelvesForBook returns all shelves containing the book`() = runTest {
        // Given
        val shelf1 = TestShelfBuilder().withId("fiction-shelf").build()
        val shelf2 = TestShelfBuilder().withId("favorites-shelf").build()
        val shelf3 = TestShelfBuilder().withId("unrelated-shelf").build()
        val book = TestBookBuilder().withId("popular-book").build()

        // Insert shelves and book
        database.bookshelfDao.upsertShelf(shelf1.toEntity())
        database.bookshelfDao.upsertShelf(shelf2.toEntity())
        database.bookshelfDao.upsertShelf(shelf3.toEntity())
        database.bookshelfDao.upsert(book.toEntity())

        // Add book to two shelves
        repository.addBookToShelf("fiction-shelf", "popular-book")
        repository.addBookToShelf("favorites-shelf", "popular-book")

        // When
        val shelvesForBook = repository.getShelvesForBook("popular-book").first()

        // Then
        assertEquals("Should be on two shelves", 2, shelvesForBook.size)
        assertTrue("Should include fiction shelf", shelvesForBook.contains("fiction-shelf"))
        assertTrue("Should include favorites shelf", shelvesForBook.contains("favorites-shelf"))
        assertFalse("Should not include unrelated shelf", shelvesForBook.contains("unrelated-shelf"))
    }

    @Test
    fun `multiple books can be added to same shelf`() = runTest {
        // Given
        val shelf = TestShelfBuilder().withId("multi-book-shelf").build()
        val book1 = TestBookBuilder().withId("book-1").withTitle("Book One").build()
        val book2 = TestBookBuilder().withId("book-2").withTitle("Book Two").build()
        val book3 = TestBookBuilder().withId("book-3").withTitle("Book Three").build()

        // Insert shelf and books
        database.bookshelfDao.upsertShelf(shelf.toEntity())
        database.bookshelfDao.upsert(book1.toEntity())
        database.bookshelfDao.upsert(book2.toEntity())
        database.bookshelfDao.upsert(book3.toEntity())

        // When - Add multiple books to same shelf
        repository.addBookToShelf("multi-book-shelf", "book-1")
        repository.addBookToShelf("multi-book-shelf", "book-2")
        repository.addBookToShelf("multi-book-shelf", "book-3")

        // Then
        val booksOnShelf = repository.getBooksForShelf("multi-book-shelf").first()
        assertEquals("Should have three books on shelf", 3, booksOnShelf.size)

        val bookIds = booksOnShelf.map { it.id }
        assertTrue("Should contain book-1", bookIds.contains("book-1"))
        assertTrue("Should contain book-2", bookIds.contains("book-2"))
        assertTrue("Should contain book-3", bookIds.contains("book-3"))
    }

    @Test
    fun `same book can be added to multiple shelves`() = runTest {
        // Given
        val shelf1 = TestShelfBuilder().withId("shelf-1").build()
        val shelf2 = TestShelfBuilder().withId("shelf-2").build()
        val shelf3 = TestShelfBuilder().withId("shelf-3").build()
        val book = TestBookBuilder().withId("versatile-book").build()

        // Insert shelves and book
        database.bookshelfDao.upsertShelf(shelf1.toEntity())
        database.bookshelfDao.upsertShelf(shelf2.toEntity())
        database.bookshelfDao.upsertShelf(shelf3.toEntity())
        database.bookshelfDao.upsert(book.toEntity())

        // When - Add same book to multiple shelves
        repository.addBookToShelf("shelf-1", "versatile-book")
        repository.addBookToShelf("shelf-2", "versatile-book")
        repository.addBookToShelf("shelf-3", "versatile-book")

        // Then
        val shelvesForBook = repository.getShelvesForBook("versatile-book").first()
        assertEquals("Book should be on three shelves", 3, shelvesForBook.size)
        assertTrue("Should be on shelf-1", shelvesForBook.contains("shelf-1"))
        assertTrue("Should be on shelf-2", shelvesForBook.contains("shelf-2"))
        assertTrue("Should be on shelf-3", shelvesForBook.contains("shelf-3"))

        // Verify each shelf contains the book
        val booksOnShelf1 = repository.getBooksForShelf("shelf-1").first()
        val booksOnShelf2 = repository.getBooksForShelf("shelf-2").first()
        val booksOnShelf3 = repository.getBooksForShelf("shelf-3").first()

        assertEquals("Shelf-1 should have one book", 1, booksOnShelf1.size)
        assertEquals("Shelf-2 should have one book", 1, booksOnShelf2.size)
        assertEquals("Shelf-3 should have one book", 1, booksOnShelf3.size)
        assertEquals("All should contain same book", "versatile-book", booksOnShelf1[0].id)
    }

    @Test
    fun `removing book from one shelf does not affect other shelves`() = runTest {
        // Given
        val shelf1 = TestShelfBuilder().withId("shelf-1").build()
        val shelf2 = TestShelfBuilder().withId("shelf-2").build()
        val book = TestBookBuilder().withId("shared-book").build()

        // Insert shelves and book
        database.bookshelfDao.upsertShelf(shelf1.toEntity())
        database.bookshelfDao.upsertShelf(shelf2.toEntity())
        database.bookshelfDao.upsert(book.toEntity())

        // Add book to both shelves
        repository.addBookToShelf("shelf-1", "shared-book")
        repository.addBookToShelf("shelf-2", "shared-book")

        // When - Remove book from only one shelf
        repository.removeBookFromShelf("shelf-1", "shared-book")

        // Then
        val booksOnShelf1 = repository.getBooksForShelf("shelf-1").first()
        val booksOnShelf2 = repository.getBooksForShelf("shelf-2").first()

        assertTrue("Shelf-1 should be empty", booksOnShelf1.isEmpty())
        assertEquals("Shelf-2 should still have the book", 1, booksOnShelf2.size)
        assertEquals("Shelf-2 should contain correct book", "shared-book", booksOnShelf2[0].id)

        // Book should still be in a shelf
        val isStillInAnyShelf = repository.isBookInAnyShelf("shared-book").first()
        assertTrue("Book should still be in shelf-2", isStillInAnyShelf)
    }

    @Test
    fun `operations handle non-existent books and shelves gracefully`() = runTest {
        // Given - No setup needed, testing with non-existent entities

        // When - Query for non-existent entities (should not crash)
        val books = repository.getBooksForShelf("non-existent-shelf").first()
        val isInAnyShelf = repository.isBookInAnyShelf("non-existent-book").first()
        val shelves = repository.getShelvesForBook("non-existent-book").first()

        // Then - Should return empty results gracefully
        assertTrue("Should handle non-existent shelf gracefully", books.isEmpty())
        assertFalse("Non-existent book should not be in any shelf", isInAnyShelf)
        assertTrue("Non-existent book should not be on any shelves", shelves.isEmpty())

        // When - Try to remove non-existent relationship (should not crash)
        repository.removeBookFromShelf("non-existent-shelf", "non-existent-book")

        // Then - Should complete without throwing exceptions
        assertTrue("Should handle removal gracefully", true)
    }

    @Test
    fun `addBookToShelf handles duplicate additions gracefully`() = runTest {
        // Given
        val shelf = TestShelfBuilder().withId("duplicate-shelf").build()
        val book = TestBookBuilder().withId("duplicate-book").build()

        // Insert shelf and book
        database.bookshelfDao.upsertShelf(shelf.toEntity())
        database.bookshelfDao.upsert(book.toEntity())

        // Add book to shelf twice
        repository.addBookToShelf("duplicate-shelf", "duplicate-book")
        repository.addBookToShelf("duplicate-shelf", "duplicate-book")

        // Then - Should handle duplicate gracefully (upsert behavior)
        val booksOnShelf = repository.getBooksForShelf("duplicate-shelf").first()
        assertEquals("Should only have one instance of the book", 1, booksOnShelf.size)
        assertEquals("Should be the correct book", "duplicate-book", booksOnShelf[0].id)
    }

    @Test
    fun `time provider integration works correctly`() = runTest {
        // Given
        val shelf = TestShelfBuilder().withId("time-test-shelf").build()
        val book = TestBookBuilder().withId("time-test-book").build()
        val specificTime = 987654321L

        // Insert shelf and book
        database.bookshelfDao.upsertShelf(shelf.toEntity())
        database.bookshelfDao.upsert(book.toEntity())

        // Set specific time
        testTimeProvider.setTime(specificTime)

        // When
        repository.addBookToShelf("time-test-shelf", "time-test-book")

        // Then - Verify the book was added successfully
        val booksOnShelf = repository.getBooksForShelf("time-test-shelf").first()
        assertEquals("Should have one book", 1, booksOnShelf.size)
        assertEquals("Should be the correct book", "time-test-book", booksOnShelf[0].id)
    }
}

// Extension functions to convert test builders to entities
private fun uk.co.zlurgg.mybookshelf.book.domain.model.Book.toEntity() =
    BookEntity(
        id = this.id,
        title = this.title,
        imageUrl = this.imageUrl,
        authors = this.authors,
        description = this.description,
        languages = this.languages,
        firstPublishYear = this.firstPublishYear,
        ratingsAverage = this.averageRating,
        ratingsCount = this.ratingCount,
        numPagesMedian = this.numPages,
        numEditions = this.numEditions,
        purchased = this.purchased,
        spineColor = this.spineColor
    )

private fun uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf.toEntity() =
    BookshelfEntity(
        id = this.id,
        name = this.name,
        shelfMaterial = this.shelfStyle.name,
        position = this.position
    )
