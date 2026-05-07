package uk.co.zlurgg.mybookshelf.bookclub.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.data.mappers.toDomain
import uk.co.zlurgg.mybookshelf.book.data.mappers.toEntity
import uk.co.zlurgg.mybookshelf.bookclub.data.mappers.toMembership
import uk.co.zlurgg.mybookshelf.bookclub.data.mappers.toMembershipEntity
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubMembershipRepository
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookClubDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMemberDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.BookClubRemoteDataSource

internal class BookClubMembershipRepositoryImpl(
    private val bookClubDao: BookClubDao,
    private val bookshelfDao: BookshelfDao,
    private val remoteDataSource: BookClubRemoteDataSource,
    private val authService: AuthService,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val helper: BookClubRepositoryHelper,
) : BookClubMembershipRepository {

    override fun observeMyBookClubs(): Flow<List<BookClubMembership>> {
        return bookClubDao.observeAllMemberships().map { entities ->
            entities.map { it.toMembership() }
        }
    }

    override suspend fun getLocalShelfForClub(code: String): Bookshelf? {
        val membership = bookClubDao.getMembershipByClubCode(code) ?: return null
        val shelfEntity = bookshelfDao.getShelfById(membership.localShelfId) ?: return null
        return shelfEntity.toDomain()
    }

    override suspend fun isMemberOfClub(code: String): Result<Boolean, DataError.Sync> {
        Timber.tag(TAG).d("Checking membership for club: %s", code)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).d("User not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        return remoteDataSource.isMember(code, user.userId)
    }

    override suspend fun joinBookClub(code: String): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Joining book club: %s", code)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot join book club: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        val clubResult = helper.fetchBookClubMetadata(code)
        if (clubResult is Result.Error) {
            return Result.Error(clubResult.error)
        }
        val club = (clubResult as Result.Success).data
        if (club == null) {
            Timber.tag(TAG).e("Cannot join: club not found")
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        val shelfName = helper.generateUniqueShelfName(club.name)

        val shelfId = idGenerator.generateId()
        val now = timeProvider.currentTimeMillis()

        val shelfEntity = Bookshelf(
            id = shelfId,
            name = shelfName,
            books = emptyList(),
            shelfStyle = club.style,
            position = 0,
            isBookClub = true,
            clubCode = code,
            clubCreatorId = club.createdBy
        ).toEntity(user.userId)

        bookshelfDao.upsertShelf(shelfEntity)

        val memberDto = BookClubMemberDto(
            userId = user.userId,
            displayName = user.username ?: "Unknown"
        )
        val memberResult = remoteDataSource.addBookClubMember(code, memberDto)
        if (memberResult is Result.Error) {
            Timber.tag(TAG).e("Failed to add member to club: %s", memberResult.error)
            bookshelfDao.deleteShelf(shelfId)
            return Result.Error(memberResult.error)
        }

        val booksResult = helper.downloadClubBooksToShelf(code, shelfId, user.userId)
        if (booksResult is Result.Error) {
            Timber.tag(TAG).w("Failed to download club books: %s", booksResult.error)
        }

        val membershipEntity = BookClubMembership(
            clubCode = code,
            localShelfId = shelfId,
            joinedAt = now,
            lastSyncedAt = now
        ).toMembershipEntity(idGenerator.generateId())

        bookClubDao.upsertMembership(membershipEntity)

        val membersResult = remoteDataSource.getBookClubMembers(code)
        if (membersResult is Result.Success) {
            val memberCount = membersResult.data.size
            remoteDataSource.updateBookClubCounts(code, club.bookCount, memberCount)
        }

        val membershipSaveResult = remoteDataSource.addClubMembership(user.userId, code)
        if (membershipSaveResult is Result.Error) {
            Timber.tag(TAG).w("Failed to save club membership to prefs: %s", membershipSaveResult.error)
        }

        Timber.tag(TAG).d("Successfully joined book club: %s, local shelf: %s", code, shelfId)
        return Result.Success(shelfId)
    }

    override suspend fun getRemoteClubMemberships(userId: String): Result<List<String>, DataError.Sync> {
        Timber.tag(TAG).d("Getting remote club memberships for user: %s", userId)

        val membershipsResult = remoteDataSource.getClubMembershipsForUser(userId)
        return when (membershipsResult) {
            is Result.Success -> {
                Timber.tag(TAG).d("Found %d club memberships", membershipsResult.data.size)
                Result.Success(membershipsResult.data)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get club memberships: %s", membershipsResult.error)
                Result.Error(membershipsResult.error)
            }
        }
    }

    override suspend fun restoreClubMembership(code: String): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Restoring book club membership: %s", code)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot restore membership: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        val existingMembership = bookClubDao.getMembershipByClubCode(code)
        if (existingMembership != null) {
            Timber.tag(TAG).d("Local membership already exists for club: %s", code)
            return Result.Success(existingMembership.localShelfId)
        }

        val clubResult = helper.fetchBookClubMetadata(code)
        if (clubResult is Result.Error) {
            return Result.Error(clubResult.error)
        }
        val club = (clubResult as Result.Success).data
        if (club == null) {
            Timber.tag(TAG).e("Cannot restore: club not found")
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        val shelfName = helper.generateUniqueShelfName(club.name)

        val shelfId = idGenerator.generateId()
        val now = timeProvider.currentTimeMillis()

        val shelfEntity = Bookshelf(
            id = shelfId,
            name = shelfName,
            books = emptyList(),
            shelfStyle = club.style,
            position = 0,
            isBookClub = true,
            clubCode = code,
            clubCreatorId = club.createdBy
        ).toEntity(user.userId)

        bookshelfDao.upsertShelf(shelfEntity)

        val booksResult = helper.downloadClubBooksToShelf(code, shelfId, user.userId)
        if (booksResult is Result.Error) {
            Timber.tag(TAG).w("Failed to download club books: %s", booksResult.error)
        }

        val membershipEntity = BookClubMembership(
            clubCode = code,
            localShelfId = shelfId,
            joinedAt = now,
            lastSyncedAt = now
        ).toMembershipEntity(idGenerator.generateId())

        bookClubDao.upsertMembership(membershipEntity)

        Timber.tag(TAG).d("Restored book club membership: %s, local shelf: %s", code, shelfId)
        return Result.Success(shelfId)
    }

    override suspend fun clearAllMemberships(): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            bookClubDao.deleteAllMemberships()
        }
    }

    companion object {
        private const val TAG = "BookClubMemberRepo"
    }
}
