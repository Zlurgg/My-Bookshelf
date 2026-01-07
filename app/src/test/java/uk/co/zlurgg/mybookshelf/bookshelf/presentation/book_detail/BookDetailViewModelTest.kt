package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookDetailsWithShelfStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.GetBookDetailsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.ToggleBookPurchaseUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpdateBookMetadataUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.AddBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.BookClubUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.CreateBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.DeleteBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.DeleteBookClubReviewUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.EditBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubCommentsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GenerateInviteLinkUseCase
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
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.helpers.testHelper

/**
 * ViewModel test demonstrating UI state testing with simplified inline mocks.
 * Tests focus on presentation logic and state changes, not business logic.
 * Business logic is tested in UseCase layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Simplified inline mocks for ViewModel UI testing
    private val mockGetBookDetails = SimpleGetBookDetailsUseCase()
    private val mockAddBookToShelf = SimpleAddBookToShelfUseCase()
    private val mockRemoveBookFromShelf = SimpleRemoveBookFromShelfUseCase()
    private val mockUpsertBook = SimpleUpsertBookUseCase()
    private val mockToggleBookPurchase = SimpleToggleBookPurchaseUseCase()
    private val mockUpdateBookMetadata = SimpleUpdateBookMetadataUseCase()

    @After
    fun tearDown() {
        mockGetBookDetails.reset()
        mockAddBookToShelf.reset()
        mockRemoveBookFromShelf.reset()
        mockUpsertBook.reset()
        mockToggleBookPurchase.reset()
        mockUpdateBookMetadata.reset()
    }

    private val mockAuthService = SimpleAuthService()

    private fun createViewModel(): BookDetailViewModel {
        val useCases = BookDetailUseCases(
            getBookDetails = mockGetBookDetails,
            addBookToShelf = mockAddBookToShelf,
            removeBookFromShelf = mockRemoveBookFromShelf,
            upsertBook = mockUpsertBook,
            toggleBookPurchase = mockToggleBookPurchase,
            updateBookMetadata = mockUpdateBookMetadata
        )
        val bookClubUseCases = BookClubUseCases(
            createBookClub = SimpleCreateBookClubUseCase(),
            generateInviteLink = SimpleGenerateInviteLinkUseCase(),
            parseClubCode = SimpleParseClubCodeUseCase(),
            getBookClubPreview = SimpleGetBookClubPreviewUseCase(),
            joinBookClub = SimpleJoinBookClubUseCase(),
            syncBookClub = SimpleSyncBookClubUseCase(),
            restoreBookClubMemberships = SimpleRestoreBookClubMembershipsUseCase(),
            leaveBookClub = SimpleLeaveBookClubUseCase(),
            validateMemberships = SimpleValidateBookClubMembershipsUseCase(),
            getBookClubReviews = SimpleGetBookClubReviewsUseCase(),
            upsertBookClubReview = SimpleUpsertBookClubReviewUseCase(),
            deleteBookClubReview = SimpleDeleteBookClubReviewUseCase(),
            getBookClubComments = SimpleGetBookClubCommentsUseCase(),
            addBookClubComment = SimpleAddBookClubCommentUseCase(),
            editBookClubComment = SimpleEditBookClubCommentUseCase(),
            deleteBookClubComment = SimpleDeleteBookClubCommentUseCase()
        )
        return BookDetailViewModel(useCases, bookClubUseCases, mockAuthService, "book-1", "test-shelf")
    }

    @Test
    fun `initial book load populates state correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = false)

        // When
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)
        val initialState = stateHelper.awaitState()

        // Then
        assertEquals("Should load book", testBook, initialState?.book)
        assertFalse("Should set onShelf status", initialState?.onShelf == true)
        assertFalse("Should clear loading flag", initialState?.isLoading == true)
        stateHelper.cleanup()
    }

    @Test
    fun `add book to shelf updates onShelf state`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = false)

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterAdd = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnAddBookClick(testBook))
        }

        // Then
        assertTrue("Should update onShelf to true", stateAfterAdd?.onShelf == true)
        stateHelper.cleanup()
    }

    @Test
    fun `remove book from shelf updates onShelf state`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = true)

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterRemove = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnRemoveBookClick(testBook))
        }

        // Then
        assertFalse("Should update onShelf to false", stateAfterRemove?.onShelf == true)
        stateHelper.cleanup()
    }

    @Test
    fun `toggle purchase updates book purchased status`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withPurchased(false).build()
        val purchasedBook = testBook.copy(purchased = true)
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = true)
        mockToggleBookPurchase.bookToReturn = purchasedBook

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnPurchaseClick)
        }

        // Then
        assertTrue("Should update book purchased status", stateAfterToggle?.book?.purchased == true)
        stateHelper.cleanup()
    }

    // NOTE: Success case tests for personal metadata removed because they require
    // reactive database flow which is better tested at integration level.
    // These would need complex mocks to simulate database triggering reactive updates.

    @Test
    fun `update reading status handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = true)
        mockUpdateBookMetadata.shouldSucceed = false

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterUpdate = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnReadingStatusChange(ReadingStatus.READ))
        }

        // Then - Error case: state should not change since save failed
        assertEquals("Should keep original reading status", ReadingStatus.WANT_TO_READ, stateAfterUpdate?.book?.readingStatus)
        assertTrue("Should set error message", stateAfterUpdate?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterUpdate?.errorMessage?.contains("Failed to update reading status") == true)
        stateHelper.cleanup()
    }

    @Test
    fun `update personal rating handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withPersonalRating(0f).build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = true)
        mockUpdateBookMetadata.shouldSucceed = false

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterUpdate = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnPersonalRatingChange(3.5f))
        }

        // Then - Error case: state should not change since save failed
        assertEquals("Should keep original rating as 0f", 0f, stateAfterUpdate?.book?.personalRating)
        assertTrue("Should set error message", stateAfterUpdate?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterUpdate?.errorMessage?.contains("Failed to update personal rating") == true)
        stateHelper.cleanup()
    }

    @Test
    fun `add book to shelf handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = false)
        mockAddBookToShelf.shouldSucceed = false

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterAdd = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnAddBookClick(testBook))
        }

        // Then
        assertFalse("Should keep onShelf as false", stateAfterAdd?.onShelf == true)
        assertTrue("Should set error message", stateAfterAdd?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterAdd?.errorMessage?.contains("Failed to add book to shelf") == true)
        stateHelper.cleanup()
    }

    @Test
    fun `remove book from shelf when onShelf handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = true)
        mockRemoveBookFromShelf.shouldSucceed = false

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterRemove = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnAddBookClick(testBook))
        }

        // Then
        assertTrue("Should keep onShelf as true", stateAfterRemove?.onShelf == true)
        assertTrue("Should set error message", stateAfterRemove?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterRemove?.errorMessage?.contains("Failed to remove book from shelf") == true)
        stateHelper.cleanup()
    }

    @Test
    fun `toggle purchase handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withPurchased(false).build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = true)
        mockToggleBookPurchase.bookToReturn = null // Triggers error

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterToggle = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnPurchaseClick)
        }

        // Then
        assertFalse("Should keep book purchased status as false", stateAfterToggle?.book?.purchased == true)
        assertTrue("Should set error message", stateAfterToggle?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterToggle?.errorMessage?.contains("Failed to toggle book purchase") == true)
        stateHelper.cleanup()
    }

    @Test
    fun `remove book click handles error correctly`() = runTest(testDispatcher) {
        // Given
        val testBook = TestBookBuilder().withId("book-1").withTitle("Test Book").build()
        mockGetBookDetails.bookDetailsToReturn = BookDetailsWithShelfStatus(testBook, isOnShelf = true)
        mockRemoveBookFromShelf.shouldSucceed = false

        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Wait for initial load
        stateHelper.awaitState()

        // When
        val stateAfterRemove = stateHelper.executeAndGetState {
            viewModel.onAction(BookDetailAction.OnRemoveBookClick(testBook))
        }

        // Then
        assertTrue("Should keep onShelf as true", stateAfterRemove?.onShelf == true)
        assertTrue("Should set error message", stateAfterRemove?.errorMessage != null)
        assertTrue("Should contain operation context",
            stateAfterRemove?.errorMessage?.contains("Failed to remove book from shelf") == true)
        stateHelper.cleanup()
    }

    // Simplified inline mock implementations for UI testing
    private class SimpleGetBookDetailsUseCase : GetBookDetailsUseCase {
        var bookDetailsToReturn: BookDetailsWithShelfStatus = BookDetailsWithShelfStatus(null, false)

        override suspend fun execute(bookId: String, shelfId: String): Flow<BookDetailsWithShelfStatus> =
            flowOf(bookDetailsToReturn)

        override suspend fun loadBookDescription(bookId: String): Result<Unit, DataError.Local> =
            Result.Success(Unit)

        fun reset() {
            bookDetailsToReturn = BookDetailsWithShelfStatus(null, false)
        }
    }

    private class SimpleAddBookToShelfUseCase : AddBookToShelfUseCase {
        var shouldSucceed = true

        override suspend fun execute(book: Book, shelfId: String): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    private class SimpleRemoveBookFromShelfUseCase : RemoveBookFromShelfUseCase {
        var shouldSucceed = true

        override suspend fun execute(bookId: String, shelfId: String): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    private class SimpleUpsertBookUseCase : UpsertBookUseCase {
        var shouldSucceed = true

        override suspend fun execute(book: Book): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    private class SimpleToggleBookPurchaseUseCase : ToggleBookPurchaseUseCase {
        var bookToReturn: Book? = null

        override suspend fun execute(book: Book, purchased: Boolean): Result<Book, DataError.Local> =
            bookToReturn?.let { Result.Success(it) } ?: Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            bookToReturn = null
        }
    }

    private class SimpleUpdateBookMetadataUseCase : UpdateBookMetadataUseCase {
        var shouldSucceed = true

        override suspend fun execute(
            bookId: String,
            readingStatus: ReadingStatus?,
            personalRating: Float?,
            personalNotes: String?,
            purchaseDate: Long?
        ): Result<Unit, DataError> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }

    // Book Club Use Case mocks
    private class SimpleAuthService : AuthService {
        override suspend fun signIn(): Result<UserData, DataError.Local> =
            Result.Success(UserData("test-user", "Test User", null))
        override suspend fun signOut(): Result<Unit, DataError.Local> = Result.Success(Unit)
        override fun getSignedInUser(): UserData? = UserData("test-user", "Test User", null)
    }

    private class SimpleCreateBookClubUseCase : CreateBookClubUseCase {
        override suspend fun execute(shelfId: String): Result<String, DataError.Sync> =
            Result.Success("ABC12345")
    }

    private class SimpleGenerateInviteLinkUseCase : GenerateInviteLinkUseCase {
        override fun execute(clubCode: String, clubName: String?): String =
            "https://mybookshelf.app/join/$clubCode"
    }

    private class SimpleParseClubCodeUseCase : ParseClubCodeUseCase {
        override fun invoke(input: String): Result<String, DataError.Validation> =
            Result.Success("TESTCODE")
    }

    private class SimpleGetBookClubPreviewUseCase : GetBookClubPreviewUseCase {
        override suspend fun invoke(code: String): Result<BookClub?, DataError.Sync> =
            Result.Success(null)
    }

    private class SimpleJoinBookClubUseCase : JoinBookClubUseCase {
        override suspend fun invoke(code: String): Result<JoinResult, DataError.Sync> =
            Result.Success(JoinResult.Success("shelf-id", "Test Shelf"))
    }

    private class SimpleRestoreBookClubMembershipsUseCase : RestoreBookClubMembershipsUseCase {
        override suspend fun invoke(): Result<RestoreResult, DataError.Sync> =
            Result.Success(RestoreResult(0, 0))
    }

    private class SimpleSyncBookClubUseCase : SyncBookClubUseCase {
        override suspend fun execute(clubCode: String, localShelfId: String): Result<SyncResult, DataError.Sync> =
            Result.Success(SyncResult(0, 0))
    }

    private class SimpleLeaveBookClubUseCase : LeaveBookClubUseCase {
        override suspend fun invoke(shelfId: String): Result<Unit, DataError.Sync> =
            Result.Success(Unit)
    }

    private class SimpleValidateBookClubMembershipsUseCase : ValidateBookClubMembershipsUseCase {
        override suspend fun invoke(): Result<List<String>, DataError.Sync> =
            Result.Success(emptyList())
    }

    private class SimpleGetBookClubReviewsUseCase : GetBookClubReviewsUseCase {
        override suspend fun invoke(clubCode: String, bookId: String): Result<List<BookClubReview>, DataError.Sync> =
            Result.Success(emptyList())
    }

    private class SimpleUpsertBookClubReviewUseCase : UpsertBookClubReviewUseCase {
        override suspend fun invoke(
            clubCode: String,
            bookId: String,
            rating: Float,
            reviewText: String
        ): Result<Unit, DataError.Sync> = Result.Success(Unit)
    }

    private class SimpleDeleteBookClubReviewUseCase : DeleteBookClubReviewUseCase {
        override suspend fun invoke(clubCode: String, bookId: String): Result<Unit, DataError.Sync> =
            Result.Success(Unit)
    }

    // Comment Use Case mocks
    private class SimpleGetBookClubCommentsUseCase : GetBookClubCommentsUseCase {
        override suspend fun invoke(clubCode: String, bookId: String): Result<List<BookClubComment>, DataError.Sync> =
            Result.Success(emptyList())
    }

    private class SimpleAddBookClubCommentUseCase : AddBookClubCommentUseCase {
        override suspend fun invoke(clubCode: String, bookId: String, text: String): Result<String, DataError.Sync> =
            Result.Success("comment-id")
    }

    private class SimpleEditBookClubCommentUseCase : EditBookClubCommentUseCase {
        override suspend fun invoke(clubCode: String, bookId: String, commentId: String, newText: String): Result<Unit, DataError.Sync> =
            Result.Success(Unit)
    }

    private class SimpleDeleteBookClubCommentUseCase : DeleteBookClubCommentUseCase {
        override suspend fun invoke(clubCode: String, bookId: String, commentId: String): Result<Unit, DataError.Sync> =
            Result.Success(Unit)
    }
}
