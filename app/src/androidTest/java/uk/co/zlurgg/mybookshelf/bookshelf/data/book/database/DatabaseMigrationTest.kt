package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive database migration tests.
 *
 * These tests validate that migrations preserve data integrity and apply schema changes correctly.
 * Uses Room's MigrationTestHelper to test real migrations on an Android device/emulator.
 *
 * Critical for ensuring user data survives app upgrades.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB_NAME = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BookshelfDatabase::class.java,
        listOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_removesOnShelfColumn_preservesBookData() {
        // Given - Create database at version 2 with sample book
        helper.createDatabase(TEST_DB_NAME, 2).apply {
            execSQL(
                """
                INSERT INTO BookEntity (id, title, description, imageUrl, languages, authors,
                firstPublishYear, ratingsAverage, ratingsCount, numPagesMedian, numEditions,
                purchased, onShelf, affiliateLink, spineColor)
                VALUES ('book-1', 'Test Book', 'Test description', 'https://example.com/cover.jpg',
                '["en"]', '["Test Author"]', '2020', 4.5, 100, 300, 5, 0, 1,
                'https://example.com/buy', -16711936)
                """.trimIndent()
            )
            close()
        }

        // When - Migrate to version 3
        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, MIGRATION_2_3)

        // Then - Verify onShelf column removed and data preserved
        val cursor = db.query("SELECT * FROM BookEntity WHERE id = 'book-1'")
        assertTrue("Should have book data", cursor.moveToFirst())

        // Verify onShelf column removed
        assertEquals("onShelf column should be removed", -1, cursor.getColumnIndex("onShelf"))

        // Verify critical data preserved
        assertEquals("book-1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Test Book", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("Test description", cursor.getString(cursor.getColumnIndexOrThrow("description")))
        assertEquals("https://example.com/buy", cursor.getString(cursor.getColumnIndexOrThrow("affiliateLink")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("purchased")))
        assertEquals(-16711936, cursor.getInt(cursor.getColumnIndexOrThrow("spineColor")))

        cursor.close()
    }

    @Test
    fun migrate3To4_addsPositionColumn_withDefaultValueZero() {
        // Given - Create database at version 3 with sample bookshelf
        helper.createDatabase(TEST_DB_NAME, 3).apply {
            execSQL(
                """
                INSERT INTO BookshelfEntity (id, name, shelfMaterial)
                VALUES ('shelf-1', 'Test Shelf', 'DARK_WOOD')
                """.trimIndent()
            )
            close()
        }

        // When - Migrate to version 4
        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 4, true, MIGRATION_3_4)

        // Then - Verify position column added with default value 0
        val cursor = db.query("SELECT * FROM BookshelfEntity WHERE id = 'shelf-1'")
        assertTrue("Should have shelf data", cursor.moveToFirst())

        // Verify position column exists with default value
        val positionIndex = cursor.getColumnIndex("position")
        assertTrue("Position column should exist", positionIndex >= 0)
        assertEquals("Default position should be 0", 0, cursor.getInt(positionIndex))

        // Verify other data preserved
        assertEquals("shelf-1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Test Shelf", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals("DARK_WOOD", cursor.getString(cursor.getColumnIndexOrThrow("shelfMaterial")))

        cursor.close()
    }

    @Test
    fun migrate4To5_removesAffiliateLinkColumn_preservesBookData() {
        // Given - Create database at version 4 with sample book
        helper.createDatabase(TEST_DB_NAME, 4).apply {
            execSQL(
                """
                INSERT INTO BookEntity (id, title, description, imageUrl, languages, authors,
                firstPublishYear, ratingsAverage, ratingsCount, numPagesMedian, numEditions,
                purchased, affiliateLink, spineColor)
                VALUES ('book-2', 'Another Book', 'Another description', 'https://example.com/cover2.jpg',
                '["en"]', '["Another Author"]', '2021', 4.0, 50, 250, 3, 1,
                'https://example.com/buy2', -65536)
                """.trimIndent()
            )
            close()
        }

        // When - Migrate to version 5
        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 5, true, MIGRATION_4_5)

        // Then - Verify affiliateLink column removed and data preserved
        val cursor = db.query("SELECT * FROM BookEntity WHERE id = 'book-2'")
        assertTrue("Should have book data", cursor.moveToFirst())

        // Verify affiliateLink column removed
        assertEquals("affiliateLink column should be removed", -1, cursor.getColumnIndex("affiliateLink"))

        // Verify all other data preserved
        assertEquals("book-2", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Another Book", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("Another description", cursor.getString(cursor.getColumnIndexOrThrow("description")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("purchased")))
        assertEquals(-65536, cursor.getInt(cursor.getColumnIndexOrThrow("spineColor")))

        cursor.close()
    }

    @Test
    fun migrateAll_2To5_preservesCompleteDataIntegrity() {
        // Given - Create database at version 2 with comprehensive test data
        helper.createDatabase(TEST_DB_NAME, 2).apply {
            // Insert book with all fields
            execSQL(
                """
                INSERT INTO BookEntity (id, title, description, imageUrl, languages, authors,
                firstPublishYear, ratingsAverage, ratingsCount, numPagesMedian, numEditions,
                purchased, onShelf, affiliateLink, spineColor)
                VALUES ('book-complete', 'Complete Test', 'Full test description', 'https://example.com/cover3.jpg',
                '["en","fr"]', '["Author One","Author Two"]', '2019', 4.8, 200, 400, 10, 1, 0,
                'https://example.com/buy3', -16776961)
                """.trimIndent()
            )

            // Insert bookshelf
            execSQL(
                """
                INSERT INTO BookshelfEntity (id, name, shelfMaterial)
                VALUES ('shelf-complete', 'Complete Shelf', 'SILVER_METAL')
                """.trimIndent()
            )

            // Insert cross-reference
            execSQL(
                """
                INSERT INTO BookshelfBookCrossRef (shelfId, bookId, addedAt)
                VALUES ('shelf-complete', 'book-complete', 1234567890)
                """.trimIndent()
            )

            close()
        }

        // When - Migrate through all versions 2→3→4→5
        val db = helper.runMigrationsAndValidate(
            TEST_DB_NAME, 5, true,
            MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
        )

        // Then - Verify complete data integrity across all tables

        // 1. Verify book data survived all migrations
        val bookCursor = db.query("SELECT * FROM BookEntity WHERE id = 'book-complete'")
        assertTrue("Should have book data after all migrations", bookCursor.moveToFirst())
        assertEquals("Complete Test", bookCursor.getString(bookCursor.getColumnIndexOrThrow("title")))
        assertEquals("Full test description", bookCursor.getString(bookCursor.getColumnIndexOrThrow("description")))
        assertEquals(1, bookCursor.getInt(bookCursor.getColumnIndexOrThrow("purchased")))
        assertEquals(-16776961, bookCursor.getInt(bookCursor.getColumnIndexOrThrow("spineColor")))

        // Verify columns removed by migrations
        assertEquals("onShelf should be removed (v2→v3)", -1, bookCursor.getColumnIndex("onShelf"))
        assertEquals("affiliateLink should be removed (v4→v5)", -1, bookCursor.getColumnIndex("affiliateLink"))
        bookCursor.close()

        // 2. Verify bookshelf data survived all migrations
        val shelfCursor = db.query("SELECT * FROM BookshelfEntity WHERE id = 'shelf-complete'")
        assertTrue("Should have shelf data after all migrations", shelfCursor.moveToFirst())
        assertEquals("Complete Shelf", shelfCursor.getString(shelfCursor.getColumnIndexOrThrow("name")))
        assertEquals("SILVER_METAL", shelfCursor.getString(shelfCursor.getColumnIndexOrThrow("shelfMaterial")))
        assertEquals(0, shelfCursor.getInt(shelfCursor.getColumnIndexOrThrow("position"))) // Added in v3→v4
        shelfCursor.close()

        // 3. Verify cross-reference relationship survived all migrations
        val crossRefCursor = db.query("SELECT * FROM BookshelfBookCrossRef WHERE shelfId = 'shelf-complete'")
        assertTrue("Should have cross-ref data after all migrations", crossRefCursor.moveToFirst())
        assertEquals("shelf-complete", crossRefCursor.getString(crossRefCursor.getColumnIndexOrThrow("shelfId")))
        assertEquals("book-complete", crossRefCursor.getString(crossRefCursor.getColumnIndexOrThrow("bookId")))
        assertEquals(1234567890L, crossRefCursor.getLong(crossRefCursor.getColumnIndexOrThrow("addedAt")))
        crossRefCursor.close()
    }
}
