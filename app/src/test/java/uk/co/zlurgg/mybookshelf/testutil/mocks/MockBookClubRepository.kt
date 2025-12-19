package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
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
    var localShelfForClubAfterJoin: Bookshelf? = null  // Used to simulate shelf creation after join
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

    override suspend fun syncBookToClub(code: String, book: Book): Result<Unit, DataError.Sync> {
        syncBookToClubCalled = true
        lastSyncBookCode = code
        lastSyncBook = book
        return syncBookToClubResult
    }

    override suspend fun removeBookFromClub(code: String, bookId: String): Result<Unit, DataError.Sync> {
        removeBookFromClubCalled = true
        lastRemoveBookCode = code
        lastRemoveBookId = bookId
        return removeBookFromClubResult
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
    }

    fun configureBookClub(bookClub: BookClub) {
        getBookClubResult = Result.Success(bookClub)
    }

    fun configureBookClubNotFound() {
        getBookClubResult = Result.Success(null)
    }

    fun configureAlreadyMember(localShelfId: String) {
        isMemberResult = Result.Success(true)
        localShelfForClub = Bookshelf(
            id = localShelfId,
            name = "Test Club (Book Club)",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            isBookClub = true,
            clubCode = "TEST1234"
        )
    }

    fun configureNotMember() {
        isMemberResult = Result.Success(false)
        localShelfForClub = null
    }

    fun configureJoinSuccess(localShelfId: String, shelfName: String) {
        joinBookClubResult = Result.Success(localShelfId)
        // Set localShelfForClubAfterJoin so getLocalShelfForClub returns the shelf after join
        localShelfForClubAfterJoin = Bookshelf(
            id = localShelfId,
            name = shelfName,
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            isBookClub = true,
            clubCode = "TEST1234"
        )
    }

    fun configureJoinError(error: DataError.Sync) {
        joinBookClubResult = Result.Error(error)
    }
}
