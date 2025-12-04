package uk.co.zlurgg.mybookshelf.bookshelf.data.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDatabase
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.migrations.MIGRATION_8_9

/**
 * Integration test for database migration from version 8 to 9.
 *
 * Tests that:
 * - New sync columns are added to BookEntity
 * - New sync columns are added to BookshelfEntity
 * - New sync columns are added to BookshelfBookCrossRef
 * - SyncMetadataEntity table is created
 * - Indexes are created for sync queries
 * - Existing data is preserved
 */
@RunWith(AndroidJUnit4::class)
class Migration8To9Test {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BookshelfDatabase::class.java
    )

    @Test
    fun migrate8To9_addsNewColumnsToBookEntity() {
        // Create database at version 8
        helper.createDatabase(testDbName, 8).use { db ->
            // Insert a book at version 8
            val values = ContentValues().apply {
                put("id", "book-1")
                put("title", "Test Book")
                put("description", "Description")
                put("imageUrl", "https://example.com/cover.jpg")
                put("languages", "[\"en\"]")
                put("authors", "[\"Author\"]")
                put("firstPublishYear", "2020")
                put("ratingsAverage", 4.5)
                put("ratingsCount", 100)
                put("numPagesMedian", 300)
                put("numEditions", 5)
                put("purchased", 0)
                put("spineColor", -16711936)
                put("readingStatus", "WANT_TO_READ")
                put("personalRating", 0.0f)
                put("personalNotes", "")
            }
            db.insert("BookEntity", SQLiteDatabase.CONFLICT_REPLACE, values)
        }

        // Run migration
        helper.runMigrationsAndValidate(testDbName, 9, true, MIGRATION_8_9).use { db ->
            // Verify new columns exist with correct defaults
            db.query("SELECT * FROM BookEntity WHERE id = 'book-1'").use { cursor ->
                assertTrue("Should have result", cursor.moveToFirst())

                // Original data preserved
                assertEquals("Test Book", cursor.getString(cursor.getColumnIndexOrThrow("title")))

                // New columns have defaults
                assertNull("ownerId should be null", cursor.getString(cursor.getColumnIndexOrThrow("ownerId")))
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("lastModifiedAt")))
                assertEquals("PENDING", cursor.getString(cursor.getColumnIndexOrThrow("syncStatus")))
                assertNull("cloudId should be null", cursor.getString(cursor.getColumnIndexOrThrow("cloudId")))
                assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("version")))
            }
        }
    }

    @Test
    fun migrate8To9_addsNewColumnsToBookshelfEntity() {
        // Create database at version 8
        helper.createDatabase(testDbName, 8).use { db ->
            // Insert a shelf at version 8
            val values = ContentValues().apply {
                put("id", "shelf-1")
                put("name", "My Shelf")
                put("shelfMaterial", "DARK_WOOD")
                put("position", 0)
                put("isTidyMode", 0)
            }
            db.insert("BookshelfEntity", SQLiteDatabase.CONFLICT_REPLACE, values)
        }

        // Run migration
        helper.runMigrationsAndValidate(testDbName, 9, true, MIGRATION_8_9).use { db ->
            // Verify new columns exist with correct defaults
            db.query("SELECT * FROM BookshelfEntity WHERE id = 'shelf-1'").use { cursor ->
                assertTrue("Should have result", cursor.moveToFirst())

                // Original data preserved
                assertEquals("My Shelf", cursor.getString(cursor.getColumnIndexOrThrow("name")))

                // New sync columns have defaults
                assertNull("ownerId should be null", cursor.getString(cursor.getColumnIndexOrThrow("ownerId")))
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("lastModifiedAt")))
                assertEquals("PENDING", cursor.getString(cursor.getColumnIndexOrThrow("syncStatus")))
                assertNull("cloudId should be null", cursor.getString(cursor.getColumnIndexOrThrow("cloudId")))
                assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("version")))

                // New sharing columns have defaults
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isShared"))) // false
                assertNull("shareCode should be null", cursor.getString(cursor.getColumnIndexOrThrow("shareCode")))
            }
        }
    }

    @Test
    fun migrate8To9_addsNewColumnsToBookshelfBookCrossRef() {
        // Create database at version 8
        helper.createDatabase(testDbName, 8).use { db ->
            // Need to insert book and shelf first
            db.insert("BookEntity", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
                put("id", "book-1")
                put("title", "Test Book")
                put("imageUrl", "url")
                put("languages", "[]")
                put("authors", "[]")
                put("firstPublishYear", "2020")
                put("ratingsCount", 0)
                put("numPagesMedian", 0)
                put("numEditions", 0)
                put("purchased", 0)
                put("spineColor", 0)
                put("readingStatus", "WANT_TO_READ")
                put("personalRating", 0.0f)
                put("personalNotes", "")
            })
            db.insert("BookshelfEntity", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
                put("id", "shelf-1")
                put("name", "Shelf")
                put("shelfMaterial", "DARK_WOOD")
                put("position", 0)
                put("isTidyMode", 0)
            })

            // Insert cross ref at version 8
            val values = ContentValues().apply {
                put("shelfId", "shelf-1")
                put("bookId", "book-1")
                put("addedAt", 1000L)
            }
            db.insert("BookshelfBookCrossRef", SQLiteDatabase.CONFLICT_REPLACE, values)
        }

        // Run migration
        helper.runMigrationsAndValidate(testDbName, 9, true, MIGRATION_8_9).use { db ->
            // Verify new columns exist with correct defaults
            db.query("SELECT * FROM BookshelfBookCrossRef WHERE shelfId = 'shelf-1' AND bookId = 'book-1'").use { cursor ->
                assertTrue("Should have result", cursor.moveToFirst())

                // Original data preserved
                assertEquals(1000L, cursor.getLong(cursor.getColumnIndexOrThrow("addedAt")))

                // New columns have defaults
                assertEquals("PENDING", cursor.getString(cursor.getColumnIndexOrThrow("syncStatus")))
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("lastModifiedAt")))
            }
        }
    }

    @Test
    fun migrate8To9_createsSyncMetadataEntityTable() {
        // Create database at version 8
        helper.createDatabase(testDbName, 8).use { }

        // Run migration
        helper.runMigrationsAndValidate(testDbName, 9, true, MIGRATION_8_9).use { db ->
            // Verify table exists by inserting data
            val values = ContentValues().apply {
                put("userId", "test-user")
                put("lastSyncTimestamp", 1234567890L)
                put("syncInProgress", 0)
                put("pendingOperationsCount", 0)
            }
            val result = db.insert("SyncMetadataEntity", SQLiteDatabase.CONFLICT_REPLACE, values)
            assertTrue("Insert should succeed", result != -1L)

            // Verify data was inserted
            db.query("SELECT * FROM SyncMetadataEntity WHERE userId = 'test-user'").use { cursor ->
                assertTrue("Should have result", cursor.moveToFirst())
                assertEquals("test-user", cursor.getString(cursor.getColumnIndexOrThrow("userId")))
                assertEquals(1234567890L, cursor.getLong(cursor.getColumnIndexOrThrow("lastSyncTimestamp")))
            }
        }
    }

    @Test
    fun migrate8To9_createsIndexesForSyncQueries() {
        // Create database at version 8
        helper.createDatabase(testDbName, 8).use { }

        // Run migration
        helper.runMigrationsAndValidate(testDbName, 9, true, MIGRATION_8_9).use { db ->
            // Query SQLite master for index information
            db.query("SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'index_%'").use { cursor ->
                val indexes = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    indexes.add(cursor.getString(0))
                }

                // Verify expected indexes exist
                assertTrue("Should have BookEntity syncStatus index",
                    indexes.contains("index_BookEntity_syncStatus"))
                assertTrue("Should have BookEntity ownerId index",
                    indexes.contains("index_BookEntity_ownerId"))
                assertTrue("Should have BookshelfEntity syncStatus index",
                    indexes.contains("index_BookshelfEntity_syncStatus"))
                assertTrue("Should have BookshelfEntity ownerId index",
                    indexes.contains("index_BookshelfEntity_ownerId"))
                assertTrue("Should have BookshelfEntity shareCode index",
                    indexes.contains("index_BookshelfEntity_shareCode"))
                assertTrue("Should have CrossRef syncStatus index",
                    indexes.contains("index_BookshelfBookCrossRef_syncStatus"))
            }
        }
    }

    @Test
    fun migrate8To9_preservesExistingData() {
        // Create database at version 8 with complex data
        helper.createDatabase(testDbName, 8).use { db ->
            // Insert multiple books
            for (i in 1..3) {
                db.insert("BookEntity", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
                    put("id", "book-$i")
                    put("title", "Book $i")
                    put("description", "Description $i")
                    put("imageUrl", "https://example.com/book$i.jpg")
                    put("languages", "[\"en\"]")
                    put("authors", "[\"Author $i\"]")
                    put("firstPublishYear", (2020 + i).toString())
                    put("ratingsAverage", 4.0 + (i * 0.1))
                    put("ratingsCount", i * 100)
                    put("numPagesMedian", i * 100)
                    put("numEditions", i)
                    put("purchased", if (i == 2) 1 else 0)
                    put("spineColor", -16711936 + i)
                    put("readingStatus", "WANT_TO_READ")
                    put("personalRating", 0.0f)
                    put("personalNotes", "Notes $i")
                })
            }

            // Insert shelves
            for (i in 1..2) {
                db.insert("BookshelfEntity", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
                    put("id", "shelf-$i")
                    put("name", "Shelf $i")
                    put("shelfMaterial", if (i == 1) "DARK_WOOD" else "LIGHT_WOOD")
                    put("position", i - 1)
                    put("isTidyMode", if (i == 1) 1 else 0)
                })
            }
        }

        // Run migration
        helper.runMigrationsAndValidate(testDbName, 9, true, MIGRATION_8_9).use { db ->
            // Verify all books preserved
            db.query("SELECT COUNT(*) FROM BookEntity").use { cursor ->
                cursor.moveToFirst()
                assertEquals(3, cursor.getInt(0))
            }

            // Verify book details preserved
            db.query("SELECT * FROM BookEntity WHERE id = 'book-2'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Book 2", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("purchased")))
                assertEquals("Notes 2", cursor.getString(cursor.getColumnIndexOrThrow("personalNotes")))
            }

            // Verify all shelves preserved
            db.query("SELECT COUNT(*) FROM BookshelfEntity").use { cursor ->
                cursor.moveToFirst()
                assertEquals(2, cursor.getInt(0))
            }

            // Verify shelf details preserved
            db.query("SELECT * FROM BookshelfEntity WHERE id = 'shelf-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Shelf 1", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                assertEquals("DARK_WOOD", cursor.getString(cursor.getColumnIndexOrThrow("shelfMaterial")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isTidyMode")))
            }
        }
    }

    @Test
    fun migratedDatabaseCanBeOpenedWithRoom() {
        // Create and migrate database
        helper.createDatabase(testDbName, 8).use { db ->
            db.insert("BookEntity", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
                put("id", "book-1")
                put("title", "Test Book")
                put("imageUrl", "url")
                put("languages", "[\"en\"]")
                put("authors", "[\"Author\"]")
                put("firstPublishYear", "2020")
                put("ratingsCount", 0)
                put("numPagesMedian", 0)
                put("numEditions", 0)
                put("purchased", 0)
                put("spineColor", 0)
                put("readingStatus", "WANT_TO_READ")
                put("personalRating", 0.0f)
                put("personalNotes", "")
            })
        }

        helper.runMigrationsAndValidate(testDbName, 9, true, MIGRATION_8_9).use { }

        // Open with Room to verify schema compatibility
        val roomDb = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BookshelfDatabase::class.java,
            testDbName
        ).addMigrations(MIGRATION_8_9).build()

        try {
            // Should be able to query without errors
            kotlinx.coroutines.runBlocking {
                val book = roomDb.bookshelfDao.getBookById("book-1")
                assertNotNull("Should retrieve migrated book", book)
                assertEquals("Test Book", book?.title)
                assertEquals("PENDING", book?.syncStatus)
            }
        } finally {
            roomDb.close()
        }
    }
}
