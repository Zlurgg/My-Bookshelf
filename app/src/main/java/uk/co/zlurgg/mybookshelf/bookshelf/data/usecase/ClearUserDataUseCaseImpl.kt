package uk.co.zlurgg.mybookshelf.bookshelf.data.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ClearUserDataUseCase
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookClubDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation that clears all local data for a user during sign-out.
 * This prevents data leakage when switching between accounts.
 *
 * Note: This implementation is in the data layer because it directly
 * accesses the DAO for database operations. The interface remains in
 * the domain layer to maintain Clean Architecture principles.
 */
class ClearUserDataUseCaseImpl(
    private val bookshelfDao: BookshelfDao,
    private val bookClubDao: BookClubDao,
) : ClearUserDataUseCase {
    companion object {
        private const val TAG = "ClearUserData"
    }

    override suspend fun execute(userId: String): Result<Int, DataError.Local> {
        Timber.tag(TAG).d("=== CLEARING USER DATA ===")
        Timber.tag(TAG).d("User ID: %s", userId)

        return try {
            // Count items before deletion for logging
            val shelves = bookshelfDao.getShelvesByOwner(userId)
            val books = bookshelfDao.getBooksByOwner(userId)
            val totalItems = shelves.size + books.size

            Timber.tag(TAG).d("Found %d shelves and %d books to delete", shelves.size, books.size)

            // Delete in correct order to respect foreign key constraints:
            // 1. Cross-refs first (references both shelves and books)
            // 2. Books second
            // 3. Shelves last
            bookshelfDao.deleteAllCrossRefsForOwner(userId)
            Timber.tag(TAG).d("Deleted cross-refs for user")

            bookshelfDao.deleteAllBooksForOwner(userId)
            Timber.tag(TAG).d("Deleted books for user")

            bookshelfDao.deleteAllShelvesForOwner(userId)
            Timber.tag(TAG).d("Deleted shelves for user")

            // Also clear book club memberships (they reference deleted shelves)
            bookClubDao.deleteAllMemberships()
            Timber.tag(TAG).d("Deleted book club memberships")

            Timber.tag(TAG).d("=== USER DATA CLEARED: %d items ===", totalItems)
            Result.Success(totalItems)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear user data")
            val error = ErrorMapper.mapExceptionToDataError(e)
            Result.Error(if (error is DataError.Local) error else DataError.Local.DATABASE_ERROR)
        }
    }
}
