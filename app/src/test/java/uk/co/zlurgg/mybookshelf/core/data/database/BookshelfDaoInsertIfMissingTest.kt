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
 * Integration tests for [BookshelfDao.insertIfMissing].
 *
 * REGRESSION INSURANCE — DO NOT DELETE.
 *
 * `insertIfMissing` is the no-op-on-exists variant of [BookshelfDao.upsert].
 * The book-club sync paths
 * ([BookClubRepositoryHelper.downloadClubBooksToShelf],
 * [BookClubSyncRepositoryImpl.syncFromRemote]) write Firestore-sourced book
 * rows that carry NO personal metadata. If a user joins (or re-syncs) a club
 * shelf that shares a book they already own, an `upsert` would clobber the
 * user's `personalRating`/`personalNotes`/`readingStatus` columns. This
 * `INSERT OR IGNORE` shape preserves them.
 *
 * If you find yourself "simplifying" `insertIfMissing` back to `upsert` for any
 * reason, the `does not clobber personal metadata when row already exists`
 * assertion is the alarm. Leave it in place.
 */
@RunWith(RobolectricTestRunner::class)
class BookshelfDaoInsertIfMissingTest {

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
    fun `insertIfMissing creates the row when book does not exist`() = runTest {
        val newBook = BookEntity(
            id = "new-book",
            title = "Dune",
            description = "A spice opera",
            imageUrl = "https://example.com/dune.jpg",
            languages = listOf("en"),
            authors = listOf("Frank Herbert"),
            firstPublishYear = "1965",
            numPagesMedian = 412,
            purchased = false,
            spineColor = -16711936,
        )

        dao.insertIfMissing(newBook)

        val stored = dao.getBookById("new-book")
        assertNotNull(stored)
        assertEquals("Dune", stored?.title)
        assertEquals("Frank Herbert", stored?.authors?.firstOrNull())
    }

    @Test
    fun `insertIfMissing does not clobber personal metadata when row already exists`() = runTest {
        // Given — a personal-library book with the columns the user cares about
        val personalBook = BookEntity(
            id = "dune",
            title = "Dune",
            description = "Local description",
            imageUrl = "https://example.com/dune.jpg",
            languages = listOf("en"),
            authors = listOf("Frank Herbert"),
            firstPublishYear = "1965",
            numPagesMedian = 412,
            purchased = true,
            spineColor = -16711936,
            personalNotes = "Loved this on a beach in 2024",
            personalRating = 5.0f,
            readingStatus = "FINISHED",
            dateAdded = 1_700_000_000L,
            purchaseDate = 1_700_100_000L,
        )
        dao.upsert(personalBook)

        // When — the same id is encountered during a club sync, with a Firestore-
        // sourced row containing the default empty personal-metadata values
        val clubSyncedRow = BookEntity(
            id = "dune",
            title = "Dune",
            description = "Different description from Firestore",
            imageUrl = "https://example.com/dune-club.jpg",
            languages = listOf("en"),
            authors = listOf("Frank Herbert"),
            firstPublishYear = "1965",
            numPagesMedian = 412,
            purchased = false, // default
            spineColor = -16711936,
            // personalNotes, personalRating, readingStatus, dateAdded, purchaseDate
            // are all left at their defaults — this is what comes off the wire
        )
        dao.insertIfMissing(clubSyncedRow)

        // Then — the row stayed as the personal book; nothing got overwritten.
        val stored = dao.getBookById("dune")
        assertNotNull(stored)
        assertEquals(
            "personalNotes must survive a club-sync insert",
            "Loved this on a beach in 2024",
            stored?.personalNotes
        )
        assertEquals(
            "personalRating must survive a club-sync insert",
            5.0f,
            stored?.personalRating
        )
        assertEquals(
            "readingStatus must survive a club-sync insert",
            "FINISHED",
            stored?.readingStatus
        )
        assertEquals(
            "purchased flag must survive a club-sync insert",
            true,
            stored?.purchased
        )
        assertEquals(
            "dateAdded must survive a club-sync insert",
            1_700_000_000L,
            stored?.dateAdded
        )
        assertEquals(
            "purchaseDate must survive a club-sync insert",
            1_700_100_000L,
            stored?.purchaseDate
        )
        // Also: shareable fields don't get rewritten either — local row wins
        // on conflict, full stop.
        assertEquals(
            "Local description must NOT be replaced by the Firestore description",
            "Local description",
            stored?.description
        )
    }

    @Test
    fun `insertIfMissing on a row with no personal metadata writes through normally`() = runTest {
        // A book that exists only as a club-shelf row (no personal data either
        // way) shouldn't change behavior — first writer wins, second is a no-op.
        val firstWrite = BookEntity(
            id = "club-only",
            title = "Foundation",
            description = "First write",
            imageUrl = "https://example.com/foundation.jpg",
            languages = listOf("en"),
            authors = listOf("Isaac Asimov"),
            firstPublishYear = "1951",
            numPagesMedian = 244,
            purchased = false,
            spineColor = 0,
        )
        dao.insertIfMissing(firstWrite)

        val secondWrite = firstWrite.copy(description = "Second write")
        dao.insertIfMissing(secondWrite)

        val stored = dao.getBookById("club-only")
        assertEquals("First write", stored?.description)
    }

    @Test
    fun `insertIfMissing returns silently for an unrelated id pre-state`() = runTest {
        // Sanity check: inserting one id doesn't affect any other id.
        val alpha = BookEntity(
            id = "alpha",
            title = "Alpha",
            description = null,
            imageUrl = "",
            languages = emptyList(),
            authors = emptyList(),
            firstPublishYear = null,
            numPagesMedian = null,
            purchased = false,
            spineColor = 0,
        )
        dao.insertIfMissing(alpha)

        assertNotNull(dao.getBookById("alpha"))
        assertNull(dao.getBookById("beta"))
    }
}
