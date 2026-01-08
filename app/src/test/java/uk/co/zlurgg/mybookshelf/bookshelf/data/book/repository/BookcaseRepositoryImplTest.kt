package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.TestTimeProvider

@RunWith(RobolectricTestRunner::class)
class BookcaseRepositoryImplTest {
    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var repository: BookcaseRepositoryImpl
    private val testTimeProvider = TestTimeProvider(currentTime = 1234567890L)

    // Mock that returns null (guest mode) so all orphan data is visible
    private val mockCurrentUserProvider =
        object : CurrentUserProvider {
            override fun getCurrentUserId(): String? = null
        }

    @Before
    fun setup() {
        // Create in-memory database for testing
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                MyBookshelfRoomDatabase::class.java,
            ).allowMainThreadQueries().build()

        repository = BookcaseRepositoryImpl(database.bookshelfDao, mockCurrentUserProvider, testTimeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getAllShelves returns empty list when no shelves exist`() =
        runTest {
            // When
            val shelves = repository.getAllShelves().first()

            // Then
            assertTrue("Should return empty list when no shelves exist", shelves.isEmpty())
        }

    @Test
    fun `addShelf creates new shelf in database`() =
        runTest {
            // Given
            val shelf =
                TestShelfBuilder()
                    .withId("new-shelf")
                    .withName("Test Shelf")
                    .withStyle(ShelfStyle.DarkWood)
                    .withPosition(0)
                    .build()

            // When
            repository.addShelf(shelf)

            // Then
            val allShelves = repository.getAllShelves().first()
            assertEquals("Should have one shelf", 1, allShelves.size)
            assertEquals("Should have correct ID", "new-shelf", allShelves[0].id)
            assertEquals("Should have correct name", "Test Shelf", allShelves[0].name)
            assertEquals("Should have correct style", ShelfStyle.DarkWood, allShelves[0].shelfStyle)
            assertEquals("Should have correct position", 0, allShelves[0].position)
        }

    @Test
    fun `getAllShelves returns shelves in position order`() =
        runTest {
            // Given
            val shelf1 = TestShelfBuilder().withId("shelf-1").withName("Third").withPosition(2).build()
            val shelf2 = TestShelfBuilder().withId("shelf-2").withName("First").withPosition(0).build()
            val shelf3 = TestShelfBuilder().withId("shelf-3").withName("Second").withPosition(1).build()

            // Insert in random order
            repository.addShelf(shelf1)
            repository.addShelf(shelf2)
            repository.addShelf(shelf3)

            // When
            val allShelves = repository.getAllShelves().first()

            // Then
            assertEquals("Should have three shelves", 3, allShelves.size)
            assertEquals("First position should be shelf-2", "shelf-2", allShelves[0].id)
            assertEquals("Second position should be shelf-3", "shelf-3", allShelves[1].id)
            assertEquals("Third position should be shelf-1", "shelf-1", allShelves[2].id)
            assertEquals("Names should be in position order", "First", allShelves[0].name)
            assertEquals("Names should be in position order", "Second", allShelves[1].name)
            assertEquals("Names should be in position order", "Third", allShelves[2].name)
        }

    @Test
    fun `getShelfById returns correct shelf when it exists`() =
        runTest {
            // Given
            val shelf =
                TestShelfBuilder()
                    .withId("test-shelf-123")
                    .withName("Test Shelf Name")
                    .withStyle(ShelfStyle.SilverMetal)
                    .withPosition(5)
                    .build()

            repository.addShelf(shelf)

            // When
            val retrievedShelf = repository.getShelfById("test-shelf-123")

            // Then
            assertEquals("Should return correct shelf ID", "test-shelf-123", retrievedShelf?.id)
            assertEquals("Should return correct shelf name", "Test Shelf Name", retrievedShelf?.name)
            assertEquals("Should return correct shelf style", ShelfStyle.SilverMetal, retrievedShelf?.shelfStyle)
            assertEquals("Should return correct shelf position", 5, retrievedShelf?.position)
        }

    @Test
    fun `getShelfById returns null when shelf does not exist`() =
        runTest {
            // When
            val retrievedShelf = repository.getShelfById("non-existent-shelf")

            // Then
            assertNull("Should return null for non-existent shelf", retrievedShelf)
        }

    @Test
    fun `updateShelf modifies existing shelf data`() =
        runTest {
            // Given
            val originalShelf =
                TestShelfBuilder()
                    .withId("update-shelf")
                    .withName("Original Name")
                    .withStyle(ShelfStyle.DarkWood)
                    .withPosition(0)
                    .build()

            repository.addShelf(originalShelf)

            val updatedShelf =
                originalShelf.copy(
                    name = "Updated Name",
                    shelfStyle = ShelfStyle.SilverMetal,
                    position = 3,
                )

            // When
            repository.updateShelf(updatedShelf)

            // Then
            val retrievedShelf = repository.getShelfById("update-shelf")!!
            assertEquals("Should preserve ID", "update-shelf", retrievedShelf.id)
            assertEquals("Should update name", "Updated Name", retrievedShelf.name)
            assertEquals("Should update style", ShelfStyle.SilverMetal, retrievedShelf.shelfStyle)
            assertEquals("Should update position", 3, retrievedShelf.position)
        }

    @Test
    fun `updateShelf works as upsert for non-existent shelf`() =
        runTest {
            // Given
            val newShelf =
                TestShelfBuilder()
                    .withId("upsert-shelf")
                    .withName("Upserted Shelf")
                    .withStyle(ShelfStyle.WhiteMetal)
                    .build()

            // When - Update non-existent shelf (should create it)
            repository.updateShelf(newShelf)

            // Then
            val retrievedShelf = repository.getShelfById("upsert-shelf")
            assertEquals("Should create new shelf with correct ID", "upsert-shelf", retrievedShelf?.id)
            assertEquals("Should create new shelf with correct name", "Upserted Shelf", retrievedShelf?.name)
            assertEquals(
                "Should create new shelf with correct style",
                ShelfStyle.WhiteMetal,
                retrievedShelf?.shelfStyle,
            )
        }

    @Test
    fun `removeShelf soft deletes shelf from database`() =
        runTest {
            // Given
            val shelf =
                TestShelfBuilder()
                    .withId("delete-shelf")
                    .withName("Shelf to Delete")
                    .build()

            repository.addShelf(shelf)

            // Verify shelf exists
            val beforeDeletion = repository.getShelfById("delete-shelf")
            assertEquals("Shelf should exist before deletion", "delete-shelf", beforeDeletion?.id)

            // When
            repository.removeShelf("delete-shelf")

            // Then - Soft delete: shelf still exists but not visible to users
            val allShelves = repository.getAllShelves().first()
            assertTrue("Should have no visible shelves after soft deletion", allShelves.isEmpty())

            // Shelf still exists in database for sync (soft delete)
            val softDeletedEntity = database.bookshelfDao.getShelfById("delete-shelf")
            assertNotNull("Soft deleted shelf should still exist in database", softDeletedEntity)
            assertEquals("Shelf should be marked as DELETED", "DELETED", softDeletedEntity?.syncStatus)
        }

    @Test
    fun `removeShelf handles non-existent shelf gracefully`() =
        runTest {
            // When - Should not throw exception
            repository.removeShelf("non-existent-shelf")

            // Then - Should complete successfully
            val allShelves = repository.getAllShelves().first()
            assertTrue("Should remain empty after deleting non-existent shelf", allShelves.isEmpty())
        }

    @Test
    fun `getBookCountForShelf returns zero for empty shelf`() =
        runTest {
            // Given
            val shelf = TestShelfBuilder().withId("empty-shelf").build()
            repository.addShelf(shelf)

            // When
            val bookCount = repository.getBookCountForShelf("empty-shelf").first()

            // Then
            assertEquals("Empty shelf should have zero books", 0, bookCount)
        }

    @Test
    fun `getBookCountForShelf returns correct count after adding books`() =
        runTest {
            // Given
            val shelf = TestShelfBuilder().withId("book-count-shelf").build()
            val book1 = TestBookBuilder().withId("book-1").build()
            val book2 = TestBookBuilder().withId("book-2").build()
            val book3 = TestBookBuilder().withId("book-3").build()

            // Insert shelf and books
            repository.addShelf(shelf)
            database.bookshelfDao.upsert(book1.toEntity())
            database.bookshelfDao.upsert(book2.toEntity())
            database.bookshelfDao.upsert(book3.toEntity())

            // Add books to shelf via cross-references
            database.bookshelfDao.upsertCrossRef(
                BookshelfBookCrossRef(
                    shelfId = "book-count-shelf",
                    bookId = "book-1",
                    addedAt = System.currentTimeMillis(),
                ),
            )
            database.bookshelfDao.upsertCrossRef(
                BookshelfBookCrossRef(
                    shelfId = "book-count-shelf",
                    bookId = "book-2",
                    addedAt = System.currentTimeMillis(),
                ),
            )

            // When
            val bookCount = repository.getBookCountForShelf("book-count-shelf").first()

            // Then
            assertEquals("Shelf should have two books", 2, bookCount)
        }

    @Test
    fun `getBookCountForShelf returns zero for non-existent shelf`() =
        runTest {
            // When
            val bookCount = repository.getBookCountForShelf("non-existent-shelf").first()

            // Then
            assertEquals("Non-existent shelf should have zero books", 0, bookCount)
        }

    @Test
    fun `removeShelf cleans up cross-references`() =
        runTest {
            // Given
            val shelf = TestShelfBuilder().withId("cleanup-shelf").build()
            val book = TestBookBuilder().withId("cleanup-book").build()

            // Insert shelf and book
            repository.addShelf(shelf)
            database.bookshelfDao.upsert(book.toEntity())

            // Add book to shelf
            database.bookshelfDao.upsertCrossRef(
                BookshelfBookCrossRef(
                    shelfId = "cleanup-shelf",
                    bookId = "cleanup-book",
                    addedAt = System.currentTimeMillis(),
                ),
            )

            // Verify book is on shelf
            val initialCount = repository.getBookCountForShelf("cleanup-shelf").first()
            assertEquals("Should have one book initially", 1, initialCount)

            // When
            repository.removeShelf("cleanup-shelf")

            // Then
            val finalCount = repository.getBookCountForShelf("cleanup-shelf").first()
            assertEquals("Should have no books after shelf deletion", 0, finalCount)

            // Book should still exist in database but not be on any shelf
            val retrievedBook = database.bookshelfDao.getBookById("cleanup-book")
            assertEquals("Book should still exist", "cleanup-book", retrievedBook?.id)
        }

    @Test
    fun `multiple shelf operations work correctly`() =
        runTest {
            // Given
            val shelf1 = TestShelfBuilder().withId("multi-1").withName("First").withPosition(0).build()
            val shelf2 = TestShelfBuilder().withId("multi-2").withName("Second").withPosition(1).build()
            val shelf3 = TestShelfBuilder().withId("multi-3").withName("Third").withPosition(2).build()

            // When - Add multiple shelves
            repository.addShelf(shelf1)
            repository.addShelf(shelf2)
            repository.addShelf(shelf3)

            // Update middle shelf
            val updatedShelf2 = shelf2.copy(name = "Updated Second")
            repository.updateShelf(updatedShelf2)

            // Remove first shelf (soft delete)
            repository.removeShelf("multi-1")

            // Then
            val allShelves = repository.getAllShelves().first()
            assertEquals("Should have two visible shelves remaining", 2, allShelves.size)

            val remainingShelf1 = repository.getShelfById("multi-2")
            val remainingShelf2 = repository.getShelfById("multi-3")

            assertEquals("Should update shelf name", "Updated Second", remainingShelf1?.name)
            assertEquals("Should preserve other shelf", "Third", remainingShelf2?.name)

            // Soft deleted shelf still exists in database but marked as DELETED
            val softDeletedEntity = database.bookshelfDao.getShelfById("multi-1")
            assertNotNull("Soft deleted shelf should still exist in database", softDeletedEntity)
            assertEquals("Shelf should be marked as DELETED", "DELETED", softDeletedEntity?.syncStatus)
        }

    @Test
    fun `different shelf styles are preserved correctly`() =
        runTest {
            // Given
            val darkWoodShelf =
                TestShelfBuilder()
                    .withId("dark-wood")
                    .withStyle(ShelfStyle.DarkWood)
                    .build()

            val silverMetalShelf =
                TestShelfBuilder()
                    .withId("silver-metal")
                    .withStyle(ShelfStyle.SilverMetal)
                    .build()

            val whiteMetalShelf =
                TestShelfBuilder()
                    .withId("white-metal")
                    .withStyle(ShelfStyle.WhiteMetal)
                    .build()

            // When
            repository.addShelf(darkWoodShelf)
            repository.addShelf(silverMetalShelf)
            repository.addShelf(whiteMetalShelf)

            // Then
            val retrievedDarkWood = repository.getShelfById("dark-wood")
            val retrievedSilverMetal = repository.getShelfById("silver-metal")
            val retrievedWhiteMetal = repository.getShelfById("white-metal")

            assertEquals("Should preserve DarkWood style", ShelfStyle.DarkWood, retrievedDarkWood?.shelfStyle)
            assertEquals("Should preserve SilverMetal style", ShelfStyle.SilverMetal, retrievedSilverMetal?.shelfStyle)
            assertEquals("Should preserve WhiteMetal style", ShelfStyle.WhiteMetal, retrievedWhiteMetal?.shelfStyle)
        }

    @Test
    fun `shelf positions can be updated correctly`() =
        runTest {
            // Given
            val shelf1 = TestShelfBuilder().withId("pos-1").withPosition(0).build()
            val shelf2 = TestShelfBuilder().withId("pos-2").withPosition(1).build()
            val shelf3 = TestShelfBuilder().withId("pos-3").withPosition(2).build()

            repository.addShelf(shelf1)
            repository.addShelf(shelf2)
            repository.addShelf(shelf3)

            // When - Update positions
            repository.updateShelf(shelf1.copy(position = 2))
            repository.updateShelf(shelf2.copy(position = 0))
            repository.updateShelf(shelf3.copy(position = 1))

            // Then
            val allShelves = repository.getAllShelves().first()
            assertEquals("Should maintain three shelves", 3, allShelves.size)

            // Check new order (DAO sorts by position)
            assertEquals("Position 0 should be pos-2", "pos-2", allShelves[0].id)
            assertEquals("Position 1 should be pos-3", "pos-3", allShelves[1].id)
            assertEquals("Position 2 should be pos-1", "pos-1", allShelves[2].id)
        }
}

// Extension function to convert test builder to entity
private fun uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book.toEntity() =
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
        spineColor = this.spineColor,
    )
