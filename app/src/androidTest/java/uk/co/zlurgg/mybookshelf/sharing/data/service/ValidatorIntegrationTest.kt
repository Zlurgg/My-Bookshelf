package uk.co.zlurgg.mybookshelf.sharing.data.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.data.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.sharing.data.export.BookIdentifier
import uk.co.zlurgg.mybookshelf.sharing.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.sharing.data.export.ExportedBookshelf
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * Integration test for BookshelfImportValidatorImpl with real database.
 * Tests validation logic and name conflict detection with actual Room database queries.
 *
 * This is a medium-scope test (Google's 20% integration test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class ValidatorIntegrationTest {

    private lateinit var database: MyBookshelfRoomDatabase
    private lateinit var bookcaseRepository: BookcaseRepositoryImpl
    private lateinit var validator: BookshelfImportValidatorImpl

    // Stub CurrentUserProvider - returns null (guest mode)
    private val stubCurrentUserProvider = object : CurrentUserProvider {
        override fun getCurrentUserId(): String? = null
    }

    // Stub TimeProvider - returns fixed timestamp
    private val stubTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = System.currentTimeMillis()
    }

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MyBookshelfRoomDatabase::class.java
        ).build()

        bookcaseRepository = BookcaseRepositoryImpl(database.bookshelfDao, stubCurrentUserProvider, stubTimeProvider)
        validator = BookshelfImportValidatorImpl(bookcaseRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun validateFormatAcceptsValidData() = runTest {
        // Given - Valid export data
        val exportData = BookshelfExportData(
            bookshelf = ExportedBookshelf(
                name = "Fiction",
                shelfStyle = ShelfStyle.DarkWood,
                bookIds = listOf(
                    BookIdentifier(workId = "OL123W")
                )
            )
        )

        // When - Validate format
        val result = validator.validateFormat(exportData)

        // Then - Should succeed
        assertTrue("Validation should succeed", result is Result.Success)
    }

    @Test
    fun validateFormatRejectsBlankShelfName() = runTest {
        // Given - Export data with blank shelf name
        val exportData = BookshelfExportData(
            bookshelf = ExportedBookshelf(
                name = "   ",
                shelfStyle = ShelfStyle.DarkWood,
                bookIds = emptyList()
            )
        )

        // When - Validate format
        val result = validator.validateFormat(exportData)

        // Then - Should fail with validation error
        assertTrue("Validation should fail", result is Result.Error)
        assertEquals(
            "Should return validation error",
            DataError.Local.VALIDATION_ERROR,
            (result as Result.Error).error
        )
    }

    @Test
    fun checkNameConflictReturnsNullWhenNoConflict() = runTest {
        // Given - Database with existing shelf
        val existingShelf = Bookshelf(
            id = "shelf-1",
            name = "Existing Shelf",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )
        bookcaseRepository.addShelf(existingShelf)

        // When - Check for different name
        val result = validator.checkNameConflict("New Shelf")

        // Then - Should return null (no conflict)
        assertTrue("Check should succeed", result is Result.Success)
        assertNull("Should have no conflict", (result as Result.Success).data)
    }

    @Test
    fun checkNameConflictReturnsNameWhenConflictExists() = runTest {
        // Given - Database with existing shelf
        val existingShelf = Bookshelf(
            id = "shelf-1",
            name = "Fiction",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )
        bookcaseRepository.addShelf(existingShelf)

        // When - Check for same name
        val result = validator.checkNameConflict("Fiction")

        // Then - Should return conflicting name
        assertTrue("Check should succeed", result is Result.Success)
        assertEquals("Fiction", (result as Result.Success).data)
    }

    @Test
    fun checkNameConflictIsCaseSensitive() = runTest {
        // Given - Database with existing shelf
        val existingShelf = Bookshelf(
            id = "shelf-1",
            name = "Fiction",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )
        bookcaseRepository.addShelf(existingShelf)

        // When - Check for different case
        val result = validator.checkNameConflict("fiction")

        // Then - Should return no conflict (case sensitive)
        assertTrue("Check should succeed", result is Result.Success)
        assertNull("Should have no conflict", (result as Result.Success).data)
    }
}
