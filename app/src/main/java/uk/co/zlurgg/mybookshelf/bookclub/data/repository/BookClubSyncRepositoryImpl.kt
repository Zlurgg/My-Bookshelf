package uk.co.zlurgg.mybookshelf.bookclub.data.repository

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.bookclub.data.mappers.toBookClubBookDto
import uk.co.zlurgg.mybookshelf.bookclub.data.mappers.toBookDomain
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubSyncRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookClubDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.bookclub.data.remote.BookClubRemoteDataSource

internal class BookClubSyncRepositoryImpl(
    private val bookClubDao: BookClubDao,
    private val bookshelfDao: BookshelfDao,
    private val remoteDataSource: BookClubRemoteDataSource,
    private val authService: AuthService,
    private val timeProvider: TimeProvider,
    private val helper: BookClubRepositoryHelper,
) : BookClubSyncRepository {

    override suspend fun getClubBooks(code: String): Result<List<Book>, DataError.Sync> {
        Timber.tag(TAG).d("Getting books for club: %s", code)

        val booksResult = remoteDataSource.getClubBooks(code)
        return when (booksResult) {
            is Result.Success -> {
                val books = booksResult.data.map { it.toBookDomain() }
                Timber.tag(TAG).d("Retrieved %d books from club", books.size)
                Result.Success(books)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get club books: %s", booksResult.error)
                Result.Error(booksResult.error)
            }
        }
    }

    override suspend fun syncBookToClub(code: String, book: Book): Result<Unit, DataError.Sync> {
        val user = authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        val userId = user.userId
        val userName = user.username ?: "Unknown"

        Timber.tag(TAG).d("Syncing book %s to club %s", book.id, code)

        val bookDto = book.toBookClubBookDto(userId, userName)
        val uploadResult = remoteDataSource.addBookToClub(code, bookDto)

        return when (uploadResult) {
            is Result.Success -> {
                Timber.tag(TAG).d("Book %s synced to club %s", book.id, code)
                helper.updateClubBookCount(code)
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to sync book %s to club: %s", book.id, uploadResult.error)
                Result.Error(uploadResult.error)
            }
        }
    }

    override suspend fun removeBookFromClub(code: String, bookId: String): Result<Unit, DataError.Sync> {
        authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        Timber.tag(TAG).d("Removing book %s from club %s", bookId, code)

        return when (val removeResult = remoteDataSource.removeBookFromClub(code, bookId)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Book %s removed from club %s", bookId, code)
                helper.updateClubBookCount(code)
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to remove book %s from club: %s", bookId, removeResult.error)
                Result.Error(removeResult.error)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun syncBooksFromClub(
        code: String,
        localShelfId: String,
    ): Result<SyncResult, DataError.Sync> {
        Timber.tag(TAG).d("Syncing books from club %s to shelf %s", code, localShelfId)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot sync: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        // Check if club still exists
        val metadataResult = remoteDataSource.getBookClubMetadata(code)
        if (metadataResult is Result.Error) {
            Timber.tag(TAG).e("Failed to get club metadata: %s", metadataResult.error)
            return Result.Error(metadataResult.error)
        }
        val clubMetadata = (metadataResult as Result.Success).data

        // If club was deleted, convert to personal shelf (preserve books)
        if (clubMetadata == null) {
            Timber.tag(TAG).d("Club %s was deleted, converting to personal shelf", code)
            helper.convertToPersonalShelf(code)
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        // Get remote books from Firestore
        val remoteBooksResult = remoteDataSource.getClubBooks(code)
        if (remoteBooksResult is Result.Error) {
            Timber.tag(TAG).e("Failed to get remote books: %s", remoteBooksResult.error)
            return Result.Error(remoteBooksResult.error)
        }
        val remoteBooks = (remoteBooksResult as Result.Success).data
        val remoteBookIds = remoteBooks.map { it.id }.toSet()

        // Get local book IDs for this shelf
        val localBookIds = bookClubDao.getBookIdsForShelf(localShelfId).toSet()

        Timber.tag(TAG).d("Remote books: %d, Local books: %d", remoteBookIds.size, localBookIds.size)

        var booksAdded = 0
        var booksRemoved = 0

        // Add books that are in remote but not local
        val booksToAdd = remoteBookIds - localBookIds
        for (bookDto in remoteBooks.filter { it.id in booksToAdd }) {
            try {
                val book = bookDto.toBookDomain()
                val bookEntity = book.toBookEntity()
                bookshelfDao.upsert(bookEntity)

                val crossRef = BookshelfBookCrossRef(
                    shelfId = localShelfId,
                    bookId = book.id,
                    addedAt = timeProvider.currentTimeMillis()
                )
                bookshelfDao.upsertCrossRef(crossRef)
                booksAdded++
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to add book %s", bookDto.id)
            }
        }

        // Remove books that are local but not in remote
        val booksToRemove = localBookIds - remoteBookIds
        for (bookId in booksToRemove) {
            try {
                bookshelfDao.deleteCrossRef(localShelfId, bookId)
                booksRemoved++
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to remove book %s", bookId)
            }
        }

        // Sync club name/style from Firestore to local shelf
        val localShelf = bookshelfDao.getShelfById(localShelfId)
        if (localShelf != null && localShelf.name != clubMetadata.name) {
            bookshelfDao.upsertShelf(localShelf.copy(name = clubMetadata.name))
            Timber.tag(TAG).d("Updated local shelf name to: %s", clubMetadata.name)
        }

        Timber.tag(TAG).d("Sync complete: added %d, removed %d books", booksAdded, booksRemoved)
        return Result.Success(SyncResult(booksAdded, booksRemoved))
    }

    companion object {
        private const val TAG = "BookClubSyncRepo"
    }
}
