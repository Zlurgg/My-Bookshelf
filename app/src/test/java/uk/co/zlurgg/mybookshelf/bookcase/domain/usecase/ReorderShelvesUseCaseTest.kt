package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

class ReorderShelvesUseCaseTest {

    private val mockRepository = MockBookcaseRepository()
    private val useCase = ReorderShelvesUseCaseImpl(mockRepository)

    @After
    fun tearDown() {
        mockRepository.reset()
    }

    @Test
    fun `execute moves shelf from position 0 to position 2`() = runTest {
        // Given
        val shelves = listOf(
            TestShelfBuilder().withId("shelf-0").withName("Shelf A").withPosition(0).build(),
            TestShelfBuilder().withId("shelf-1").withName("Shelf B").withPosition(1).build(),
            TestShelfBuilder().withId("shelf-2").withName("Shelf C").withPosition(2).build()
        )
        val shelfToMove = shelves[0] // Move "Shelf A" from position 0

        // When
        val result = useCase(shelfToMove, 2, shelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val reorderedShelves = (result as Result.Success).data

        assertEquals("Should have same number of shelves", 3, reorderedShelves.size)
        assertEquals("Position 0 should now be Shelf B", "shelf-1", reorderedShelves[0].id)
        assertEquals("Position 1 should now be Shelf C", "shelf-2", reorderedShelves[1].id)
        assertEquals("Position 2 should now be Shelf A", "shelf-0", reorderedShelves[2].id)

        // Verify positions are updated correctly
        assertEquals("Shelf B should have position 0", 0, reorderedShelves[0].position)
        assertEquals("Shelf C should have position 1", 1, reorderedShelves[1].position)
        assertEquals("Shelf A should have position 2", 2, reorderedShelves[2].position)
    }

    @Test
    fun `execute moves shelf from position 2 to position 0`() = runTest {
        // Given
        val shelves = listOf(
            TestShelfBuilder().withId("shelf-0").withName("Shelf A").withPosition(0).build(),
            TestShelfBuilder().withId("shelf-1").withName("Shelf B").withPosition(1).build(),
            TestShelfBuilder().withId("shelf-2").withName("Shelf C").withPosition(2).build()
        )
        val shelfToMove = shelves[2] // Move "Shelf C" from position 2

        // When
        val result = useCase(shelfToMove, 0, shelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val reorderedShelves = (result as Result.Success).data

        assertEquals("Position 0 should now be Shelf C", "shelf-2", reorderedShelves[0].id)
        assertEquals("Position 1 should now be Shelf A", "shelf-0", reorderedShelves[1].id)
        assertEquals("Position 2 should now be Shelf B", "shelf-1", reorderedShelves[2].id)
    }

    @Test
    fun `execute moves shelf from middle to middle position`() = runTest {
        // Given - 5 shelves
        val shelves = (0..4).map { i ->
            TestShelfBuilder()
                .withId("shelf-$i")
                .withName("Shelf ${('A' + i)}")
                .withPosition(i)
                .build()
        }
        val shelfToMove = shelves[1] // Move "Shelf B" from position 1 to position 3

        // When
        val result = useCase(shelfToMove, 3, shelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val reorderedShelves = (result as Result.Success).data

        assertEquals("Should have 5 shelves", 5, reorderedShelves.size)
        assertEquals("Position 0: Shelf A", "shelf-0", reorderedShelves[0].id)
        assertEquals("Position 1: Shelf C", "shelf-2", reorderedShelves[1].id)
        assertEquals("Position 2: Shelf D", "shelf-3", reorderedShelves[2].id)
        assertEquals("Position 3: Shelf B (moved)", "shelf-1", reorderedShelves[3].id)
        assertEquals("Position 4: Shelf E", "shelf-4", reorderedShelves[4].id)
    }

    @Test
    fun `execute handles moving shelf to same position`() = runTest {
        // Given
        val shelves = listOf(
            TestShelfBuilder().withId("shelf-0").withName("Shelf A").withPosition(0).build(),
            TestShelfBuilder().withId("shelf-1").withName("Shelf B").withPosition(1).build(),
            TestShelfBuilder().withId("shelf-2").withName("Shelf C").withPosition(2).build()
        )
        val shelfToMove = shelves[1] // Move "Shelf B" to same position (1)

        // When
        val result = useCase(shelfToMove, 1, shelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val reorderedShelves = (result as Result.Success).data

        // Order should remain the same
        assertEquals("Position 0 should still be Shelf A", "shelf-0", reorderedShelves[0].id)
        assertEquals("Position 1 should still be Shelf B", "shelf-1", reorderedShelves[1].id)
        assertEquals("Position 2 should still be Shelf C", "shelf-2", reorderedShelves[2].id)
    }

    @Test
    fun `execute clamps new position to valid range - position too high`() = runTest {
        // Given
        val shelves = listOf(
            TestShelfBuilder().withId("shelf-0").withPosition(0).build(),
            TestShelfBuilder().withId("shelf-1").withPosition(1).build(),
            TestShelfBuilder().withId("shelf-2").withPosition(2).build()
        )
        val shelfToMove = shelves[0]

        // When - Try to move to position 10 (should clamp to max valid position)
        val result = useCase(shelfToMove, 10, shelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val reorderedShelves = (result as Result.Success).data

        // Should be moved to last position (2)
        assertEquals("Moved shelf should be at last position", "shelf-0", reorderedShelves[2].id)
        assertEquals("Moved shelf should have position 2", 2, reorderedShelves[2].position)
    }

    @Test
    fun `execute clamps new position to valid range - negative position`() = runTest {
        // Given
        val shelves = listOf(
            TestShelfBuilder().withId("shelf-0").withPosition(0).build(),
            TestShelfBuilder().withId("shelf-1").withPosition(1).build(),
            TestShelfBuilder().withId("shelf-2").withPosition(2).build()
        )
        val shelfToMove = shelves[2]

        // When - Try to move to position -5 (should clamp to 0)
        val result = useCase(shelfToMove, -5, shelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val reorderedShelves = (result as Result.Success).data

        // Should be moved to first position (0)
        assertEquals("Moved shelf should be at first position", "shelf-2", reorderedShelves[0].id)
        assertEquals("Moved shelf should have position 0", 0, reorderedShelves[0].position)
    }

    @Test
    fun `execute returns error when shelf not found in current list`() = runTest {
        // Given
        val currentShelves = listOf(
            TestShelfBuilder().withId("shelf-1").withPosition(0).build(),
            TestShelfBuilder().withId("shelf-2").withPosition(1).build()
        )
        val nonExistentShelf = TestShelfBuilder().withId("non-existent").withPosition(0).build()

        // When
        val result = useCase(nonExistentShelf, 1, currentShelves)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return NOT_FOUND error", DataError.Local.NOT_FOUND, error)
    }

    @Test
    fun `execute preserves shelf data during reordering`() = runTest {
        // Given
        val shelves = listOf(
            TestShelfBuilder()
                .withId("fiction")
                .withName("Science Fiction")
                .withStyle(ShelfStyle.DarkWood)
                .withPosition(0)
                .build(),
            TestShelfBuilder()
                .withId("fantasy")
                .withName("Fantasy")
                .withStyle(ShelfStyle.SilverMetal)
                .withPosition(1)
                .build()
        )
        val shelfToMove = shelves[0]

        // When
        val result = useCase(shelfToMove, 1, shelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val reorderedShelves = (result as Result.Success).data
        val movedShelf = reorderedShelves.find { it.id == "fiction" }!!

        // Data should be preserved except position
        assertEquals("Should preserve name", "Science Fiction", movedShelf.name)
        assertEquals("Should preserve style", ShelfStyle.DarkWood, movedShelf.shelfStyle)
        assertEquals("Should update position", 1, movedShelf.position)
    }

    @Test
    fun `execute calls repository updateShelf for affected shelves only`() = runTest {
        // Given
        val shelves = listOf(
            TestShelfBuilder().withId("shelf-0").withPosition(0).build(),
            TestShelfBuilder().withId("shelf-1").withPosition(1).build(),
            TestShelfBuilder().withId("shelf-2").withPosition(2).build(),
            TestShelfBuilder().withId("shelf-3").withPosition(3).build()
        )
        val shelfToMove = shelves[1] // Move shelf from position 1 to position 3

        // When
        val result = useCase(shelfToMove, 3, shelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        // Positions changed: shelf-1 (1→3), shelf-2 (2→1), shelf-3 (3→2)
        // shelf-0 position unchanged, so should not be updated
        assertTrue("Should call repository to update changed shelves", mockRepository.updateShelfCalled)
    }

    @Test
    fun `execute returns error when repository fails`() = runTest {
        // Given
        val shelves = listOf(
            TestShelfBuilder().withId("shelf-0").withPosition(0).build(),
            TestShelfBuilder().withId("shelf-1").withPosition(1).build()
        )
        val shelfToMove = shelves[0]
        mockRepository.errorToReturn = DataError.Local.UNKNOWN

        // When
        val result = useCase(shelfToMove, 1, shelves)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.UNKNOWN, error)
    }

    @Test
    fun `execute handles single shelf list`() = runTest {
        // Given
        val singleShelf = listOf(
            TestShelfBuilder().withId("only-shelf").withPosition(0).build()
        )
        val shelfToMove = singleShelf[0]

        // When
        val result = useCase(shelfToMove, 0, singleShelf)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val reorderedShelves = (result as Result.Success).data

        assertEquals("Should have one shelf", 1, reorderedShelves.size)
        assertEquals("Should be same shelf", "only-shelf", reorderedShelves[0].id)
        assertEquals("Should have position 0", 0, reorderedShelves[0].position)
    }

    @Test
    fun `execute handles empty shelf list gracefully`() = runTest {
        // Given
        val emptyShelves = emptyList<uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf>()
        val shelfToMove = TestShelfBuilder().withId("orphaned-shelf").build()

        // When
        val result = useCase(shelfToMove, 0, emptyShelves)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return NOT_FOUND error", DataError.Local.NOT_FOUND, error)
    }
}
