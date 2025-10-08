package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

/**
 * TDD-style test for RenameShelfUseCase.
 * Tests validation logic and repository interaction for shelf renaming.
 */
class RenameShelfUseCaseTest {

    private val mockRepository = MockBookcaseRepository()
    private lateinit var useCase: RenameShelfUseCase

    @Before
    fun setup() {
        mockRepository.reset()
        useCase = RenameShelfUseCaseImpl(mockRepository)
    }

    @Test
    fun `successfully renames shelf with valid new name`() = runTest {
        // Given - An existing shelf
        val existingShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Old Name")
            .withStyle(ShelfStyle.DarkWood)
            .withPosition(0)
            .build()

        val otherShelves = listOf(
            TestShelfBuilder().withId("shelf-2").withName("Other Shelf").build()
        )

        mockRepository.shelfByIdToReturn = existingShelf
        mockRepository.configureShelves(listOf(existingShelf) + otherShelves)

        val newName = "New Name"

        // When
        val result = useCase.execute("shelf-1", newName)

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call updateShelf", mockRepository.updateShelfCalled)

        val updatedShelf = mockRepository.lastUpdatedShelf
        assertEquals("Should have new name", newName, updatedShelf?.name)
        assertEquals("Should preserve ID", "shelf-1", updatedShelf?.id)
        assertEquals("Should preserve style", ShelfStyle.DarkWood, updatedShelf?.shelfStyle)
        assertEquals("Should preserve position", 0, updatedShelf?.position)
    }

    @Test
    fun `returns error when shelf not found`() = runTest {
        // Given - Shelf doesn't exist
        mockRepository.shelfByIdToReturn = null

        // When
        val result = useCase.execute("non-existent-shelf", "New Name")

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return NOT_FOUND error", DataError.Local.NOT_FOUND, error)
    }

    @Test
    fun `returns error when new name is blank`() = runTest {
        // Given - Existing shelf with blank new name
        val existingShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Old Name")
            .build()

        mockRepository.shelfByIdToReturn = existingShelf
        mockRepository.configureShelves(listOf(existingShelf))

        // When
        val result = useCase.execute("shelf-1", "   ")

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return VALIDATION_ERROR", DataError.Local.VALIDATION_ERROR, error)
    }

    @Test
    fun `returns error when new name is empty`() = runTest {
        // Given - Existing shelf with empty new name
        val existingShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Old Name")
            .build()

        mockRepository.shelfByIdToReturn = existingShelf
        mockRepository.configureShelves(listOf(existingShelf))

        // When
        val result = useCase.execute("shelf-1", "")

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return VALIDATION_ERROR", DataError.Local.VALIDATION_ERROR, error)
    }

    @Test
    fun `returns error when new name conflicts with existing shelf`() = runTest {
        // Given - Two shelves, trying to rename to existing name
        val shelfToRename = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Fiction")
            .build()

        val existingShelf = TestShelfBuilder()
            .withId("shelf-2")
            .withName("Science Fiction")
            .build()

        mockRepository.shelfByIdToReturn = shelfToRename
        mockRepository.configureShelves(listOf(shelfToRename, existingShelf))

        // When - Try to rename to existing shelf's name
        val result = useCase.execute("shelf-1", "Science Fiction")

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return NAME_CONFLICT error", DataError.Local.NAME_CONFLICT, error)
    }

    @Test
    fun `allows renaming to same name (case insensitive)`() = runTest {
        // Given - Shelf with name "Fiction"
        val existingShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Fiction")
            .build()

        mockRepository.shelfByIdToReturn = existingShelf
        mockRepository.configureShelves(listOf(existingShelf))

        // When - Rename to same name with different case
        val result = useCase.execute("shelf-1", "fiction")

        // Then - Should succeed (renaming to self)
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call updateShelf", mockRepository.updateShelfCalled)
        assertEquals("Should have new name", "fiction", mockRepository.lastUpdatedShelf?.name)
    }

    @Test
    fun `trims whitespace from new name`() = runTest {
        // Given - Existing shelf
        val existingShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Old Name")
            .build()

        mockRepository.shelfByIdToReturn = existingShelf
        mockRepository.configureShelves(listOf(existingShelf))

        // When - Rename with whitespace
        val result = useCase.execute("shelf-1", "  New Name  ")

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should trim whitespace", "New Name", mockRepository.lastUpdatedShelf?.name)
    }

    @Test
    fun `returns error when repository throws exception`() = runTest {
        // Given - Repository configured to throw
        val existingShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Old Name")
            .build()

        mockRepository.shelfByIdToReturn = existingShelf
        mockRepository.configureShelves(listOf(existingShelf))
        mockRepository.shouldThrowException = true

        // When
        val result = useCase.execute("shelf-1", "New Name")

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return UNKNOWN error", DataError.Local.UNKNOWN, error)
    }

    @Test
    fun `preserves all shelf properties except name`() = runTest {
        // Given - Shelf with books and all properties
        val books = listOf(
            TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        )
        val existingShelf = TestShelfBuilder()
            .withId("shelf-1")
            .withName("Old Name")
            .withStyle(ShelfStyle.SilverMetal)
            .withPosition(5)
            .withBooks(books)
            .build()

        mockRepository.shelfByIdToReturn = existingShelf
        mockRepository.configureShelves(listOf(existingShelf))

        // When
        val result = useCase.execute("shelf-1", "New Name")

        // Then
        assertTrue("Should return success", result is Result.Success)

        val updatedShelf = mockRepository.lastUpdatedShelf!!
        assertEquals("Should update name", "New Name", updatedShelf.name)
        assertEquals("Should preserve ID", "shelf-1", updatedShelf.id)
        assertEquals("Should preserve style", ShelfStyle.SilverMetal, updatedShelf.shelfStyle)
        assertEquals("Should preserve position", 5, updatedShelf.position)
        assertEquals("Should preserve books", books, updatedShelf.books)
    }
}
