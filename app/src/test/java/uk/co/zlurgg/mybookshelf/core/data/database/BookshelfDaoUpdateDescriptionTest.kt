package uk.co.zlurgg.mybookshelf.core.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity

/**
 * Integration tests for [BookshelfDao.updateDescription].
 *
 * REGRESSION INSURANCE — DO NOT DELETE.
 *
 * Several of the assertions below ("does not clobber personalNotes",
 * "does not clobber readingStatus", etc.) look tautological at first glance:
 * the SQL `UPDATE BookEntity SET description = ?` literally cannot affect
 * other columns by language semantics. The tests exist anyway because a
 * future "refactor" might replace the targeted UPDATE with an `upsertBook`-style
 * write — which DOES clobber the entire row — and the description-fetch flow
 * in BookDetailViewModel races against debounced personal-metadata writes
 * (notes, rating, reading status). The original 1.4 plan explicitly forbids
 * the upsert path here for exactly this reason. If you find yourself
 * "simplifying" `updateDescription` to call `upsert`, these assertions are
 * the alarm. Leave them in place.
 */
@RunWith(RobolectricTestRunner::class)
class BookshelfDaoUpdateDescriptionTest {

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

    @Test
    fun `updateDescription writes only the description column`() = runTest {
        // Given
        val original = BookEntity(
            id = "book-1",
            title = "Original Title",
            description = null,
            imageUrl = "https://example.com/cover.jpg",
            languages = listOf("en"),
            authors = listOf("Author"),
            firstPublishYear = "2020",
            numPagesMedian = 300,
            purchased = false,
            spineColor = -16711936,
        )
        dao.upsert(original)

        // When
        dao.updateDescription("book-1", "Freshly fetched description")

        // Then
        val updated = dao.getBookById("book-1")
        assertNotNull(updated)
        assertEquals("Freshly fetched description", updated?.description)
        // Other columns must remain unchanged
        assertEquals(original.title, updated?.title)
        assertEquals(original.imageUrl, updated?.imageUrl)
        assertEquals(original.authors, updated?.authors)
        assertEquals(original.languages, updated?.languages)
        assertEquals(original.purchased, updated?.purchased)
        assertEquals(original.spineColor, updated?.spineColor)
    }

    @Test
    fun `updateDescription does not clobber personal metadata`() = runTest {
        // Given — a book with ALL the user-personal columns populated (see the
        // top-of-class comment: this is the regression-insurance assertion).
        val withPersonalData = BookEntity(
            id = "book-1",
            title = "Title",
            description = "Old description",
            imageUrl = "https://example.com/cover.jpg",
            languages = listOf("en"),
            authors = listOf("Author"),
            firstPublishYear = "2020",
            numPagesMedian = 300,
            purchased = true,
            spineColor = -16711936,
            personalNotes = "These notes took the user 10 minutes to type",
            personalRating = 4.5f,
            readingStatus = "READING",
            dateAdded = 1_700_000_000L,
            purchaseDate = 1_700_100_000L,
        )
        dao.upsert(withPersonalData)

        // When
        dao.updateDescription("book-1", "New description from Google Books")

        // Then
        val updated = dao.getBookById("book-1")
        assertNotNull(updated)
        assertEquals("New description from Google Books", updated?.description)
        // Personal metadata columns MUST be untouched.
        assertEquals(
            "personalNotes must not be clobbered",
            "These notes took the user 10 minutes to type",
            updated?.personalNotes
        )
        assertEquals(
            "personalRating must not be clobbered",
            4.5f,
            updated?.personalRating
        )
        assertEquals(
            "readingStatus must not be clobbered",
            "READING",
            updated?.readingStatus
        )
        assertEquals(
            "dateAdded must not be clobbered",
            1_700_000_000L,
            updated?.dateAdded
        )
        assertEquals(
            "purchaseDate must not be clobbered",
            1_700_100_000L,
            updated?.purchaseDate
        )
    }

    @Test
    fun `updateDescription accepts null to clear description`() = runTest {
        // Given
        val withDescription = BookEntity(
            id = "book-1",
            title = "Title",
            description = "Existing description",
            imageUrl = "https://example.com/cover.jpg",
            languages = listOf("en"),
            authors = listOf("Author"),
            firstPublishYear = "2020",
            numPagesMedian = 300,
            purchased = false,
            spineColor = -16711936,
        )
        dao.upsert(withDescription)

        // When
        dao.updateDescription("book-1", null)

        // Then
        val updated = dao.getBookById("book-1")
        assertNotNull(updated)
        assertNull("Description should be null after explicit clear", updated?.description)
    }

    @Test
    fun `updateDescription is a no-op for unknown book id`() = runTest {
        // When
        dao.updateDescription("does-not-exist", "anything")

        // Then — no exception, no row created
        val missing = dao.getBookById("does-not-exist")
        assertNull(missing)
    }
}
