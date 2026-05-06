package uk.co.zlurgg.mybookshelf.bookclub.presentation.handlers

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.BookClubCreationResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.JoinResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.LookupResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.SyncResult
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.BookClubOperationUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of ClubOperations that delegates to book club use cases.
 * This bridges bookshelf screens to bookclub functionality without direct imports.
 */
class ClubOperationsImpl(
    private val bookClubUseCases: BookClubOperationUseCases
) : ClubOperations {

    // @Volatile provides visibility across coroutine dispatchers.
    // A theoretical race exists between lookup and join, but in practice all
    // calls originate from the same ViewModel scope on Main dispatcher.
    // If multi-dispatcher usage is needed, replace with AtomicReference.
    @Volatile
    private var lastLookedUpCode: String? = null

    override suspend fun createBookClub(
        shelfId: String,
        shelfName: String
    ): Result<BookClubCreationResult, DataError.Sync> {
        return when (val createResult = bookClubUseCases.createBookClub(shelfId)) {
            is Result.Success -> {
                val clubCode = createResult.data
                Result.Success(BookClubCreationResult(clubCode))
            }
            is Result.Error -> Result.Error(createResult.error)
        }
    }

    override suspend fun lookupBookClub(codeOrUrl: String): LookupResult {
        val parseResult = bookClubUseCases.parseClubCode(codeOrUrl)
        when (parseResult) {
            is Result.Error -> return LookupResult.InvalidCode(parseResult.error)
            is Result.Success -> {
                val code = parseResult.data
                return when (val previewResult = bookClubUseCases.getBookClubPreview(code)) {
                    is Result.Success -> {
                        val bookClub = previewResult.data
                        if (bookClub != null) {
                            lastLookedUpCode = code
                            LookupResult.Found(bookClub.name, code, bookClub.memberCount)
                        } else {
                            LookupResult.NotFound(DataError.Sync.CLUB_NOT_FOUND)
                        }
                    }
                    is Result.Error -> LookupResult.NotFound(previewResult.error)
                }
            }
        }
    }

    override suspend fun joinBookClub(): Result<JoinResult, DataError.Sync> {
        val code = lastLookedUpCode
            ?: return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        return joinBookClub(code)
    }

    override suspend fun joinBookClub(code: String): Result<JoinResult, DataError.Sync> {
        lastLookedUpCode = code
        return when (val result = bookClubUseCases.joinBookClub(code)) {
            is Result.Success -> {
                val joinResult = result.data
                val mapped = when (joinResult) {
                    is uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.JoinResult.Success ->
                        JoinResult.Success(joinResult.shelfName)
                    is uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.JoinResult.AlreadyMember ->
                        JoinResult.AlreadyMember
                }
                Result.Success(mapped)
            }
            is Result.Error -> Result.Error(result.error)
        }
    }

    override fun clearLookupState() {
        lastLookedUpCode = null
    }

    override suspend fun syncBooksFromClub(
        clubCode: String,
        localShelfId: String
    ): Result<SyncResult, DataError.Sync> {
        return when (val result = bookClubUseCases.syncBookClub(clubCode, localShelfId)) {
            is Result.Success -> Result.Success(SyncResult(result.data.booksAdded, result.data.booksRemoved))
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun leaveBookClub(shelfId: String): Result<Unit, DataError.Sync> {
        return bookClubUseCases.leaveBookClub(shelfId)
    }

    override suspend fun validateMemberships(): List<String> {
        return when (val result = bookClubUseCases.validateMemberships()) {
            is Result.Success -> result.data
            is Result.Error -> emptyList()
        }
    }

    override suspend fun deleteBookClub(clubCode: String): Result<Unit, DataError.Sync> {
        return bookClubUseCases.deleteBookClub(clubCode)
    }

    override suspend fun syncBookToClub(clubCode: String, book: Book): Result<Unit, DataError.Sync> {
        return bookClubUseCases.syncBookToClub(clubCode, book)
    }

    override suspend fun removeBookFromClub(clubCode: String, bookId: String): Result<Unit, DataError.Sync> {
        return bookClubUseCases.removeBookFromClub(clubCode, bookId)
    }

    override suspend fun updateClubStyle(clubCode: String, styleName: String): Result<Unit, DataError.Sync> {
        return bookClubUseCases.updateClubStyle(clubCode, styleName)
    }

    override suspend fun clearAllMemberships(): Result<Unit, DataError.Local> {
        return bookClubUseCases.clearClubMemberships()
    }

    override suspend fun renameBookClub(clubCode: String, newName: String): Result<Unit, DataError> {
        return bookClubUseCases.renameBookClub(clubCode, newName)
    }

    override suspend fun getClubsCreatedByUser(userId: String): Result<List<String>, DataError.Sync> {
        return bookClubUseCases.getClubsCreatedByUser(userId)
    }

    override suspend fun getClubMembershipsForUser(userId: String): Result<List<String>, DataError.Sync> {
        return bookClubUseCases.getClubMembershipsForUser(userId)
    }

    override suspend fun removeUserFromClub(clubCode: String, userId: String): Result<Unit, DataError.Sync> {
        return bookClubUseCases.removeUserFromClub(clubCode, userId)
    }
}
