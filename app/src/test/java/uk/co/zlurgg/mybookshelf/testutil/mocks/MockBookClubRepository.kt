package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Reusable mock BookClubRepository for testing.
 * Provides configurable behavior for testing different book club scenarios.
 */
class MockBookClubRepository : BookClubRepository {
    // Configuration properties
    var createBookClubResult: Result<String, DataError.Sync> = Result.Success("TEST1234")
    var getBookClubResult: Result<BookClub?, DataError.Sync> = Result.Success(null)
    var deleteBookClubResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var isMemberResult: Result<Boolean, DataError.Sync> = Result.Success(false)
    var joinBookClubResult: Result<String, DataError.Sync> = Result.Success("local-shelf-id")
    var getClubBooksResult: Result<List<Book>, DataError.Sync> = Result.Success(emptyList())
    var localShelfForClub: Bookshelf? = null
    var localShelfForClubAfterJoin: Bookshelf? = null // Used to simulate shelf creation after join
    var myBookClubs: List<BookClubMembership> = emptyList()
    var getRemoteClubMembershipsResult: Result<List<String>, DataError.Sync> = Result.Success(emptyList())
    var restoreClubMembershipResult: Result<String, DataError.Sync> = Result.Success("restored-shelf-id")

    // Tracking properties
    var createBookClubCalled = false
    var getBookClubCalled = false
    var deleteBookClubCalled = false
    var isMemberOfClubCalled = false
    var joinBookClubCalled = false
    var getClubBooksCalled = false
    var getLocalShelfForClubCalled = false

    var lastCreateShelfId: String? = null
    var lastGetBookClubCode: String? = null
    var lastDeleteCode: String? = null
    var lastIsMemberCode: String? = null
    var lastJoinCode: String? = null
    var lastGetBooksCode: String? = null
    var lastGetLocalShelfCode: String? = null
    var getRemoteClubMembershipsCalled = false
    var restoreClubMembershipCalled = false
    var lastGetRemoteMembershipsUserId: String? = null
    var lastRestoreClubCode: String? = null

    override suspend fun createBookClub(shelfId: String): Result<String, DataError.Sync> {
        createBookClubCalled = true
        lastCreateShelfId = shelfId
        return createBookClubResult
    }

    override suspend fun getBookClub(code: String): Result<BookClub?, DataError.Sync> {
        getBookClubCalled = true
        lastGetBookClubCode = code
        return getBookClubResult
    }

    override suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync> {
        deleteBookClubCalled = true
        lastDeleteCode = code
        return deleteBookClubResult
    }

    override fun observeMyBookClubs(): Flow<List<BookClubMembership>> {
        return flowOf(myBookClubs)
    }

    override suspend fun getLocalShelfForClub(code: String): Bookshelf? {
        getLocalShelfForClubCalled = true
        lastGetLocalShelfCode = code
        // After join has been called, return the post-join shelf if configured
        return if (joinBookClubCalled && localShelfForClubAfterJoin != null) {
            localShelfForClubAfterJoin
        } else {
            localShelfForClub
        }
    }

    override suspend fun isMemberOfClub(code: String): Result<Boolean, DataError.Sync> {
        isMemberOfClubCalled = true
        lastIsMemberCode = code
        return isMemberResult
    }

    override suspend fun joinBookClub(code: String): Result<String, DataError.Sync> {
        joinBookClubCalled = true
        lastJoinCode = code
        return joinBookClubResult
    }

    override suspend fun getRemoteClubMemberships(userId: String): Result<List<String>, DataError.Sync> {
        getRemoteClubMembershipsCalled = true
        lastGetRemoteMembershipsUserId = userId
        return getRemoteClubMembershipsResult
    }

    override suspend fun restoreClubMembership(code: String): Result<String, DataError.Sync> {
        restoreClubMembershipCalled = true
        lastRestoreClubCode = code
        return restoreClubMembershipResult
    }

    override suspend fun getClubBooks(code: String): Result<List<Book>, DataError.Sync> {
        getClubBooksCalled = true
        lastGetBooksCode = code
        return getClubBooksResult
    }

    // Book sync methods (new)
    var syncBookToClubResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var removeBookFromClubResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var syncBookToClubCalled = false
    var removeBookFromClubCalled = false
    var lastSyncBookCode: String? = null
    var lastSyncBook: Book? = null
    var lastRemoveBookCode: String? = null
    var lastRemoveBookId: String? = null

    override suspend fun syncBookToClub(
        code: String,
        book: Book,
    ): Result<Unit, DataError.Sync> {
        syncBookToClubCalled = true
        lastSyncBookCode = code
        lastSyncBook = book
        return syncBookToClubResult
    }

    override suspend fun removeBookFromClub(
        code: String,
        bookId: String,
    ): Result<Unit, DataError.Sync> {
        removeBookFromClubCalled = true
        lastRemoveBookCode = code
        lastRemoveBookId = bookId
        return removeBookFromClubResult
    }

    // Sync and rename methods
    var syncBooksFromClubResult: Result<SyncResult, DataError.Sync> = Result.Success(SyncResult(0, 0))
    var renameBookClubResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var syncBooksFromClubCalled = false
    var renameBookClubCalled = false
    var lastSyncFromClubCode: String? = null
    var lastSyncFromClubShelfId: String? = null
    var lastRenameClubCode: String? = null
    var lastRenameNewName: String? = null

    override suspend fun syncBooksFromClub(
        code: String,
        localShelfId: String,
    ): Result<SyncResult, DataError.Sync> {
        syncBooksFromClubCalled = true
        lastSyncFromClubCode = code
        lastSyncFromClubShelfId = localShelfId
        return syncBooksFromClubResult
    }

    override suspend fun renameBookClub(
        code: String,
        newName: String,
    ): Result<Unit, DataError.Sync> {
        renameBookClubCalled = true
        lastRenameClubCode = code
        lastRenameNewName = newName
        return renameBookClubResult
    }

    // Leave book club
    var leaveBookClubResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var leaveBookClubCalled = false
    var lastLeaveCode: String? = null

    override suspend fun leaveBookClub(code: String): Result<Unit, DataError.Sync> {
        leaveBookClubCalled = true
        lastLeaveCode = code
        return leaveBookClubResult
    }

    // Convert club to personal shelf
    var convertClubToPersonalShelfResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var convertClubToPersonalShelfCalled = false
    var lastConvertCode: String? = null

    override suspend fun convertClubToPersonalShelf(code: String): Result<Unit, DataError.Sync> {
        convertClubToPersonalShelfCalled = true
        lastConvertCode = code
        return convertClubToPersonalShelfResult
    }

    // Update club style
    var updateClubStyleResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var updateClubStyleCalled = false
    var lastUpdateStyleCode: String? = null
    var lastUpdateStyleValue: String? = null

    override suspend fun updateClubStyle(
        code: String,
        style: String,
    ): Result<Unit, DataError.Sync> {
        updateClubStyleCalled = true
        lastUpdateStyleCode = code
        lastUpdateStyleValue = style
        return updateClubStyleResult
    }

    // Reviews
    var getBookReviewsResult: Result<List<BookClubReview>, DataError.Sync> = Result.Success(emptyList())
    var upsertBookReviewResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var deleteBookReviewResult: Result<Unit, DataError.Sync> = Result.Success(Unit)

    override suspend fun getBookReviews(
        code: String,
        bookId: String,
    ): Result<List<BookClubReview>, DataError.Sync> {
        return getBookReviewsResult
    }

    override suspend fun upsertBookReview(
        code: String,
        bookId: String,
        rating: Float,
        reviewText: String,
    ): Result<Unit, DataError.Sync> {
        return upsertBookReviewResult
    }

    override suspend fun deleteBookReview(
        code: String,
        bookId: String,
    ): Result<Unit, DataError.Sync> {
        return deleteBookReviewResult
    }

    // Comments
    var getBookCommentsResult: Result<List<BookClubComment>, DataError.Sync> = Result.Success(emptyList())
    var addBookCommentResult: Result<String, DataError.Sync> = Result.Success("comment-id")
    var editBookCommentResult: Result<Unit, DataError.Sync> = Result.Success(Unit)
    var deleteBookCommentResult: Result<Unit, DataError.Sync> = Result.Success(Unit)

    var getBookCommentsCalled = false
    var addBookCommentCalled = false
    var editBookCommentCalled = false
    var deleteBookCommentCalled = false

    var lastGetCommentsCode: String? = null
    var lastGetCommentsBookId: String? = null
    var lastAddCommentCode: String? = null
    var lastAddCommentBookId: String? = null
    var lastAddCommentText: String? = null
    var lastEditCommentCode: String? = null
    var lastEditCommentBookId: String? = null
    var lastEditCommentId: String? = null
    var lastEditCommentText: String? = null
    var lastDeleteCommentCode: String? = null
    var lastDeleteCommentBookId: String? = null
    var lastDeleteCommentId: String? = null

    override suspend fun getBookComments(
        code: String,
        bookId: String,
    ): Result<List<BookClubComment>, DataError.Sync> {
        getBookCommentsCalled = true
        lastGetCommentsCode = code
        lastGetCommentsBookId = bookId
        return getBookCommentsResult
    }

    override suspend fun addBookComment(
        code: String,
        bookId: String,
        text: String,
    ): Result<String, DataError.Sync> {
        addBookCommentCalled = true
        lastAddCommentCode = code
        lastAddCommentBookId = bookId
        lastAddCommentText = text
        return addBookCommentResult
    }

    override suspend fun editBookComment(
        code: String,
        bookId: String,
        commentId: String,
        newText: String,
    ): Result<Unit, DataError.Sync> {
        editBookCommentCalled = true
        lastEditCommentCode = code
        lastEditCommentBookId = bookId
        lastEditCommentId = commentId
        lastEditCommentText = newText
        return editBookCommentResult
    }

    override suspend fun deleteBookComment(
        code: String,
        bookId: String,
        commentId: String,
    ): Result<Unit, DataError.Sync> {
        deleteBookCommentCalled = true
        lastDeleteCommentCode = code
        lastDeleteCommentBookId = bookId
        lastDeleteCommentId = commentId
        return deleteBookCommentResult
    }

    // Helper methods for test setup
    fun reset() {
        createBookClubResult = Result.Success("TEST1234")
        getBookClubResult = Result.Success(null)
        deleteBookClubResult = Result.Success(Unit)
        isMemberResult = Result.Success(false)
        joinBookClubResult = Result.Success("local-shelf-id")
        getClubBooksResult = Result.Success(emptyList())
        localShelfForClub = null
        localShelfForClubAfterJoin = null
        myBookClubs = emptyList()
        getRemoteClubMembershipsResult = Result.Success(emptyList())
        restoreClubMembershipResult = Result.Success("restored-shelf-id")
        syncBookToClubResult = Result.Success(Unit)
        removeBookFromClubResult = Result.Success(Unit)
        syncBooksFromClubResult = Result.Success(SyncResult(0, 0))
        renameBookClubResult = Result.Success(Unit)
        leaveBookClubResult = Result.Success(Unit)
        convertClubToPersonalShelfResult = Result.Success(Unit)
        updateClubStyleResult = Result.Success(Unit)

        createBookClubCalled = false
        getBookClubCalled = false
        deleteBookClubCalled = false
        isMemberOfClubCalled = false
        joinBookClubCalled = false
        getClubBooksCalled = false
        getLocalShelfForClubCalled = false
        getRemoteClubMembershipsCalled = false
        restoreClubMembershipCalled = false
        syncBookToClubCalled = false
        removeBookFromClubCalled = false
        syncBooksFromClubCalled = false
        renameBookClubCalled = false
        leaveBookClubCalled = false
        convertClubToPersonalShelfCalled = false
        updateClubStyleCalled = false
        getBookCommentsCalled = false
        addBookCommentCalled = false
        editBookCommentCalled = false
        deleteBookCommentCalled = false

        lastCreateShelfId = null
        lastGetBookClubCode = null
        lastDeleteCode = null
        lastIsMemberCode = null
        lastJoinCode = null
        lastGetBooksCode = null
        lastGetLocalShelfCode = null
        lastGetRemoteMembershipsUserId = null
        lastRestoreClubCode = null
        lastSyncBookCode = null
        lastSyncBook = null
        lastRemoveBookCode = null
        lastRemoveBookId = null
        lastSyncFromClubCode = null
        lastSyncFromClubShelfId = null
        lastRenameClubCode = null
        lastRenameNewName = null
        lastLeaveCode = null
        lastConvertCode = null
        lastUpdateStyleCode = null
        lastUpdateStyleValue = null
        lastGetCommentsCode = null
        lastGetCommentsBookId = null
        lastAddCommentCode = null
        lastAddCommentBookId = null
        lastAddCommentText = null
        lastEditCommentCode = null
        lastEditCommentBookId = null
        lastEditCommentId = null
        lastEditCommentText = null
        lastDeleteCommentCode = null
        lastDeleteCommentBookId = null
        lastDeleteCommentId = null

        getBookCommentsResult = Result.Success(emptyList())
        addBookCommentResult = Result.Success("comment-id")
        editBookCommentResult = Result.Success(Unit)
        deleteBookCommentResult = Result.Success(Unit)
    }

    fun configureBookClub(bookClub: BookClub) {
        getBookClubResult = Result.Success(bookClub)
    }

    fun configureBookClubNotFound() {
        getBookClubResult = Result.Success(null)
    }

    fun configureAlreadyMember(localShelfId: String) {
        isMemberResult = Result.Success(true)
        localShelfForClub =
            Bookshelf(
                id = localShelfId,
                name = "Test Club (Book Club)",
                books = emptyList(),
                shelfStyle = ShelfStyle.DarkWood,
                isBookClub = true,
                clubCode = "TEST1234",
            )
    }

    fun configureNotMember() {
        isMemberResult = Result.Success(false)
        localShelfForClub = null
    }

    fun configureJoinSuccess(
        localShelfId: String,
        shelfName: String,
    ) {
        joinBookClubResult = Result.Success(localShelfId)
        // Set localShelfForClubAfterJoin so getLocalShelfForClub returns the shelf after join
        localShelfForClubAfterJoin =
            Bookshelf(
                id = localShelfId,
                name = shelfName,
                books = emptyList(),
                shelfStyle = ShelfStyle.DarkWood,
                isBookClub = true,
                clubCode = "TEST1234",
            )
    }

    fun configureJoinError(error: DataError.Sync) {
        joinBookClubResult = Result.Error(error)
    }
}
