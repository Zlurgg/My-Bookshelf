package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toBookClubBookDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toDomain
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookClubCodeGenerator
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookClubDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMemberDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMetadataDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.RemoteSyncDataSource

/**
 * Implementation of BookClubRepository.
 *
 * Coordinates between local Room database and Firestore for book club operations.
 */
class BookClubRepositoryImpl(
    private val bookClubDao: BookClubDao,
    private val bookshelfDao: BookshelfDao,
    private val remoteDataSource: RemoteSyncDataSource,
    private val codeGenerator: BookClubCodeGenerator,
    private val authService: AuthService,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : BookClubRepository {

    companion object {
        private const val TAG = "BookClubRepository"
    }

    override suspend fun createBookClub(shelfId: String): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Creating book club from shelf: %s", shelfId)

        // Get current user
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot create book club: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        // Get the shelf
        val shelfEntity = bookshelfDao.getShelfById(shelfId)
        if (shelfEntity == null) {
            Timber.tag(TAG).e("Cannot create book club: shelf not found")
            return Result.Error(DataError.Sync.DOCUMENT_NOT_FOUND)
        }

        // Check if shelf already has a book club
        if (shelfEntity.isBookClub && !shelfEntity.clubCode.isNullOrEmpty()) {
            Timber.tag(TAG).d("Shelf already has book club: %s", shelfEntity.clubCode)
            return Result.Success(shelfEntity.clubCode)
        }

        // Generate unique club code
        val codeResult = codeGenerator.generateUniqueCode()
        if (codeResult is Result.Error) {
            Timber.tag(TAG).e("Failed to generate club code: %s", codeResult.error)
            return Result.Error(codeResult.error)
        }
        val clubCode = (codeResult as Result.Success).data

        Timber.tag(TAG).d("Generated club code: %s", clubCode)

        val now = timeProvider.currentTimeMillis()

        // Create Firestore metadata
        val metadata = BookClubMetadataDto(
            code = clubCode,
            name = shelfEntity.name,
            shelfStyle = shelfEntity.shelfMaterial,
            createdBy = user.userId,
            createdByName = user.username ?: "Unknown",
            lastModifiedAt = now,
            bookCount = 0, // Will be updated after adding books
            memberCount = 1
        )

        // Upload metadata to Firestore
        val createResult = remoteDataSource.createBookClub(clubCode, metadata)
        if (createResult is Result.Error) {
            Timber.tag(TAG).e("Failed to create book club in Firestore: %s", createResult.error)
            return Result.Error(createResult.error)
        }

        // Add current user as first member
        val memberDto = BookClubMemberDto(
            userId = user.userId,
            displayName = user.username ?: "Unknown"
        )
        val memberResult = remoteDataSource.addBookClubMember(clubCode, memberDto)
        if (memberResult is Result.Error) {
            Timber.tag(TAG).e("Failed to add member to club: %s", memberResult.error)
            return Result.Error(memberResult.error)
        }

        // Upload all books from the shelf to the club
        val booksResult = uploadShelfBooksToClub(shelfId, clubCode, user.userId, user.username ?: "Unknown")
        if (booksResult is Result.Error) {
            Timber.tag(TAG).e("Failed to upload books to club: %s", booksResult.error)
            return Result.Error(booksResult.error)
        }
        val bookCount = (booksResult as Result.Success).data

        // Update book count in Firestore
        val updateCountResult = remoteDataSource.updateBookClubCounts(clubCode, bookCount, 1)
        if (updateCountResult is Result.Error) {
            Timber.tag(TAG).w("Failed to update book count: %s", updateCountResult.error)
            // Non-critical, continue
        }

        // Update local shelf to mark as book club
        bookClubDao.updateShelfBookClubStatus(shelfId, true, clubCode)

        // Create local membership record
        val membershipEntity = BookClubMembership(
            clubCode = clubCode,
            localShelfId = shelfId,
            joinedAt = now,
            lastSyncedAt = now
        ).toEntity(idGenerator.generateId())

        bookClubDao.upsertMembership(membershipEntity)

        Timber.tag(TAG).d("Book club created successfully: %s with %d books", clubCode, bookCount)
        return Result.Success(clubCode)
    }

    override suspend fun getBookClub(code: String): Result<BookClub?, DataError.Sync> {
        Timber.tag(TAG).d("Getting book club: %s", code)

        val metadataResult = remoteDataSource.getBookClubMetadata(code)
        return when (metadataResult) {
            is Result.Success -> {
                val metadata = metadataResult.data
                if (metadata != null) {
                    Result.Success(metadata.toDomain())
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

    override suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Deleting book club: %s", code)

        // Delete from Firestore
        val deleteResult = remoteDataSource.deleteBookClub(code)
        if (deleteResult is Result.Error) {
            Timber.tag(TAG).e("Failed to delete book club from Firestore: %s", deleteResult.error)
            return Result.Error(deleteResult.error)
        }

        // Delete local membership record
        bookClubDao.deleteMembership(code)

        Timber.tag(TAG).d("Book club deleted successfully: %s", code)
        return Result.Success(Unit)
    }

    override fun observeMyBookClubs(): Flow<List<BookClubMembership>> {
        return bookClubDao.observeAllMemberships().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLocalShelfForClub(code: String): Bookshelf? {
        val membership = bookClubDao.getMembershipByClubCode(code) ?: return null
        val shelfEntity = bookshelfDao.getShelfById(membership.localShelfId) ?: return null
        return shelfEntity.toDomain()
    }

    override suspend fun getClubBooks(code: String): Result<List<Book>, DataError.Sync> {
        Timber.tag(TAG).d("Getting books for club: %s", code)

        val booksResult = remoteDataSource.getClubBooks(code)
        return when (booksResult) {
            is Result.Success -> {
                val books = booksResult.data.map { it.toBook() }
                Timber.tag(TAG).d("Retrieved %d books from club", books.size)
                Result.Success(books)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get club books: %s", booksResult.error)
                Result.Error(booksResult.error)
            }
        }
    }

    // ========== Private Helpers ==========

    private suspend fun uploadShelfBooksToClub(
        shelfId: String,
        clubCode: String,
        userId: String,
        userName: String
    ): Result<Int, DataError.Sync> {
        // Get all books from the local shelf
        // We need to get books synchronously, not as a Flow
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
                    // Continue with other books
                } else {
                    uploadedCount++
                }
            }
        }

        Timber.tag(TAG).d("Uploaded %d/%d books to club", uploadedCount, crossRefs.size)
        return Result.Success(uploadedCount)
    }
}
