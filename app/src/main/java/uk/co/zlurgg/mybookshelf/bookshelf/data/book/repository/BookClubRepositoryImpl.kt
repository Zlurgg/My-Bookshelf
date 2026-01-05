package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toBookClubBookDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toDomain
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookClubCodeGenerator
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

        // Get the source shelf
        val sourceShelf = bookshelfDao.getShelfById(shelfId)
        if (sourceShelf == null) {
            Timber.tag(TAG).e("Cannot create book club: shelf not found")
            return Result.Error(DataError.Sync.DOCUMENT_NOT_FOUND)
        }

        // Check if this shelf is already a book club (don't allow creating club from club)
        if (sourceShelf.isBookClub && !sourceShelf.clubCode.isNullOrEmpty()) {
            Timber.tag(TAG).d("Shelf is already a book club: %s", sourceShelf.clubCode)
            return Result.Success(sourceShelf.clubCode)
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
            name = sourceShelf.name,
            shelfStyle = sourceShelf.shelfMaterial,
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

        // Upload all books from the source shelf to the club
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

        // Create a NEW local shelf for the book club (keep original untouched)
        val clubShelfName = generateUniqueShelfName(sourceShelf.name)
        val clubShelfId = idGenerator.generateId()

        val clubShelfEntity = Bookshelf(
            id = clubShelfId,
            name = clubShelfName,
            books = emptyList(),
            shelfStyle = sourceShelf.shelfMaterial.let { ShelfStyle.valueOf(it) },
            position = 0, // Position at top
            isBookClub = true,
            clubCode = clubCode,
            clubCreatorId = user.userId  // Creator is the current user
        ).toEntity(user.userId)

        bookshelfDao.upsertShelf(clubShelfEntity)

        // Copy books from source shelf to club shelf
        val sourceBookIds = bookClubDao.getBookIdsForShelf(shelfId)
        for (bookId in sourceBookIds) {
            val crossRef = BookshelfBookCrossRef(
                shelfId = clubShelfId,
                bookId = bookId,
                addedAt = now
            )
            bookshelfDao.upsertCrossRef(crossRef)
        }

        // Create local membership record pointing to the new club shelf
        val membershipEntity = BookClubMembership(
            clubCode = clubCode,
            localShelfId = clubShelfId,
            joinedAt = now,
            lastSyncedAt = now
        ).toEntity(idGenerator.generateId())

        bookClubDao.upsertMembership(membershipEntity)

        // Save club code to user preferences for restore on sign-in
        val membershipSaveResult = remoteDataSource.addClubMembership(user.userId, clubCode)
        if (membershipSaveResult is Result.Error) {
            Timber.tag(TAG).w("Failed to save club membership to prefs: %s", membershipSaveResult.error)
            // Non-critical, continue - local state is saved
        }

        Timber.tag(TAG).d("Book club created successfully: %s with %d books, local shelf: %s", clubCode, bookCount, clubShelfId)
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

        // 1. Verify user is signed in
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot delete book club: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        // 2. Get club metadata to verify creator
        val clubResult = getBookClub(code)
        if (clubResult is Result.Error) {
            return Result.Error(clubResult.error)
        }
        val club = (clubResult as Result.Success).data
        if (club == null) {
            Timber.tag(TAG).e("Cannot delete: club not found")
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        // 3. Permission check - only creator can delete
        if (club.createdBy != user.userId) {
            Timber.tag(TAG).w("PERMISSION DENIED: User '%s' is not creator '%s' of club %s", user.userId, club.createdBy, code)
            return Result.Error(DataError.Sync.PERMISSION_DENIED)
        }

        // 4. Get all members BEFORE deleting to clean up their settings
        val membersResult = remoteDataSource.getBookClubMembers(code)
        val memberIds = if (membersResult is Result.Success) {
            membersResult.data.map { it.userId }
        } else {
            Timber.tag(TAG).w("Failed to get members, proceeding with deletion")
            emptyList()
        }

        // 5. Clean up each member's club_memberships in user settings
        for (memberId in memberIds) {
            val removeResult = remoteDataSource.removeClubMembership(memberId, code)
            if (removeResult is Result.Error) {
                Timber.tag(TAG).w("Failed to remove membership for user %s: %s", memberId, removeResult.error)
                // Continue with other members
            }
        }

        // 6. Delete Firestore subcollections and document
        val deleteResult = remoteDataSource.deleteBookClub(code)
        if (deleteResult is Result.Error) {
            Timber.tag(TAG).e("Failed to delete book club from Firestore: %s", deleteResult.error)
            return Result.Error(deleteResult.error)
        }

        // 7. Delete local membership record
        // Note: Local shelf deletion is handled by DeleteShelfUseCaseImpl
        bookClubDao.deleteMembership(code)

        Timber.tag(TAG).d("Book club deleted successfully: %s", code)
        return Result.Success(Unit)
    }

    override suspend fun renameBookClub(code: String, newName: String): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Renaming book club %s to: %s", code, newName)

        val user = authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        // Get club to verify creator
        val clubResult = getBookClub(code)
        if (clubResult is Result.Error) return clubResult
        val club = (clubResult as Result.Success).data
            ?: return Result.Error(DataError.Sync.CLUB_NOT_FOUND)

        // Only creator can rename
        if (club.createdBy != user.userId) {
            Timber.tag(TAG).w("User %s is not creator of club %s", user.userId, code)
            return Result.Error(DataError.Sync.PERMISSION_DENIED)
        }

        // Update Firestore
        val updateResult = remoteDataSource.updateBookClubName(
            code, newName, timeProvider.currentTimeMillis()
        )
        if (updateResult is Result.Error) {
            Timber.tag(TAG).e("Failed to update club name in Firestore: %s", updateResult.error)
            return Result.Error(updateResult.error)
        }

        // Update local shelf
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

        // Get club to verify creator
        val clubResult = getBookClub(code)
        if (clubResult is Result.Error) return clubResult
        val club = (clubResult as Result.Success).data
            ?: return Result.Error(DataError.Sync.CLUB_NOT_FOUND)

        // Only creator can update style
        if (club.createdBy != user.userId) {
            Timber.tag(TAG).w("User %s is not creator of club %s", user.userId, code)
            return Result.Error(DataError.Sync.PERMISSION_DENIED)
        }

        // Update Firestore
        val updateResult = remoteDataSource.updateBookClubStyle(
            code, style, timeProvider.currentTimeMillis()
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

        // 1. Verify user is signed in
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot leave book club: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        // 2. Get club metadata to check creator
        val clubResult = getBookClub(code)
        if (clubResult is Result.Error) {
            return Result.Error(clubResult.error)
        }
        val club = (clubResult as Result.Success).data
        if (club == null) {
            Timber.tag(TAG).e("Cannot leave: club not found")
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        // 3. Creator cannot leave - they must delete the club instead
        if (club.createdBy == user.userId) {
            Timber.tag(TAG).w("Creator cannot leave club %s - must delete instead", code)
            return Result.Error(DataError.Sync.CREATOR_CANNOT_LEAVE)
        }

        // 4. Get local membership for cleanup later
        val membership = bookClubDao.getMembershipByClubCode(code)
        val localShelfId = membership?.localShelfId

        // 5. Remove user from Firestore members collection
        val removeMemberResult = remoteDataSource.removeBookClubMember(code, user.userId)
        if (removeMemberResult is Result.Error) {
            Timber.tag(TAG).e("Failed to remove member from club: %s", removeMemberResult.error)
            return Result.Error(removeMemberResult.error)
        }

        // 6. Remove from user settings (club_memberships in preferences)
        val removeFromSettingsResult = remoteDataSource.removeClubMembership(user.userId, code)
        if (removeFromSettingsResult is Result.Error) {
            Timber.tag(TAG).w("Failed to remove from user settings: %s", removeFromSettingsResult.error)
            // Non-critical, continue
        }

        // 7. Decrement member count in Firestore
        // Use stored member count minus 1 (the user we just removed)
        val newMemberCount = maxOf(0, club.memberCount - 1)
        val updateCountResult = remoteDataSource.updateBookClubCounts(code, club.bookCount, newMemberCount)
        if (updateCountResult is Result.Error) {
            Timber.tag(TAG).w("Failed to update member count: %s", updateCountResult.error)
            // Non-critical, continue - member has been removed
        } else {
            Timber.tag(TAG).d("Updated member count to: %d", newMemberCount)
        }

        // 8. Delete local membership record
        bookClubDao.deleteMembership(code)

        // 9. Delete local shelf if it exists
        if (localShelfId != null) {
            bookshelfDao.deleteShelf(localShelfId)
            Timber.tag(TAG).d("Deleted local shelf: %s", localShelfId)
        }

        Timber.tag(TAG).d("Successfully left book club: %s", code)
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

        // Get current user
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot join book club: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        // Get club metadata
        val clubResult = getBookClub(code)
        if (clubResult is Result.Error) {
            return Result.Error(clubResult.error)
        }
        val club = (clubResult as Result.Success).data
        if (club == null) {
            Timber.tag(TAG).e("Cannot join: club not found")
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        // Generate unique local shelf name
        val shelfName = generateUniqueShelfName(club.name)

        // Create local shelf with correct owner
        val shelfId = idGenerator.generateId()
        val now = timeProvider.currentTimeMillis()

        val shelfEntity = Bookshelf(
            id = shelfId,
            name = shelfName,
            books = emptyList(),
            shelfStyle = club.style,
            position = 0, // Will be positioned at top
            isBookClub = true,
            clubCode = code,
            clubCreatorId = club.createdBy  // Store creator ID from club metadata
        ).toEntity(user.userId)

        bookshelfDao.upsertShelf(shelfEntity)

        // Add user as member in Firestore
        val memberDto = BookClubMemberDto(
            userId = user.userId,
            displayName = user.username ?: "Unknown"
        )
        val memberResult = remoteDataSource.addBookClubMember(code, memberDto)
        if (memberResult is Result.Error) {
            Timber.tag(TAG).e("Failed to add member to club: %s", memberResult.error)
            // Clean up local shelf
            bookshelfDao.deleteShelf(shelfId)
            return Result.Error(memberResult.error)
        }

        // Download all club books with correct owner
        val booksResult = downloadClubBooksToShelf(code, shelfId, user.userId)
        if (booksResult is Result.Error) {
            Timber.tag(TAG).w("Failed to download club books: %s", booksResult.error)
            // Non-critical, continue - user can sync later
        }

        // Create local membership record
        val membershipEntity = BookClubMembership(
            clubCode = code,
            localShelfId = shelfId,
            joinedAt = now,
            lastSyncedAt = now
        ).toEntity(idGenerator.generateId())

        bookClubDao.upsertMembership(membershipEntity)

        // Update member count in Firestore
        val membersResult = remoteDataSource.getBookClubMembers(code)
        if (membersResult is Result.Success) {
            val memberCount = membersResult.data.size
            remoteDataSource.updateBookClubCounts(code, club.bookCount, memberCount)
        }

        // Save club code to user preferences for restore on sign-in
        val membershipSaveResult = remoteDataSource.addClubMembership(user.userId, code)
        if (membershipSaveResult is Result.Error) {
            Timber.tag(TAG).w("Failed to save club membership to prefs: %s", membershipSaveResult.error)
            // Non-critical, continue - local state is saved
        }

        Timber.tag(TAG).d("Successfully joined book club: %s, local shelf: %s", code, shelfId)
        return Result.Success(shelfId)
    }

    override suspend fun getRemoteClubMemberships(userId: String): Result<List<String>, DataError.Sync> {
        Timber.tag(TAG).d("Getting remote club memberships for user: %s", userId)

        // Read club memberships from user preferences (denormalized approach)
        val prefsResult = remoteDataSource.getUserPreferences(userId)
        return when (prefsResult) {
            is Result.Success -> {
                val memberships = prefsResult.data?.clubMemberships ?: emptyList()
                Timber.tag(TAG).d("Found %d club memberships in user prefs", memberships.size)
                Result.Success(memberships)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get user preferences: %s", prefsResult.error)
                Result.Error(prefsResult.error)
            }
        }
    }

    override suspend fun restoreClubMembership(code: String): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Restoring book club membership: %s", code)

        // Get current user
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot restore membership: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
        }

        // Check if local shelf already exists
        val existingMembership = bookClubDao.getMembershipByClubCode(code)
        if (existingMembership != null) {
            Timber.tag(TAG).d("Local membership already exists for club: %s", code)
            return Result.Success(existingMembership.localShelfId)
        }

        // Get club metadata
        val clubResult = getBookClub(code)
        if (clubResult is Result.Error) {
            return Result.Error(clubResult.error)
        }
        val club = (clubResult as Result.Success).data
        if (club == null) {
            Timber.tag(TAG).e("Cannot restore: club not found")
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        // Generate unique local shelf name
        val shelfName = generateUniqueShelfName(club.name)

        // Create local shelf with correct owner
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
            clubCreatorId = club.createdBy  // Store creator ID from club metadata
        ).toEntity(user.userId)

        bookshelfDao.upsertShelf(shelfEntity)

        // Download club books (user is already a member in Firestore)
        val booksResult = downloadClubBooksToShelf(code, shelfId, user.userId)
        if (booksResult is Result.Error) {
            Timber.tag(TAG).w("Failed to download club books: %s", booksResult.error)
            // Non-critical, continue
        }

        // Create local membership record
        val membershipEntity = BookClubMembership(
            clubCode = code,
            localShelfId = shelfId,
            joinedAt = now,
            lastSyncedAt = now
        ).toEntity(idGenerator.generateId())

        bookClubDao.upsertMembership(membershipEntity)

        Timber.tag(TAG).d("Restored book club membership: %s, local shelf: %s", code, shelfId)
        return Result.Success(shelfId)
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

    private suspend fun generateUniqueShelfName(clubName: String): String {
        // Use club name directly - badge [BC] will indicate it's a book club
        val baseName = clubName

        // Check if name exists
        bookshelfDao.getShelfByName(baseName) ?: return baseName

        // Find unique name with numeric suffix
        var counter = 2
        while (true) {
            val candidateName = "$clubName $counter"
            bookshelfDao.getShelfByName(candidateName) ?: return candidateName
            counter++
            if (counter > 100) {
                // Safety limit - just use the base name and let it overwrite
                Timber.tag(TAG).w("Could not find unique name after 100 attempts")
                return baseName
            }
        }
    }

    private suspend fun downloadClubBooksToShelf(
        clubCode: String,
        shelfId: String,
        userId: String
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
                // Debug: Log the DTO values
                Timber.tag(TAG).d("BookDTO - id: %s, title: %s, coverUrl: %s, spineColor: %d",
                    bookDto.id, bookDto.title, bookDto.coverUrl, bookDto.spineColor)

                // Convert to Book domain model and save locally with correct owner
                val book = bookDto.toBook()
                Timber.tag(TAG).d("Book - id: %s, imageUrl: %s, spineColor: %d",
                    book.id, book.imageUrl, book.spineColor)

                val bookEntity = book.toBookEntity(userId)
                Timber.tag(TAG).d("BookEntity - id: %s, imageUrl: %s, spineColor: %d",
                    bookEntity.id, bookEntity.imageUrl, bookEntity.spineColor)

                // Upsert the book (in case it already exists)
                bookshelfDao.upsert(bookEntity)

                // Add to shelf via cross-reference
                val crossRef = BookshelfBookCrossRef(
                    shelfId = shelfId,
                    bookId = book.id,
                    addedAt = timeProvider.currentTimeMillis()
                )
                bookshelfDao.upsertCrossRef(crossRef)
                addedCount++
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to add book %s to local shelf", bookDto.id)
                // Continue with other books
            }
        }

        Timber.tag(TAG).d("Downloaded %d/%d books to local shelf", addedCount, clubBooks.size)
        return Result.Success(addedCount)
    }

    // ========== Book Sync Operations ==========

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
                // Update book count
                updateClubBookCount(code)
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
                // Update book count
                updateClubBookCount(code)
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to remove book %s from club: %s", bookId, removeResult.error)
                Result.Error(removeResult.error)
            }
        }
    }

    private suspend fun updateClubBookCount(code: String) {
        // Get current book count from Firestore
        val booksResult = remoteDataSource.getClubBooks(code)
        if (booksResult is Result.Success) {
            val bookCount = booksResult.data.size
            // Get current member count from metadata
            val metadataResult = remoteDataSource.getBookClubMetadata(code)
            if (metadataResult is Result.Success && metadataResult.data != null) {
                val memberCount = metadataResult.data.memberCount
                remoteDataSource.updateBookClubCounts(code, bookCount, memberCount)
            }
        }
    }

    // ========== Sync Operations ==========

    override suspend fun syncBooksFromClub(
        code: String,
        localShelfId: String
    ): Result<SyncResult, DataError.Sync> {
        Timber.tag(TAG).d("Syncing books from club %s to shelf %s", code, localShelfId)

        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).e("Cannot sync: not signed in")
            return Result.Error(DataError.Sync.NOT_SIGNED_IN)
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
                Timber.tag(TAG).d("Adding missing book: %s", bookDto.title)
                val book = bookDto.toBook()
                val bookEntity = book.toBookEntity(user.userId)
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
                Timber.tag(TAG).d("Removing deleted book: %s", bookId)
                bookshelfDao.deleteCrossRef(localShelfId, bookId)
                booksRemoved++
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to remove book %s", bookId)
            }
        }

        // Sync club name from Firestore to local shelf
        val metadataResult = remoteDataSource.getBookClubMetadata(code)
        if (metadataResult is Result.Success && metadataResult.data != null) {
            val clubMetadata = metadataResult.data
            val localShelf = bookshelfDao.getShelfById(localShelfId)
            if (localShelf != null && localShelf.name != clubMetadata.name) {
                // Update local shelf name to match Firestore
                bookshelfDao.upsertShelf(localShelf.copy(name = clubMetadata.name))
                Timber.tag(TAG).d("Updated local shelf name to: %s", clubMetadata.name)
            }
        }

        // Update last synced timestamp
        val membership = bookClubDao.getMembershipByClubCode(code)
        if (membership != null) {
            val updatedMembership = membership.copy(lastSyncedAt = timeProvider.currentTimeMillis())
            bookClubDao.upsertMembership(updatedMembership)
        }

        Timber.tag(TAG).d("Sync complete: added %d, removed %d books", booksAdded, booksRemoved)
        return Result.Success(SyncResult(booksAdded, booksRemoved))
    }
}
