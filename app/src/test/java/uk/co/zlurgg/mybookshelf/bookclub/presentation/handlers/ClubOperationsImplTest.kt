package uk.co.zlurgg.mybookshelf.bookclub.presentation.handlers

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.BookClubCreationResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.JoinResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.LookupResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.SyncResult
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.BookClubOperationUseCases
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.CreateBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetBookClubPreviewUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.JoinBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.LeaveBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.ParseClubCodeUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreResult
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.SyncBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.ValidateBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.SyncResult as DomainSyncResult
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.JoinResult as DomainJoinResult

class ClubOperationsImplTest {

    private val mockRepository = MockBookClubRepository()

    // Configurable use case stubs
    private var createBookClubResult: Result<String, DataError.Sync> = Result.Success("TESTCODE")
    private var parseClubCodeResult: Result<String, DataError.Validation> = Result.Success("TESTCODE")
    private var getBookClubPreviewResult: Result<BookClub?, DataError.Sync> = Result.Success(null)
    private var joinBookClubResult: Result<DomainJoinResult, DataError.Sync> =
        Result.Success(DomainJoinResult.Success("shelf-1", "Test Club"))
    private var syncBookClubResult: Result<DomainSyncResult, DataError.Sync> =
        Result.Success(DomainSyncResult(0, 0))
    private var leaveBookClubResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    private var validateMembershipsResult: Result<List<String>, DataError.Sync> = Result.Success(emptyList())

    private var lastJoinCode: String? = null
    private var lastSyncClubCode: String? = null
    private var lastSyncShelfId: String? = null

    private fun createClubOperations(): ClubOperationsImpl {
        val useCases = BookClubOperationUseCases(
            createBookClub = object : CreateBookClubUseCase {
                override suspend fun invoke(shelfId: String) = createBookClubResult
            },
            parseClubCode = object : ParseClubCodeUseCase {
                override fun invoke(input: String) = parseClubCodeResult
            },
            getBookClubPreview = object : GetBookClubPreviewUseCase {
                override suspend fun invoke(code: String) = getBookClubPreviewResult
            },
            joinBookClub = object : JoinBookClubUseCase {
                override suspend fun invoke(code: String): Result<DomainJoinResult, DataError.Sync> {
                    lastJoinCode = code
                    return joinBookClubResult
                }
            },
            syncBookClub = object : SyncBookClubUseCase {
                override suspend fun invoke(
                    clubCode: String,
                    localShelfId: String
                ): Result<DomainSyncResult, DataError.Sync> {
                    lastSyncClubCode = clubCode
                    lastSyncShelfId = localShelfId
                    return syncBookClubResult
                }
            },
            restoreBookClubMemberships = object : RestoreBookClubMembershipsUseCase {
                override suspend fun invoke() = Result.Success(RestoreResult(0, 0))
            },
            leaveBookClub = object : LeaveBookClubUseCase {
                override suspend fun invoke(shelfId: String) = leaveBookClubResult
            },
            validateMemberships = object : ValidateBookClubMembershipsUseCase {
                override suspend fun invoke() = validateMembershipsResult
            }
        )
        return ClubOperationsImpl(useCases, mockRepository)
    }

    @After
    fun tearDown() {
        mockRepository.reset()
        createBookClubResult = Result.Success("TESTCODE")
        parseClubCodeResult = Result.Success("TESTCODE")
        getBookClubPreviewResult = Result.Success(null)
        joinBookClubResult = Result.Success(
            DomainJoinResult.Success("shelf-1", "Test Club")
        )
        syncBookClubResult = Result.Success(DomainSyncResult(0, 0))
        leaveBookClubResult = Result.Success(Unit)
        validateMembershipsResult = Result.Success(emptyList())
        lastJoinCode = null
        lastSyncClubCode = null
        lastSyncShelfId = null
    }

    // ========== lookupBookClub Tests ==========

    @Test
    fun `lookupBookClub - when code is invalid - returns InvalidCode`() = runTest {
        // Given
        parseClubCodeResult = Result.Error(DataError.Validation.INVALID_CLUB_CODE)
        val ops = createClubOperations()

        // When
        val result = ops.lookupBookClub("bad-code")

        // Then
        assertTrue("Should be InvalidCode", result is LookupResult.InvalidCode)
        assertEquals(
            DataError.Validation.INVALID_CLUB_CODE,
            (result as LookupResult.InvalidCode).error
        )
    }

    @Test
    fun `lookupBookClub - when club found - returns Found with correct data`() = runTest {
        // Given
        parseClubCodeResult = Result.Success("ABC123")
        getBookClubPreviewResult = Result.Success(
            BookClub(
                code = "ABC123",
                name = "Reading Circle",
                style = ShelfStyle.DarkWood,
                createdAt = 1000L,
                createdBy = "user-1",
                createdByName = "Alice",
                bookCount = 10,
                memberCount = 5
            )
        )
        val ops = createClubOperations()

        // When
        val result = ops.lookupBookClub("ABC123")

        // Then
        assertTrue("Should be Found", result is LookupResult.Found)
        val found = result as LookupResult.Found
        assertEquals("Reading Circle", found.clubName)
        assertEquals("ABC123", found.clubCode)
        assertEquals(5, found.memberCount)
    }

    @Test
    fun `lookupBookClub - when club not found - returns NotFound`() = runTest {
        // Given
        parseClubCodeResult = Result.Success("XYZ999")
        getBookClubPreviewResult = Result.Success(null)
        val ops = createClubOperations()

        // When
        val result = ops.lookupBookClub("XYZ999")

        // Then
        assertTrue("Should be NotFound", result is LookupResult.NotFound)
        assertEquals(DataError.Sync.CLUB_NOT_FOUND, (result as LookupResult.NotFound).error)
    }

    @Test
    fun `lookupBookClub - when preview returns error - returns NotFound with error`() = runTest {
        // Given
        parseClubCodeResult = Result.Success("ABC123")
        getBookClubPreviewResult = Result.Error(DataError.Sync.NETWORK_ERROR)
        val ops = createClubOperations()

        // When
        val result = ops.lookupBookClub("ABC123")

        // Then
        assertTrue("Should be NotFound", result is LookupResult.NotFound)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as LookupResult.NotFound).error)
    }

    // ========== lastLookedUpCode State Management Tests ==========

    @Test
    fun `joinBookClub no-arg - when no prior lookup - returns CLUB_NOT_FOUND`() = runTest {
        // Given — fresh instance, no lookup performed
        val ops = createClubOperations()

        // When
        val result = ops.joinBookClub()

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.CLUB_NOT_FOUND, (result as Result.Error).error)
    }

    @Test
    fun `joinBookClub no-arg - after successful lookup - uses stored code`() = runTest {
        // Given
        parseClubCodeResult = Result.Success("LOOKED_UP")
        getBookClubPreviewResult = Result.Success(
            BookClub("LOOKED_UP", "Club", ShelfStyle.DarkWood, 0L, "u", "User", 0, 1)
        )
        val ops = createClubOperations()
        ops.lookupBookClub("LOOKED_UP")

        // When
        ops.joinBookClub()

        // Then
        assertEquals("Should join with looked-up code", "LOOKED_UP", lastJoinCode)
    }

    @Test
    fun `clearLookupState - after lookup - no-arg join returns error`() = runTest {
        // Given
        parseClubCodeResult = Result.Success("CODE_A")
        getBookClubPreviewResult = Result.Success(
            BookClub("CODE_A", "Club A", ShelfStyle.DarkWood, 0L, "u", "User", 0, 1)
        )
        val ops = createClubOperations()
        ops.lookupBookClub("CODE_A")

        // When
        ops.clearLookupState()
        val result = ops.joinBookClub()

        // Then
        assertTrue("Should return error after clearing", result is Result.Error)
        assertEquals(DataError.Sync.CLUB_NOT_FOUND, (result as Result.Error).error)
    }

    @Test
    fun `lastLookedUpCode - lookup A then lookup B - no-arg join uses B (last wins)`() = runTest {
        // Given
        val ops = createClubOperations()

        // Lookup A
        parseClubCodeResult = Result.Success("CODE_A")
        getBookClubPreviewResult = Result.Success(
            BookClub("CODE_A", "Club A", ShelfStyle.DarkWood, 0L, "u", "User", 0, 1)
        )
        ops.lookupBookClub("CODE_A")

        // Lookup B
        parseClubCodeResult = Result.Success("CODE_B")
        getBookClubPreviewResult = Result.Success(
            BookClub("CODE_B", "Club B", ShelfStyle.DarkWood, 0L, "u", "User", 0, 2)
        )
        ops.lookupBookClub("CODE_B")

        // When
        ops.joinBookClub()

        // Then
        assertEquals("Should join with last looked-up code", "CODE_B", lastJoinCode)
    }

    @Test
    fun `lastLookedUpCode - lookup A succeeds then lookup B fails - stale code A remains`() = runTest {
        // Stale state: failed lookup does NOT clear previously stored code.
        // This matches current behavior — see @Volatile comment in ClubOperationsImpl.
        val ops = createClubOperations()

        // Lookup A succeeds
        parseClubCodeResult = Result.Success("CODE_A")
        getBookClubPreviewResult = Result.Success(
            BookClub("CODE_A", "Club A", ShelfStyle.DarkWood, 0L, "u", "User", 0, 1)
        )
        ops.lookupBookClub("CODE_A")

        // Lookup B fails (club not found)
        parseClubCodeResult = Result.Success("CODE_B")
        getBookClubPreviewResult = Result.Success(null)
        ops.lookupBookClub("CODE_B")

        // When — no-arg join uses stale CODE_A
        ops.joinBookClub()

        // Then
        assertEquals("Stale code from first lookup is used", "CODE_A", lastJoinCode)
    }

    @Test
    fun `joinBookClub with code - sets lastLookedUpCode as side effect`() = runTest {
        // Given
        val ops = createClubOperations()

        // When — join with explicit code
        ops.joinBookClub("DIRECT_CODE")

        // Then — subsequent no-arg join should use that code
        ops.joinBookClub()
        assertEquals("Should reuse code from previous join", "DIRECT_CODE", lastJoinCode)
    }

    // ========== joinBookClub(code) Tests ==========

    @Test
    fun `joinBookClub with code - success - maps to service JoinResult Success`() = runTest {
        // Given
        joinBookClubResult = Result.Success(
            DomainJoinResult.Success("shelf-1", "My Club")
        )
        val ops = createClubOperations()

        // When
        val result = ops.joinBookClub("CODE123")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val joinResult = (result as Result.Success).data
        assertTrue("Should be JoinResult.Success", joinResult is JoinResult.Success)
        assertEquals("My Club", (joinResult as JoinResult.Success).shelfName)
    }

    @Test
    fun `joinBookClub with code - already member - maps to service JoinResult AlreadyMember`() = runTest {
        // Given
        joinBookClubResult = Result.Success(
            DomainJoinResult.AlreadyMember("shelf-1")
        )
        val ops = createClubOperations()

        // When
        val result = ops.joinBookClub("CODE123")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val joinResult = (result as Result.Success).data
        assertTrue("Should be JoinResult.AlreadyMember", joinResult is JoinResult.AlreadyMember)
    }

    @Test
    fun `joinBookClub with code - error - propagates`() = runTest {
        // Given
        joinBookClubResult = Result.Error(DataError.Sync.NOT_SIGNED_IN)
        val ops = createClubOperations()

        // When
        val result = ops.joinBookClub("CODE123")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NOT_SIGNED_IN, (result as Result.Error).error)
    }

    // ========== createBookClub Tests ==========

    @Test
    fun `createBookClub - success - wraps code in BookClubCreationResult`() = runTest {
        // Given
        createBookClubResult = Result.Success("NEW_CODE")
        val ops = createClubOperations()

        // When
        val result = ops.createBookClub("shelf-1", "My Shelf")

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals(
            BookClubCreationResult("NEW_CODE"),
            (result as Result.Success).data
        )
    }

    @Test
    fun `createBookClub - error - propagates`() = runTest {
        // Given
        createBookClubResult = Result.Error(DataError.Sync.NOT_SIGNED_IN)
        val ops = createClubOperations()

        // When
        val result = ops.createBookClub("shelf-1", "My Shelf")

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NOT_SIGNED_IN, (result as Result.Error).error)
    }

    // ========== Use Case Delegation Tests ==========

    @Test
    fun `syncBooksFromClub - maps domain SyncResult to service SyncResult`() = runTest {
        // Given
        syncBookClubResult = Result.Success(DomainSyncResult(booksAdded = 3, booksRemoved = 1))
        val ops = createClubOperations()

        // When
        val result = ops.syncBooksFromClub("CLUB1", "shelf-1")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val syncResult = (result as Result.Success).data
        assertEquals(SyncResult(booksAdded = 3, booksRemoved = 1), syncResult)
        assertEquals("CLUB1", lastSyncClubCode)
        assertEquals("shelf-1", lastSyncShelfId)
    }

    @Test
    fun `leaveBookClub - delegates to use case`() = runTest {
        // Given
        leaveBookClubResult = Result.Success(Unit)
        val ops = createClubOperations()

        // When
        val result = ops.leaveBookClub("shelf-1")

        // Then
        assertTrue("Should return success", result is Result.Success)
    }

    @Test
    fun `validateMemberships - success - returns club names`() = runTest {
        // Given
        validateMembershipsResult = Result.Success(listOf("Deleted Club A", "Deleted Club B"))
        val ops = createClubOperations()

        // When
        val result = ops.validateMemberships()

        // Then
        assertEquals(listOf("Deleted Club A", "Deleted Club B"), result)
    }

    @Test
    fun `validateMemberships - error - returns empty list`() = runTest {
        // Given
        validateMembershipsResult = Result.Error(DataError.Sync.NETWORK_ERROR)
        val ops = createClubOperations()

        // When
        val result = ops.validateMemberships()

        // Then
        assertEquals(emptyList<String>(), result)
    }

    // ========== Repository Pass-Through Test ==========

    @Test
    fun `deleteBookClub - delegates to repository`() = runTest {
        // Given — representative test for all repository pass-through methods
        mockRepository.deleteBookClubResult = Result.Success(Unit)
        val ops = createClubOperations()

        // When
        val result = ops.deleteBookClub("CLUB_CODE")

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertTrue("Should call repository", mockRepository.deleteBookClubCalled)
        assertEquals("CLUB_CODE", mockRepository.lastDeleteCode)
    }
}
