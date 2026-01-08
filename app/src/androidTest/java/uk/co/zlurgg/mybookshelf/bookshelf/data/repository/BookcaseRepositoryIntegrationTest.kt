package uk.co.zlurgg.mybookshelf.bookshelf.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * Integration test for BookcaseRepository with real Room database.
 * Tests bookshelf management (create, update, delete, reorder) with actual SQLite.
 *
 * This is a medium-scope test (Google's 20% integration test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class BookcaseRepositoryIntegrationTest {
    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var repository: BookcaseRepositoryImpl

    // Stub CurrentUserProvider - returns null (guest mode)
    private val stubCurrentUserProvider =
        object : CurrentUserProvider {
            override fun getCurrentUserId(): String? = null
        }

    // Stub TimeProvider - returns fixed timestamp
    private val stubTimeProvider =
        object : TimeProvider {
            override fun currentTimeMillis(): Long = System.currentTimeMillis()
        }

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                MyBookshelfRoomDatabase::class.java,
            ).build()

        repository = BookcaseRepositoryImpl(database.bookshelfDao, stubCurrentUserProvider, stubTimeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addShelfPersistsToRealDatabase() =
        runTest {
            // Given - New shelf to add
            val shelf =
                Bookshelf(
                    id = "shelf-1",
                    name = "Fiction",
                    books = emptyList(),
                    shelfStyle = ShelfStyle.DarkWood,
                    position = 0,
                )

            // When - Add shelf through repository
            repository.addShelf(shelf)

            // Then - Shelf should be retrievable
            val retrieved = repository.getShelfById("shelf-1")
            assertEquals("shelf-1", retrieved?.id)
            assertEquals("Fiction", retrieved?.name)
            assertEquals(ShelfStyle.DarkWood, retrieved?.shelfStyle)
            assertEquals(0, retrieved?.position)
        }

    @Test
    fun updateShelfPersistsChanges() =
        runTest {
            // Given - Shelf exists in database
            val originalShelf =
                Bookshelf(
                    id = "shelf-1",
                    name = "Original Name",
                    books = emptyList(),
                    shelfStyle = ShelfStyle.DarkWood,
                    position = 0,
                )
            repository.addShelf(originalShelf)

            // When - Update shelf
            val updatedShelf = originalShelf.copy(name = "Updated Name", position = 5)
            repository.updateShelf(updatedShelf)

            // Then - Changes should persist
            val retrieved = repository.getShelfById("shelf-1")
            assertEquals("Updated Name", retrieved?.name)
            assertEquals(5, retrieved?.position)
        }

    @Test
    fun removeShelfDeletesFromDatabase() =
        runTest {
            // Given - Shelf exists
            val shelf =
                Bookshelf(
                    id = "shelf-1",
                    name = "To Delete",
                    books = emptyList(),
                    shelfStyle = ShelfStyle.DarkWood,
                    position = 0,
                )
            repository.addShelf(shelf)

            // When - Remove shelf
            repository.removeShelf("shelf-1")

            // Then - Shelf should not be visible in user-facing queries
            // Note: removeShelf soft-deletes (marks as DELETED) for sync purposes
            // The shelf still exists in DB but is filtered from getAllShelves()
            val allShelves = repository.getAllShelves().first()
            assertTrue("Shelf should not be visible after removal", allShelves.none { it.id == "shelf-1" })
        }

    @Test
    fun getAllShelvesReturnsOrderedByPosition() =
        runTest {
            // Given - Multiple shelves with different positions
            val shelf3 = Bookshelf("shelf-3", "Third", emptyList(), ShelfStyle.DarkWood, 2)
            val shelf1 = Bookshelf("shelf-1", "First", emptyList(), ShelfStyle.DarkWood, 0)
            val shelf2 = Bookshelf("shelf-2", "Second", emptyList(), ShelfStyle.DarkWood, 1)

            repository.addShelf(shelf3)
            repository.addShelf(shelf1)
            repository.addShelf(shelf2)

            // When - Get all shelves
            val shelves = repository.getAllShelves().first()

            // Then - Should be ordered by position
            assertEquals(3, shelves.size)
            assertEquals("First", shelves[0].name)
            assertEquals("Second", shelves[1].name)
            assertEquals("Third", shelves[2].name)
        }

    @Test
    fun getBookCountForShelfReturnsCorrectCount() =
        runTest {
            // Given - Shelf with books
            val shelfId = "shelf-1"
            val shelf = Bookshelf(shelfId, "Test Shelf", emptyList(), ShelfStyle.DarkWood, 0)
            repository.addShelf(shelf)

            // Add books
            val book1 = createTestBookEntity("book-1", "Book 1")
            val book2 = createTestBookEntity("book-2", "Book 2")
            val book3 = createTestBookEntity("book-3", "Book 3")

            listOf(book1, book2, book3).forEach { book ->
                database.bookshelfDao.upsert(book)
                database.bookshelfDao.upsertCrossRef(
                    BookshelfBookCrossRef(
                        shelfId = shelfId,
                        bookId = book.id,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
            }

            // When - Get book count
            val count = repository.getBookCountForShelf(shelfId).first()

            // Then - Should return 3
            assertEquals(3, count)
        }

    @Test
    fun removeShelfCascadesAndRemovesCrossRefs() =
        runTest {
            // Given - Shelf with books
            val shelfId = "shelf-1"
            val bookId = "book-1"

            // Add shelf
            val shelf = Bookshelf(shelfId, "Test Shelf", emptyList(), ShelfStyle.DarkWood, 0)
            repository.addShelf(shelf)

            // Add book and cross-reference directly via DAO
            database.bookshelfDao.upsert(createTestBookEntity(bookId, "Test Book"))
            database.bookshelfDao.upsertCrossRef(
                BookshelfBookCrossRef(
                    shelfId = shelfId,
                    bookId = bookId,
                    addedAt = System.currentTimeMillis(),
                ),
            )

            // When - Remove shelf
            repository.removeShelf(shelfId)

            // Then - Cross-reference should be deleted
            val booksInShelf = database.bookshelfDao.getBooksForShelf(shelfId).first()
            assertEquals("Cross-references should be deleted with shelf", 0, booksInShelf.size)
        }

    @Test
    fun shelfPositionsPersistAcrossRepositoryRecreation() =
        runTest {
            // Given - Shelves with specific positions
            repository.addShelf(Bookshelf("shelf-1", "First", emptyList(), ShelfStyle.DarkWood, 0))
            repository.addShelf(Bookshelf("shelf-2", "Second", emptyList(), ShelfStyle.DarkWood, 1))

            // When - Create new repository instance (simulating app restart)
            val newRepository = BookcaseRepositoryImpl(database.bookshelfDao, stubCurrentUserProvider, stubTimeProvider)
            val shelves = newRepository.getAllShelves().first()

            // Then - Positions should persist
            assertEquals(2, shelves.size)
            assertEquals(0, shelves[0].position)
            assertEquals(1, shelves[1].position)
        }

    private fun createTestBookEntity(
        id: String,
        title: String,
    ): BookEntity {
        return BookEntity(
            id = id,
            title = title,
            authors = listOf("Test Author"),
            imageUrl = "https://example.com/cover.jpg",
            description = "Test description",
            languages = listOf("en"),
            firstPublishYear = "2024",
            ratingsAverage = 4.5,
            ratingsCount = 100,
            numPagesMedian = 300,
            numEditions = 5,
            purchased = false,
            spineColor = 0xFF8B4513.toInt(),
        )
    }
}
