package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetCurrentUserIdUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookcase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ClearUserDataUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.AddBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.BookClubUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.CreateBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.DeleteBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.DeleteBookClubReviewUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.EditBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GenerateInviteLinkUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubCommentsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubPreviewUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubReviewsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.JoinBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.JoinResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.LeaveBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.ParseClubCodeUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.RestoreBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.RestoreResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.SyncBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.UpsertBookClubReviewUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.ValidateBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.HandleTutorialAccessUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.TutorialAccessResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfManagementHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfOperationsHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookclub.handlers.BookClubOperationsHandler
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookClubRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockCreateShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockDeleteShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockDuplicateShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetAllShelvesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRenameShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockReorderShelvesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockShareBookshelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockSyncRepository
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockUpdateShelfStyleUseCase
import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateInfo
import uk.co.zlurgg.mybookshelf.update.domain.usecases.CheckForUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DismissUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DownloadUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.GetCurrentVersionInfoUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.UpdateUseCases

/**
 * ViewModel test demonstrating UI state testing with simplified inline mocks.
 * Tests focus on presentation logic and state changes, not business logic.
 * Business logic is tested in UseCase layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookcaseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Shared mocks for testing
    private val mockGetAllShelves = MockGetAllShelvesUseCase()
    private val mockCreateShelf = MockCreateShelfUseCase()
    private val mockDeleteShelf = MockDeleteShelfUseCase()
    private val mockReorderShelves = MockReorderShelvesUseCase()
    private val mockRenameShelf = MockRenameShelfUseCase()
    private val mockUpdateShelfStyle = MockUpdateShelfStyleUseCase()

    @After
    fun tearDown() {
        mockCreateShelf.reset()
        mockDeleteShelf.reset()
        mockReorderShelves.reset()
        mockRenameShelf.reset()
        mockUpdateShelfStyle.reset()
    }

    private fun createViewModel(): BookcaseViewModel {
        val useCases = BookcaseUseCases(
            getAllShelves = mockGetAllShelves,
            createShelf = mockCreateShelf,
            deleteShelf = mockDeleteShelf,
            reorderShelves = mockReorderShelves,
            getShelfById = MockGetShelfByIdUseCase(),
            renameShelf = mockRenameShelf,
            updateShelfStyle = mockUpdateShelfStyle,
            duplicateShelf = MockDuplicateShelfUseCase(),
            shareShelf = MockShareBookshelfUseCase()
        )
        val mockHandleTutorialAccess = object : HandleTutorialAccessUseCase {
            override suspend operator fun invoke(): Result<TutorialAccessResult, DataError.Local> {
                return Result.Success(TutorialAccessResult.DoNotNavigate)
            }
        }

        val shelfOperations = ShelfOperationsHandler(useCases)
        val mockBookClubRepository = MockBookClubRepository()
        val shelfManagement = ShelfManagementHandler(useCases, mockHandleTutorialAccess, mockBookClubRepository)

        // No-op update use cases for testing
        val mockCheckForUpdate = object : CheckForUpdateUseCase {
            override suspend operator fun invoke(forceCheck: Boolean): UpdateInfo? = null
        }
        val mockDownloadUpdate = object : DownloadUpdateUseCase {
            override operator fun invoke(updateInfo: UpdateInfo): Long? = null
        }
        val mockDismissUpdate = object : DismissUpdateUseCase {
            override suspend operator fun invoke(version: String) = Unit
        }
        val mockGetCurrentVersionInfo = object : GetCurrentVersionInfoUseCase {
            override suspend operator fun invoke(): UpdateInfo? = null
        }
        val mockAuthService = object : uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService {
            override suspend fun signIn() = Result.Success(
                uk.co.zlurgg.mybookshelf.auth.domain.model.UserData("test", null, null)
            )
            override suspend fun signOut() = Result.Success(Unit)
            override fun getSignedInUser() = null
        }
        val mockAuthStateRepository = object : uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository {
            override suspend fun isSignedIn(): Result<Boolean, DataError.Local> = Result.Success(false)
            override suspend fun setSignedInState(isSignedIn: Boolean): Result<Unit, DataError.Local> =
                Result.Success(Unit)
        }
        val mockSyncScheduler = object : SyncSchedulerService {
            override fun schedulePeriodicSync() = Unit
            override fun triggerImmediateSync() = Unit
            override fun cancelAllSync() = Unit
        }
        val mockClearUserData = object : ClearUserDataUseCase {
            override suspend operator fun invoke(userId: String): Result<Int, DataError.Local> = Result.Success(0)
        }
        val mockCurrentUserProvider = object : CurrentUserProvider {
            override fun getCurrentUserId(): String = "test-user-id"
        }
        val mockSyncRepository = MockSyncRepository()
        val mockSignIn = SignInUseCaseImpl(mockAuthService, mockAuthStateRepository, mockSyncScheduler)
        val mockCheckSignInStatus = CheckSignInStatusUseCaseImpl(mockAuthService, mockAuthStateRepository)
        val mockSignOut = SignOutUseCaseImpl(
            mockAuthService,
            mockAuthStateRepository,
            mockSyncScheduler,
            mockClearUserData,
            mockCurrentUserProvider,
            mockSyncRepository
        )
        val mockGetCurrentUserIdUseCase = object : GetCurrentUserIdUseCase {
            override operator fun invoke(): String = "test-user-id"
        }

        // Facades
        val updateUseCases = UpdateUseCases(
            checkForUpdate = mockCheckForUpdate,
            downloadUpdate = mockDownloadUpdate,
            dismissUpdate = mockDismissUpdate,
            getCurrentVersionInfo = mockGetCurrentVersionInfo
        )
        val authUseCases = AuthUseCases(
            signIn = mockSignIn,
            signOut = mockSignOut,
            checkSignInStatus = mockCheckSignInStatus,
            getCurrentUserId = mockGetCurrentUserIdUseCase
        )

        // Book Club operations handler
        val mockCreateBookClub = object : CreateBookClubUseCase {
            override suspend operator fun invoke(shelfId: String): Result<String, DataError.Sync> =
                Result.Success("ABC12345")
        }
        val mockGenerateInviteLink = object : GenerateInviteLinkUseCase {
            override operator fun invoke(clubCode: String, clubName: String?): String =
                "https://mybookshelf.app/join/$clubCode"
        }
        val mockParseClubCode = object : ParseClubCodeUseCase {
            override fun invoke(input: String): Result<String, DataError.Validation> =
                Result.Success("TESTCODE")
        }
        val mockGetBookClubPreview = object : GetBookClubPreviewUseCase {
            override suspend fun invoke(code: String): Result<BookClub?, DataError.Sync> =
                Result.Success(null)
        }
        val mockJoinBookClub = object : JoinBookClubUseCase {
            override suspend fun invoke(code: String): Result<JoinResult, DataError.Sync> =
                Result.Success(JoinResult.Success("shelf-id", "Test Shelf"))
        }
        val mockRestoreBookClubMemberships = object : RestoreBookClubMembershipsUseCase {
            override suspend fun invoke(): Result<RestoreResult, DataError.Sync> =
                Result.Success(RestoreResult(0, 0))
        }
        val mockSyncBookClub = object : SyncBookClubUseCase {
            override suspend operator fun invoke(
                clubCode: String,
                localShelfId: String,
            ): Result<SyncResult, DataError.Sync> = Result.Success(SyncResult(0, 0))
        }
        val mockLeaveBookClub = object : LeaveBookClubUseCase {
            override suspend fun invoke(shelfId: String): Result<Unit, DataError.Sync> =
                Result.Success(Unit)
        }
        val mockValidateMemberships = object : ValidateBookClubMembershipsUseCase {
            override suspend fun invoke(): Result<List<String>, DataError.Sync> =
                Result.Success(emptyList())
        }
        val mockGetBookClubReviews = object : GetBookClubReviewsUseCase {
            override suspend fun invoke(
                clubCode: String,
                bookId: String
            ): Result<List<BookClubReview>, DataError.Sync> =
                Result.Success(emptyList())
        }
        val mockUpsertBookClubReview = object : UpsertBookClubReviewUseCase {
            override suspend fun invoke(
                clubCode: String,
                bookId: String,
                rating: Float,
                reviewText: String
            ): Result<Unit, DataError.Sync> =
                Result.Success(Unit)
        }
        val mockDeleteBookClubReview = object : DeleteBookClubReviewUseCase {
            override suspend fun invoke(clubCode: String, bookId: String): Result<Unit, DataError.Sync> =
                Result.Success(Unit)
        }
        val mockGetBookClubComments = object : GetBookClubCommentsUseCase {
            override suspend fun invoke(
                clubCode: String,
                bookId: String
            ): Result<List<BookClubComment>, DataError.Sync> =
                Result.Success(emptyList())
        }
        val mockAddBookClubComment = object : AddBookClubCommentUseCase {
            override suspend fun invoke(
                clubCode: String,
                bookId: String,
                text: String
            ): Result<String, DataError.Sync> =
                Result.Success("comment-id")
        }
        val mockEditBookClubComment = object : EditBookClubCommentUseCase {
            override suspend fun invoke(
                clubCode: String,
                bookId: String,
                commentId: String,
                newText: String
            ): Result<Unit, DataError.Sync> =
                Result.Success(Unit)
        }
        val mockDeleteBookClubComment = object : DeleteBookClubCommentUseCase {
            override suspend fun invoke(
                clubCode: String,
                bookId: String,
                commentId: String
            ): Result<Unit, DataError.Sync> =
                Result.Success(Unit)
        }
        val bookClubUseCases = BookClubUseCases(
            createBookClub = mockCreateBookClub,
            generateInviteLink = mockGenerateInviteLink,
            parseClubCode = mockParseClubCode,
            getBookClubPreview = mockGetBookClubPreview,
            joinBookClub = mockJoinBookClub,
            syncBookClub = mockSyncBookClub,
            restoreBookClubMemberships = mockRestoreBookClubMemberships,
            leaveBookClub = mockLeaveBookClub,
            validateMemberships = mockValidateMemberships,
            getBookClubReviews = mockGetBookClubReviews,
            upsertBookClubReview = mockUpsertBookClubReview,
            deleteBookClubReview = mockDeleteBookClubReview,
            getBookClubComments = mockGetBookClubComments,
            addBookClubComment = mockAddBookClubComment,
            editBookClubComment = mockEditBookClubComment,
            deleteBookClubComment = mockDeleteBookClubComment
        )
        val bookClubOperations = BookClubOperationsHandler(bookClubUseCases)

        return BookcaseViewModel(
            shelfOperations,
            shelfManagement,
            useCases,
            bookClubOperations,
            updateUseCases,
            authUseCases
        )
    }

    @Test
    fun `ShowAddDialog action toggles dialog visibility`() = runTest(testDispatcher) {
        // Given
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When - show dialog
        val stateAfterShow = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowAddDialog(true))
        }

        // Then
        assertTrue("Should show dialog", stateAfterShow?.showAddDialog == true)

        // When - hide dialog
        val stateAfterHide = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowAddDialog(false))
        }

        // Then
        assertFalse("Should hide dialog", stateAfterHide?.showAddDialog == true)
        stateHelper.cleanup()
    }

    @Test
    fun `ToggleReorderMode changes reorder state`() = runTest(testDispatcher) {
        // Given
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Initial state should be false
        val initialState = stateHelper.awaitState()
        assertFalse("Should start with reorder mode off", initialState?.isReorderMode == true)

        // When - toggle to enable
        val stateAfterEnable = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ToggleReorderMode)
        }

        // Then
        assertTrue("Should enter reorder mode", stateAfterEnable?.isReorderMode == true)

        // When - toggle again to disable
        val stateAfterDisable = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ToggleReorderMode)
        }

        // Then
        assertFalse("Should exit reorder mode", stateAfterDisable?.isReorderMode == true)
        stateHelper.cleanup()
    }

    @Test
    fun `ResetOperationState clears error and success flags`() = runTest(testDispatcher) {
        // Given
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When - reset operation state
        val stateAfterReset = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ResetOperationState)
        }

        // Then
        assertFalse("Should clear operation success", stateAfterReset?.operationSuccess == true)
        assertTrue("Should clear error message", stateAfterReset?.errorMessage == null)
        stateHelper.cleanup()
    }

    @Test
    fun `delete shelf handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testShelf = TestShelfBuilder().withId("shelf-1").withName("Test Shelf").build()
        val bookcase = Bookcase(id = "bookcase", bookshelves = listOf(testShelf), bookCounts = emptyMap())
        mockGetAllShelves.configureBookcase(bookcase)
        mockDeleteShelf.shouldReturnError = true

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterDelete = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.OnRemoveBookShelf(testShelf))
        }

        // Then
        assertNotNull("Should set error message", stateAfterDelete?.errorMessage)
        assertTrue(
            "Should contain operation context",
            stateAfterDelete?.errorMessage?.contains("Failed to remove shelf") == true
        )
        // Shelf should be reverted back to the list
        assertTrue(
            "Should revert shelf removal",
            stateAfterDelete?.bookshelves?.any { it.id == "shelf-1" } == true
        )
        stateHelper.cleanup()
    }

    @Test
    fun `restore shelf handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testShelf = TestShelfBuilder().withId("shelf-1").withName("Test Shelf").build()
        val bookcase = Bookcase(id = "bookcase", bookshelves = listOf(testShelf), bookCounts = emptyMap())
        mockGetAllShelves.configureBookcase(bookcase)

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // Remove shelf first (will succeed)
        mockDeleteShelf.shouldReturnError = false
        stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.OnRemoveBookShelf(testShelf))
        }

        // Now try to restore with error
        mockDeleteShelf.shouldReturnError = true

        // When
        val stateAfterRestore = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.OnUndoRemove(testShelf))
        }

        // Then
        assertNotNull("Should set error message", stateAfterRestore?.errorMessage)
        assertTrue(
            "Should contain operation context",
            stateAfterRestore?.errorMessage?.contains("Failed to restore shelf") == true
        )
        stateHelper.cleanup()
    }

    @Test
    fun `add shelf handles error correctly`() = runTest(testDispatcher) {
        // Given
        mockCreateShelf.shouldReturnError = true

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterAdd = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.OnAddBookshelfClick("New Shelf", ShelfStyle.DarkWood))
        }

        // Then
        assertNotNull("Should set error message", stateAfterAdd?.errorMessage)
        assertTrue(
            "Should contain operation context",
            stateAfterAdd?.errorMessage?.contains("Failed to add shelf") == true
        )
        assertFalse("Should clear loading flag", stateAfterAdd?.isLoading == true)
        stateHelper.cleanup()
    }

    // Note: Reorder shelf error test skipped - after error, ViewModel reloads from Flow
    // which complicates testing. Error handling code path is validated in ReorderShelvesUseCaseTest.

    // Note: Load shelves error test skipped - tested via Flow catch in init block,
    // requires complex Flow error mocking. Error handling code path validated by other tests.

    @Test
    fun `ShowRenameDialog action sets shelf to rename and shows dialog`() = runTest(testDispatcher) {
        // Given
        val testShelf = TestShelfBuilder().withId("shelf-1").withName("Test Shelf").build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When - show rename dialog
        val stateAfterShow = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowRenameDialog(testShelf))
        }

        // Then
        assertTrue("Should show rename dialog", stateAfterShow?.showRenameDialog == true)
        assertTrue("Should set shelf to rename", stateAfterShow?.shelfToRename?.id == "shelf-1")
        stateHelper.cleanup()
    }

    @Test
    fun `DismissRenameDialog action hides dialog and clears shelf`() = runTest(testDispatcher) {
        // Given
        val testShelf = TestShelfBuilder().withId("shelf-1").withName("Test Shelf").build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Show dialog first
        stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowRenameDialog(testShelf))
        }

        // When - dismiss dialog
        val stateAfterDismiss = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.DismissRenameDialog)
        }

        // Then
        assertFalse("Should hide rename dialog", stateAfterDismiss?.showRenameDialog == true)
        assertTrue("Should clear shelf to rename", stateAfterDismiss?.shelfToRename == null)
        stateHelper.cleanup()
    }

    @Test
    fun `rename shelf success updates shelf name in list`() = runTest(testDispatcher) {
        // Given
        val testShelf = TestShelfBuilder().withId("shelf-1").withName("Old Name").build()
        val bookcase = Bookcase(id = "bookcase", bookshelves = listOf(testShelf), bookCounts = emptyMap())
        mockGetAllShelves.configureBookcase(bookcase)

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When - rename shelf
        val stateAfterRename = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.OnRenameShelf("shelf-1", "New Name"))
        }

        // Then
        val renamedShelf = stateAfterRename?.bookshelves?.find { it.id == "shelf-1" }
        assertTrue("Should update shelf name", renamedShelf?.name == "New Name")
        assertFalse("Should hide rename dialog", stateAfterRename?.showRenameDialog == true)
        assertTrue("Should set operation success", stateAfterRename?.operationSuccess == true)
        stateHelper.cleanup()
    }

    @Test
    fun `rename shelf handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testShelf = TestShelfBuilder().withId("shelf-1").withName("Old Name").build()
        val bookcase = Bookcase(id = "bookcase", bookshelves = listOf(testShelf), bookCounts = emptyMap())
        mockGetAllShelves.configureBookcase(bookcase)
        mockRenameShelf.shouldReturnError = true
        mockRenameShelf.errorToReturn = DataError.Local.VALIDATION_ERROR

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // Show rename dialog first
        stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowRenameDialog(testShelf))
        }

        // When - rename shelf with error
        val stateAfterRename = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.OnRenameShelf("shelf-1", "")) // Empty name causes error
        }

        // Then - Error should be inline in dialog, not global snackbar
        assertNotNull("Should set inline rename error", stateAfterRename?.renameError)
        assertTrue(
            "Should contain operation context",
            stateAfterRename?.renameError?.contains("Failed to rename shelf") == true
        )
        assertTrue("Dialog should stay open to show error", stateAfterRename?.showRenameDialog == true)
        val shelf = stateAfterRename?.bookshelves?.find { it.id == "shelf-1" }
        assertTrue("Should not change shelf name", shelf?.name == "Old Name")
        stateHelper.cleanup()
    }

    @Test
    fun `dismissing rename dialog clears inline error`() = runTest(testDispatcher) {
        // Given
        val testShelf = TestShelfBuilder().withId("shelf-1").withName("Old Name").build()
        val bookcase = Bookcase(id = "bookcase", bookshelves = listOf(testShelf), bookCounts = emptyMap())
        mockGetAllShelves.configureBookcase(bookcase)
        mockRenameShelf.shouldReturnError = true
        mockRenameShelf.errorToReturn = DataError.Local.VALIDATION_ERROR

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // Show rename dialog
        stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowRenameDialog(testShelf))
        }

        // Trigger error
        stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.OnRenameShelf("shelf-1", ""))
        }

        // When - dismiss dialog
        val stateAfterDismiss = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.DismissRenameDialog)
        }

        // Then
        assertFalse("Should hide rename dialog", stateAfterDismiss?.showRenameDialog == true)
        assertTrue("Should clear shelf to rename", stateAfterDismiss?.shelfToRename == null)
        assertTrue("Should clear inline rename error", stateAfterDismiss?.renameError == null)
        stateHelper.cleanup()
    }
}
