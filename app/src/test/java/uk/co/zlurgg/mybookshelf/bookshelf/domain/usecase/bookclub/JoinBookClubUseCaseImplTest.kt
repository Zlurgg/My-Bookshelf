package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockAuthService
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository

class JoinBookClubUseCaseImplTest {

    private val mockRepository = MockBookClubRepository()
    private val mockAuthService = MockAuthService()
    private val useCase = JoinBookClubUseCaseImpl(mockRepository, mockAuthService)

    @After
    fun tearDown() {
        mockRepository.reset()
        mockAuthService.reset()
    }

    // ========== Authentication Tests ==========

    @Test
    fun `returns NOT_SIGNED_IN error when user is not signed in`() = runTest {
        // Given
        mockAuthService.configureSignedOut()
        val code = "TEST1234"

        // When
        val result = useCase(code)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NOT_SIGNED_IN, (result as Result.Error).error)
    }

    // ========== Already Member Tests ==========

    @Test
    fun `returns AlreadyMember when user is already a member`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockRepository.configureAlreadyMember("existing-shelf-id")
        val code = "TEST1234"

        // When
        val result = useCase(code)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val joinResult = (result as Result.Success).data
        assertTrue("Should be AlreadyMember", joinResult is JoinResult.AlreadyMember)
        assertEquals("existing-shelf-id", (joinResult as JoinResult.AlreadyMember).localShelfId)
    }

    @Test
    fun `checks membership with correct code`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockRepository.configureNotMember()
        mockRepository.configureBookClubNotFound()
        val code = "ABCD5678"

        // When
        useCase(code)

        // Then
        assertTrue("Should check membership", mockRepository.isMemberOfClubCalled)
        assertEquals("ABCD5678", mockRepository.lastIsMemberCode)
    }

    // ========== Club Not Found Tests ==========

    @Test
    fun `returns CLUB_NOT_FOUND when club does not exist`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockRepository.configureNotMember()
        mockRepository.configureBookClubNotFound()
        val code = "INVALID1"

        // When
        val result = useCase(code)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.CLUB_NOT_FOUND, (result as Result.Error).error)
    }

    // ========== Successful Join Tests ==========

    @Test
    fun `returns Success with shelf info when join succeeds`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockRepository.configureNotMember()
        mockRepository.configureBookClub(
            BookClub(
                code = "TEST1234",
                name = "Science Fiction Club",
                style = ShelfStyle.DarkWood,
                createdAt = System.currentTimeMillis(),
                createdBy = "creator-id",
                createdByName = "Creator",
                bookCount = 5,
                memberCount = 2
            )
        )
        mockRepository.configureJoinSuccess(
            localShelfId = "new-shelf-id",
            shelfName = "Science Fiction Club (Book Club)"
        )

        // When
        val result = useCase("TEST1234")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val joinResult = (result as Result.Success).data
        assertTrue("Should be Success", joinResult is JoinResult.Success)

        val success = joinResult as JoinResult.Success
        assertEquals("new-shelf-id", success.localShelfId)
        assertEquals("Science Fiction Club (Book Club)", success.shelfName)
    }

    @Test
    fun `calls repository joinBookClub with correct code`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockRepository.configureNotMember()
        mockRepository.configureBookClub(
            BookClub(
                code = "CLUB5678",
                name = "Mystery Club",
                style = ShelfStyle.DarkWood,
                createdAt = System.currentTimeMillis(),
                createdBy = "creator-id",
                createdByName = "Creator",
                bookCount = 10,
                memberCount = 3
            )
        )
        mockRepository.configureJoinSuccess("shelf-id", "Mystery Club (Book Club)")

        // When
        useCase("CLUB5678")

        // Then
        assertTrue("Should call joinBookClub", mockRepository.joinBookClubCalled)
        assertEquals("CLUB5678", mockRepository.lastJoinCode)
    }

    // ========== Error Propagation Tests ==========

    @Test
    fun `propagates membership check error`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockRepository.isMemberResult = Result.Error(DataError.Sync.NETWORK_ERROR)
        val code = "TEST1234"

        // When
        val result = useCase(code)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `propagates getBookClub error`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockRepository.configureNotMember()
        mockRepository.getBookClubResult = Result.Error(DataError.Sync.PERMISSION_DENIED)
        val code = "TEST1234"

        // When
        val result = useCase(code)

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.PERMISSION_DENIED, (result as Result.Error).error)
    }

    @Test
    fun `propagates joinBookClub error`() = runTest {
        // Given
        mockAuthService.configureSignedIn()
        mockRepository.configureNotMember()
        mockRepository.configureBookClub(
            BookClub(
                code = "TEST1234",
                name = "Test Club",
                style = ShelfStyle.DarkWood,
                createdAt = System.currentTimeMillis(),
                createdBy = "creator-id",
                createdByName = "Creator",
                bookCount = 0,
                memberCount = 1
            )
        )
        mockRepository.configureJoinError(DataError.Sync.QUOTA_EXCEEDED)

        // When
        val result = useCase("TEST1234")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.QUOTA_EXCEEDED, (result as Result.Error).error)
    }

    // ========== Edge Cases ==========

    @Test
    fun `handles member in Firestore but no local shelf gracefully`() = runTest {
        // Given - edge case where Firestore says member but local data is missing
        mockAuthService.configureSignedIn()
        mockRepository.isMemberResult = Result.Success(true)
        mockRepository.localShelfForClub = null // No local shelf found
        mockRepository.configureBookClub(
            BookClub(
                code = "TEST1234",
                name = "Orphaned Club",
                style = ShelfStyle.DarkWood,
                createdAt = System.currentTimeMillis(),
                createdBy = "creator-id",
                createdByName = "Creator",
                bookCount = 3,
                memberCount = 2
            )
        )
        mockRepository.configureJoinSuccess("recreated-shelf-id", "Orphaned Club (Book Club)")

        // When
        val result = useCase("TEST1234")

        // Then
        // Should proceed with join to recreate local data
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call joinBookClub to recreate local data", mockRepository.joinBookClubCalled)
    }
}
