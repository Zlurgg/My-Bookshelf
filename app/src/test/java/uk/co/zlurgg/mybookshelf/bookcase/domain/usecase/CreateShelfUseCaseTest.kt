package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.GetOrCreateTutorialBookUseCase
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.TestIdGenerator
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncSchedulerService

/**
 * Clean UseCase test demonstrating focused testing principles:
 * - Test business logic in isolation
 * - Use simple mocks instead of complex fakes
 * - Test one behavior per test method
 * - Clear, descriptive test names
 */
class CreateShelfUseCaseTest {

    private val mockRepository = MockBookcaseRepository()
    private val testIdGenerator = TestIdGenerator()
    private val mockSyncSchedulerService = MockSyncSchedulerService()
    private val mockGetOrCreateTutorialBook = object : GetOrCreateTutorialBookUseCase {
        override suspend operator fun invoke(tutorialShelfId: String): Result<String, DataError.Local> {
            return Result.Success("tutorial-book-id")
        }
    }
    private val useCase = CreateShelfUseCaseImpl(
        mockRepository,
        testIdGenerator,
        mockGetOrCreateTutorialBook,
        mockSyncSchedulerService
    )

    @Test
    fun `creates shelf with correct data when no existing shelves`() = runTest {
        // Given
        val name = "My Books"
        val style = ShelfStyle.DarkWood
        val existingShelves = emptyList<Bookshelf>()

        // When
        val result = useCase(name, style, existingShelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val shelf = (result as Result.Success).data

        assertEquals("Should have correct name", name, shelf.name)
        assertEquals("Should have correct style", style, shelf.shelfStyle)
        assertEquals("Should start at position 0", 0, shelf.position)
        assertEquals("Should have empty books list", emptyList<Book>(), shelf.books)
        assertTrue("Should have generated ID", shelf.id.isNotEmpty())
        assertTrue("Should call repository", mockRepository.addShelfCalled)
    }

    @Test
    fun `calculates correct position when existing shelves present`() = runTest {
        // Given
        val existingShelves = listOf(
            TestShelfBuilder().withId("1").withPosition(0).build(),
            TestShelfBuilder().withId("2").withPosition(2).build(),
            TestShelfBuilder().withId("3").withPosition(1).build()
        )

        // When
        val result = useCase("New Shelf", ShelfStyle.SilverMetal, existingShelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val shelf = (result as Result.Success).data
        assertEquals("Should position after highest existing shelf", 3, shelf.position)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        mockRepository.errorToReturn = DataError.Local.UNKNOWN

        // When
        val result = useCase("Test", ShelfStyle.DarkWood, emptyList())

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.UNKNOWN, error)
    }

    @Test
    fun `generates unique ID for each shelf`() = runTest {
        // Given - reset counter for predictable test
        testIdGenerator.reset()

        // When
        val result1 = useCase("Shelf 1", ShelfStyle.DarkWood, emptyList())
        val result2 = useCase("Shelf 2", ShelfStyle.SilverMetal, emptyList())

        // Then
        val shelf1 = (result1 as Result.Success).data
        val shelf2 = (result2 as Result.Success).data
        assertTrue("Should have different IDs", shelf1.id != shelf2.id)
    }
}
