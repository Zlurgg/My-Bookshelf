package uk.co.zlurgg.mybookshelf.bookclub.data.repository

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.data.mappers.toEntity
import uk.co.zlurgg.mybookshelf.bookclub.data.mappers.toMembershipEntity
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubManagementRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.service.BookClubCodeGenerator
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookClubDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMemberDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMetadataDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.RemoteSyncDataSource

internal class BookClubManagementRepositoryImpl(
    private val bookClubDao: BookClubDao,
    private val bookshelfDao: BookshelfDao,
    private val remoteDataSource: RemoteSyncDataSource,
    private val codeGenerator: BookClubCodeGenerator,
    private val authService: AuthService,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val helper: BookClubRepositoryHelper,
) : BookClubManagementRepository {

    override suspend fun createBookClub(shelfId: String): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Creating book club from shelf: %s", shelfId)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot create book club: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        val sourceShelf = bookshelfDao.getShelfById(shelfId)
        if (sourceShelf == null) {
            Timber.tag(TAG).e("Cannot create book club: shelf not found")
            return Result.Error(DataError.Sync.DOCUMENT_NOT_FOUND)
        }

        if (sourceShelf.isBookClub && !sourceShelf.clubCode.isNullOrEmpty()) {
            Timber.tag(TAG).d("Shelf is already a book club: %s", sourceShelf.clubCode)
            return Result.Success(sourceShelf.clubCode)
        }

        val codeResult = codeGenerator.generateUniqueCode()
        if (codeResult is Result.Error) {
            Timber.tag(TAG).e("Failed to generate club code: %s", codeResult.error)
            return Result.Error(codeResult.error)
        }
        val clubCode = (codeResult as Result.Success).data

        Timber.tag(TAG).d("Generated club code: %s", clubCode)

        val now = timeProvider.currentTimeMillis()

        val metadata = BookClubMetadataDto(
            code = clubCode,
            name = sourceShelf.name,
            shelfStyle = sourceShelf.shelfMaterial,
            createdBy = user.userId,
            createdByName = user.username ?: "Unknown",
            lastModifiedAt = now,
            bookCount = 0,
            memberCount = 1
        )

        val createResult = remoteDataSource.createBookClub(clubCode, metadata)
        if (createResult is Result.Error) {
            Timber.tag(TAG).e("Failed to create book club in Firestore: %s", createResult.error)
            return Result.Error(createResult.error)
        }

        val memberDto = BookClubMemberDto(
            userId = user.userId,
            displayName = user.username ?: "Unknown"
        )
        val memberResult = remoteDataSource.addBookClubMember(clubCode, memberDto)
        if (memberResult is Result.Error) {
            Timber.tag(TAG).e("Failed to add member to club: %s", memberResult.error)
            return Result.Error(memberResult.error)
        }

        val booksResult = helper.uploadShelfBooksToClub(shelfId, clubCode, user.userId, user.username ?: "Unknown")
        if (booksResult is Result.Error) {
            Timber.tag(TAG).e("Failed to upload books to club: %s", booksResult.error)
            return Result.Error(booksResult.error)
        }
        val bookCount = (booksResult as Result.Success).data

        val updateCountResult = remoteDataSource.updateBookClubCounts(clubCode, bookCount, 1)
        if (updateCountResult is Result.Error) {
            Timber.tag(TAG).w("Failed to update book count: %s", updateCountResult.error)
        }

        val clubShelfName = helper.generateUniqueShelfName(sourceShelf.name)
        val clubShelfId = idGenerator.generateId()

        val clubShelfEntity = Bookshelf(
            id = clubShelfId,
            name = clubShelfName,
            books = emptyList(),
            shelfStyle = sourceShelf.shelfMaterial.let { ShelfStyle.valueOf(it) },
            position = 0,
            isBookClub = true,
            clubCode = clubCode,
            clubCreatorId = user.userId
        ).toEntity(user.userId)

        bookshelfDao.upsertShelf(clubShelfEntity)

        val sourceBookIds = bookClubDao.getBookIdsForShelf(shelfId)
        for (bookId in sourceBookIds) {
            val crossRef = BookshelfBookCrossRef(
                shelfId = clubShelfId,
                bookId = bookId,
                addedAt = now
            )
            bookshelfDao.upsertCrossRef(crossRef)
        }

        val membershipEntity = BookClubMembership(
            clubCode = clubCode,
            localShelfId = clubShelfId,
            joinedAt = now,
            lastSyncedAt = now
        ).toMembershipEntity(idGenerator.generateId())

        bookClubDao.upsertMembership(membershipEntity)

        val membershipSaveResult = remoteDataSource.addClubMembership(user.userId, clubCode)
        if (membershipSaveResult is Result.Error) {
            Timber.tag(TAG).w("Failed to save club membership to prefs: %s", membershipSaveResult.error)
        }

        Timber.tag(TAG).d(
            "Book club created successfully: %s with %d books, local shelf: %s",
            clubCode,
            bookCount,
            clubShelfId
        )
        return Result.Success(clubCode)
    }

    override suspend fun getBookClub(code: String): Result<BookClub?, DataError.Sync> {
        return helper.fetchBookClubMetadata(code)
    }

    override suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Deleting book club: %s", code)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot delete book club: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        val clubResult = helper.fetchBookClubMetadata(code)
        if (clubResult is Result.Error) {
            return Result.Error(clubResult.error)
        }
        val club = (clubResult as Result.Success).data
        if (club == null) {
            Timber.tag(TAG).e("Cannot delete: club not found")
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        if (club.createdBy != user.userId) {
            Timber.tag(TAG).w(
                "PERMISSION DENIED: User '%s' is not creator '%s' of club %s",
                user.userId,
                club.createdBy,
                code
            )
            return Result.Error(DataError.Sync.PERMISSION_DENIED)
        }

        val deleteResult = remoteDataSource.deleteBookClub(code)
        if (deleteResult is Result.Error) {
            Timber.tag(TAG).e("Failed to delete book club from Firestore: %s", deleteResult.error)
            return Result.Error(deleteResult.error)
        }

        val removeResult = remoteDataSource.removeClubMembership(user.userId, code)
        if (removeResult is Result.Error) {
            Timber.tag(TAG).w("Failed to remove from user preferences: %s", removeResult.error)
        }

        bookClubDao.deleteMembership(code)

        Timber.tag(TAG).d("Book club deleted successfully: %s", code)
        return Result.Success(Unit)
    }

    override suspend fun renameBookClub(code: String, newName: String): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Renaming book club %s to: %s", code, newName)

        val user = authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        val clubResult = helper.fetchBookClubMetadata(code)
        if (clubResult is Result.Error) return clubResult
        val club = (clubResult as Result.Success).data
            ?: return Result.Error(DataError.Sync.CLUB_NOT_FOUND)

        if (club.createdBy != user.userId) {
            Timber.tag(TAG).w("User %s is not creator of club %s", user.userId, code)
            return Result.Error(DataError.Sync.PERMISSION_DENIED)
        }

        val updateResult = remoteDataSource.updateBookClubName(
            code,
            newName,
            timeProvider.currentTimeMillis()
        )
        if (updateResult is Result.Error) {
            Timber.tag(TAG).e("Failed to update club name in Firestore: %s", updateResult.error)
            return Result.Error(updateResult.error)
        }

        val membership = bookClubDao.getMembershipByClubCode(code)
        if (membership != null) {
            val localShelf = bookshelfDao.getShelfById(membership.localShelfId)
            if (localShelf != null) {
                bookshelfDao.upsertShelf(localShelf.copy(name = newName))
                Timber.tag(TAG).d("Updated local shelf name to: %s", newName)
            }
        }

        Timber.tag(TAG).d("Book club renamed successfully")
        return Result.Success(Unit)
    }

    override suspend fun updateClubStyle(code: String, style: String): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Updating book club %s style to: %s", code, style)

        val user = authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        val clubResult = helper.fetchBookClubMetadata(code)
        if (clubResult is Result.Error) return clubResult
        val club = (clubResult as Result.Success).data
            ?: return Result.Error(DataError.Sync.CLUB_NOT_FOUND)

        if (club.createdBy != user.userId) {
            Timber.tag(TAG).w("User %s is not creator of club %s", user.userId, code)
            return Result.Error(DataError.Sync.PERMISSION_DENIED)
        }

        val updateResult = remoteDataSource.updateBookClubStyle(
            code,
            style,
            timeProvider.currentTimeMillis()
        )
        if (updateResult is Result.Error) {
            Timber.tag(TAG).e("Failed to update club style in Firestore: %s", updateResult.error)
            return Result.Error(updateResult.error)
        }

        Timber.tag(TAG).d("Book club style updated successfully")
        return Result.Success(Unit)
    }

    override suspend fun leaveBookClub(code: String): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Leaving book club: %s", code)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot leave book club: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        val membership = bookClubDao.getMembershipByClubCode(code)
        val localShelfId = membership?.localShelfId

        val clubResult = helper.fetchBookClubMetadata(code)
        val club = when (clubResult) {
            is Result.Success -> clubResult.data
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to fetch club: %s", clubResult.error)
                return Result.Error(clubResult.error)
            }
        }

        if (club == null) {
            Timber.tag(TAG).d("Club %s was deleted, cleaning up locally", code)
            helper.cleanupLocalClubData(code, localShelfId, user.userId)
            return Result.Success(Unit)
        }

        if (club.createdBy == user.userId) {
            Timber.tag(TAG).w("Creator cannot leave club %s - must delete instead", code)
            return Result.Error(DataError.Sync.CREATOR_CANNOT_LEAVE)
        }

        val membersResult = remoteDataSource.getBookClubMembers(code)
        if (membersResult is Result.Success) {
            val newMemberCount = maxOf(0, membersResult.data.size - 1)
            val updateCountResult = remoteDataSource.updateBookClubCounts(code, club.bookCount, newMemberCount)
            if (updateCountResult is Result.Error) {
                Timber.tag(TAG).w("Failed to update member count: %s", updateCountResult.error)
            } else {
                Timber.tag(TAG).d("Updated member count to: %d", newMemberCount)
            }
        } else {
            Timber.tag(TAG).w("Failed to get members for count update")
        }

        val removeMemberResult = remoteDataSource.removeBookClubMember(code, user.userId)
        if (removeMemberResult is Result.Error) {
            Timber.tag(TAG).e("Failed to remove member from club: %s", removeMemberResult.error)
            return Result.Error(removeMemberResult.error)
        }

        val removeFromSettingsResult = remoteDataSource.removeClubMembership(user.userId, code)
        if (removeFromSettingsResult is Result.Error) {
            Timber.tag(TAG).w("Failed to remove from user settings: %s", removeFromSettingsResult.error)
        }

        helper.cleanupLocalClubData(code, localShelfId, user.userId)

        Timber.tag(TAG).d("Successfully left book club: %s", code)
        return Result.Success(Unit)
    }

    override suspend fun convertClubToPersonalShelf(code: String): Result<Unit, DataError.Sync> {
        return helper.convertToPersonalShelf(code)
    }

    companion object {
        private const val TAG = "BookClubMgmtRepo"
    }
}
