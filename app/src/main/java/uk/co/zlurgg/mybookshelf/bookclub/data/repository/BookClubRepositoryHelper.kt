package uk.co.zlurgg.mybookshelf.bookclub.data.repository

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.bookclub.data.mappers.toBookClub
import uk.co.zlurgg.mybookshelf.bookclub.data.mappers.toBookClubBookDto
import uk.co.zlurgg.mybookshelf.bookclub.data.mappers.toBookDomain
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookClubDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.bookclub.data.remote.BookClubRemoteDataSource

/**
 * Shared operations used by multiple BookClub repository implementations.
 * Handles cross-boundary concerns like book transfers and club metadata lookups.
 */
internal class BookClubRepositoryHelper(
    private val bookClubDao: BookClubDao,
    private val bookshelfDao: BookshelfDao,
    private val remoteDataSource: BookClubRemoteDataSource,
    private val authService: AuthService,
    private val timeProvider: TimeProvider,
) {

    suspend fun fetchBookClubMetadata(code: String): Result<BookClub?, DataError.Sync> {
        Timber.tag(TAG).d("Fetching book club metadata: %s", code)

        val metadataResult = remoteDataSource.getBookClubMetadata(code)
        return when (metadataResult) {
            is Result.Success -> {
                val metadata = metadataResult.data
                if (metadata != null) {
                    Result.Success(metadata.toBookClub())
                } else {
                    Result.Success(null)
                }
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get book club: %s", metadataResult.error)
                Result.Error(metadataResult.error)
            }
        }
    }

    suspend fun cleanupLocalClubData(code: String, localShelfId: String?, userId: String) {
        bookClubDao.deleteMembership(code)

        if (localShelfId != null) {
            bookshelfDao.deleteAllCrossRefsForShelf(localShelfId)
            bookshelfDao.deleteShelf(localShelfId)
            Timber.tag(TAG).d("Hard deleted local shelf and cross-refs: %s", localShelfId)
        }

        val removeResult = remoteDataSource.removeClubMembership(userId, code)
        if (removeResult is Result.Error) {
            Timber.tag(TAG).w("Failed to remove from user preferences: %s", removeResult.error)
        }
    }

    suspend fun uploadShelfBooksToClub(
        shelfId: String,
        clubCode: String,
        userId: String,
        userName: String,
    ): Result<Int, DataError.Sync> {
        val crossRefs = bookClubDao.getBookIdsForShelf(shelfId)

        var uploadedCount = 0
        for (bookId in crossRefs) {
            val bookEntity = bookshelfDao.getBookById(bookId)
            if (bookEntity != null) {
                val book = bookEntity.toBook()
                val bookDto = book.toBookClubBookDto(userId, userName)

                val uploadResult = remoteDataSource.addBookToClub(clubCode, bookDto)
                if (uploadResult is Result.Error) {
                    Timber.tag(TAG).w("Failed to upload book %s to club: %s", bookId, uploadResult.error)
                } else {
                    uploadedCount++
                }
            }
        }

        Timber.tag(TAG).d("Uploaded %d/%d books to club", uploadedCount, crossRefs.size)
        return Result.Success(uploadedCount)
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun downloadClubBooksToShelf(
        clubCode: String,
        shelfId: String,
    ): Result<Int, DataError.Sync> {
        val booksResult = remoteDataSource.getClubBooks(clubCode)

        if (booksResult is Result.Error) {
            return Result.Error(booksResult.error)
        }

        val clubBooks = (booksResult as Result.Success).data
        Timber.tag(TAG).d("Downloaded %d books from club %s", clubBooks.size, clubCode)
        var addedCount = 0

        for (bookDto in clubBooks) {
            try {
                val book = bookDto.toBookDomain()
                val bookEntity = book.toBookEntity()

                bookshelfDao.upsert(bookEntity)

                val crossRef = BookshelfBookCrossRef(
                    shelfId = shelfId,
                    bookId = book.id,
                    addedAt = timeProvider.currentTimeMillis()
                )
                bookshelfDao.upsertCrossRef(crossRef)
                addedCount++
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to add book %s to local shelf", bookDto.id)
            }
        }

        Timber.tag(TAG).d("Downloaded %d/%d books to local shelf", addedCount, clubBooks.size)
        return Result.Success(addedCount)
    }

    fun generateUniqueShelfName(clubName: String): String {
        return clubName
    }

    suspend fun updateClubBookCount(code: String) {
        val booksResult = remoteDataSource.getClubBooks(code)
        if (booksResult is Result.Success) {
            val bookCount = booksResult.data.size
            val metadataResult = remoteDataSource.getBookClubMetadata(code)
            if (metadataResult is Result.Success && metadataResult.data != null) {
                val memberCount = metadataResult.data.memberCount
                remoteDataSource.updateBookClubCounts(code, bookCount, memberCount)
            }
        }
    }

    suspend fun convertToPersonalShelf(code: String): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Converting club %s to personal shelf", code)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot convert: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        val membership = bookClubDao.getMembershipByClubCode(code)
        if (membership == null) {
            Timber.tag(TAG).w("No local membership found for club: %s", code)
            return Result.Success(Unit)
        }

        val localShelfId = membership.localShelfId

        val shelfEntity = bookshelfDao.getShelfById(localShelfId)
        if (shelfEntity != null) {
            val convertedShelf = shelfEntity.copy(
                isBookClub = false,
                clubCode = null,
                clubCreatorId = null
            )
            bookshelfDao.upsertShelf(convertedShelf)
            Timber.tag(TAG).d("Converted shelf '%s' to personal shelf", shelfEntity.name)
        }

        bookClubDao.deleteMembership(code)

        val removeResult = remoteDataSource.removeClubMembership(user.userId, code)
        if (removeResult is Result.Error) {
            Timber.tag(TAG).w("Failed to remove from user preferences: %s", removeResult.error)
        }

        Timber.tag(TAG).d("Successfully converted club %s to personal shelf", code)
        return Result.Success(Unit)
    }

    companion object {
        private const val TAG = "BookClubRepoHelper"
    }
}
