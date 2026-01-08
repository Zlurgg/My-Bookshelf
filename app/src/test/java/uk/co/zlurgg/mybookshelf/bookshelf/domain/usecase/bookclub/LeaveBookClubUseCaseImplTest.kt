package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookcaseRepository

class LeaveBookClubUseCaseImplTest {
    private val mockBookcaseRepository = MockBookcaseRepository()
    private val mockBookClubRepository = MockBookClubRepository()
    private val useCase = LeaveBookClubUseCaseImpl(mockBookcaseRepository, mockBookClubRepository)

    @After
    fun tearDown() {
        mockBookcaseRepository.reset()
        mockBookClubRepository.reset()
    }

    // ========== Shelf Validation Tests ==========

    @Test
    fun `returns DOCUMENT_NOT_FOUND when shelf does not exist`() =
        runTest {
            // Given
            mockBookcaseRepository.shelfByIdToReturn = null
            val shelfId = "non-existent-shelf"

            // When
            val result = useCase(shelfId)

            // Then
            assertTrue("Should return error", result is Result.Error)
            assertEquals(DataError.Sync.DOCUMENT_NOT_FOUND, (result as Result.Error).error)
        }

    @Test
    fun `returns CLUB_NOT_FOUND when shelf is not a book club`() =
        runTest {
            // Given - regular shelf, not a book club
            val regularShelf =
                TestShelfBuilder()
                    .withId("regular-shelf")
                    .withName("My Books")
                    .build()
            mockBookcaseRepository.shelfByIdToReturn = regularShelf

            // When
            val result = useCase("regular-shelf")

            // Then
            assertTrue("Should return error", result is Result.Error)
            assertEquals(DataError.Sync.CLUB_NOT_FOUND, (result as Result.Error).error)
        }

    @Test
    fun `returns CLUB_NOT_FOUND when book club shelf has null club code`() =
        runTest {
            // Given - book club shelf with missing club code (edge case)
            val bookClubShelfNoCode =
                TestShelfBuilder()
                    .withId("book-club-shelf")
                    .withName("Book Club Shelf")
                    .withIsBookClub(true)
                    .withClubCode(null)
                    .build()
            mockBookcaseRepository.shelfByIdToReturn = bookClubShelfNoCode

            // When
            val result = useCase("book-club-shelf")

            // Then
            assertTrue("Should return error", result is Result.Error)
            assertEquals(DataError.Sync.CLUB_NOT_FOUND, (result as Result.Error).error)
        }

    // ========== Successful Leave Tests ==========

    @Test
    fun `returns success when user successfully leaves book club`() =
        runTest {
            // Given - valid book club shelf
            val bookClubShelf =
                TestShelfBuilder()
                    .withId("club-shelf")
                    .withName("Science Fiction Club")
                    .withIsBookClub(true)
                    .withClubCode("TEST1234")
                    .build()
            mockBookcaseRepository.shelfByIdToReturn = bookClubShelf
            mockBookClubRepository.leaveBookClubResult = Result.Success(Unit)

            // When
            val result = useCase("club-shelf")

            // Then
            assertTrue("Should return success", result is Result.Success)
            assertTrue("Should call leaveBookClub on repository", mockBookClubRepository.leaveBookClubCalled)
            assertEquals("TEST1234", mockBookClubRepository.lastLeaveCode)
        }

    @Test
    fun `calls repository with correct club code`() =
        runTest {
            // Given
            val bookClubShelf =
                TestShelfBuilder()
                    .withId("my-club-shelf")
                    .withName("Mystery Club")
                    .withIsBookClub(true)
                    .withClubCode("MYST5678")
                    .build()
            mockBookcaseRepository.shelfByIdToReturn = bookClubShelf
            mockBookClubRepository.leaveBookClubResult = Result.Success(Unit)

            // When
            useCase("my-club-shelf")

            // Then
            assertEquals("Should call with correct code", "MYST5678", mockBookClubRepository.lastLeaveCode)
        }

    // ========== Error Propagation Tests ==========

    @Test
    fun `returns CREATOR_CANNOT_LEAVE when user is creator`() =
        runTest {
            // Given - creator tries to leave
            val bookClubShelf =
                TestShelfBuilder()
                    .withId("owned-club-shelf")
                    .withName("My Club")
                    .withIsBookClub(true)
                    .withClubCode("MYCLUB12")
                    .build()
            mockBookcaseRepository.shelfByIdToReturn = bookClubShelf
            mockBookClubRepository.leaveBookClubResult = Result.Error(DataError.Sync.CREATOR_CANNOT_LEAVE)

            // When
            val result = useCase("owned-club-shelf")

            // Then
            assertTrue("Should return error", result is Result.Error)
            assertEquals(DataError.Sync.CREATOR_CANNOT_LEAVE, (result as Result.Error).error)
        }

    @Test
    fun `propagates NOT_SIGNED_IN error from repository`() =
        runTest {
            // Given
            val bookClubShelf =
                TestShelfBuilder()
                    .withId("club-shelf")
                    .withName("Test Club")
                    .withIsBookClub(true)
                    .withClubCode("CLUB1234")
                    .build()
            mockBookcaseRepository.shelfByIdToReturn = bookClubShelf
            mockBookClubRepository.leaveBookClubResult = Result.Error(DataError.Sync.NOT_SIGNED_IN)

            // When
            val result = useCase("club-shelf")

            // Then
            assertTrue("Should return error", result is Result.Error)
            assertEquals(DataError.Sync.NOT_SIGNED_IN, (result as Result.Error).error)
        }

    @Test
    fun `propagates NETWORK_ERROR from repository`() =
        runTest {
            // Given
            val bookClubShelf =
                TestShelfBuilder()
                    .withId("club-shelf")
                    .withName("Test Club")
                    .withIsBookClub(true)
                    .withClubCode("CLUB1234")
                    .build()
            mockBookcaseRepository.shelfByIdToReturn = bookClubShelf
            mockBookClubRepository.leaveBookClubResult = Result.Error(DataError.Sync.NETWORK_ERROR)

            // When
            val result = useCase("club-shelf")

            // Then
            assertTrue("Should return error", result is Result.Error)
            assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
        }

    @Test
    fun `propagates NOT_MEMBER error from repository`() =
        runTest {
            // Given
            val bookClubShelf =
                TestShelfBuilder()
                    .withId("club-shelf")
                    .withName("Test Club")
                    .withIsBookClub(true)
                    .withClubCode("CLUB1234")
                    .build()
            mockBookcaseRepository.shelfByIdToReturn = bookClubShelf
            mockBookClubRepository.leaveBookClubResult = Result.Error(DataError.Sync.NOT_MEMBER)

            // When
            val result = useCase("club-shelf")

            // Then
            assertTrue("Should return error", result is Result.Error)
            assertEquals(DataError.Sync.NOT_MEMBER, (result as Result.Error).error)
        }
}
