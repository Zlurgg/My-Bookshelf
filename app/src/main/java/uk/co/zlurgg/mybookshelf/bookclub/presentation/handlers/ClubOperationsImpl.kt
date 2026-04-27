package uk.co.zlurgg.mybookshelf.bookclub.presentation.handlers

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.BookClubCreationResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.JoinResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.LookupResult
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations.SyncResult
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.BookClubOperationUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of ClubOperations that delegates to book club use cases and repository.
 * This bridges bookshelf screens to bookclub functionality without direct imports.
 */
class ClubOperationsImpl(
    private val bookClubUseCases: BookClubOperationUseCases,
    private val bookClubRepository: BookClubRepository
) : ClubOperations {

    @Volatile
    private var lastLookedUpCode: String? = null

    override suspend fun createBookClub(
        shelfId: String,
        shelfName: String
    ): Result<BookClubCreationResult, DataError.Sync> {
        return when (val createResult = bookClubUseCases.createBookClub(shelfId)) {
            is Result.Success -> {
                val clubCode = createResult.data
                val inviteLink = bookClubUseCases.generateInviteLink(clubCode, shelfName)
                Result.Success(BookClubCreationResult(clubCode, inviteLink))
            }
            is Result.Error -> Result.Error(createResult.error)
        }
    }

    override suspend fun lookupBookClub(codeOrUrl: String): LookupResult {
        val parseResult = bookClubUseCases.parseClubCode(codeOrUrl)
        if (parseResult is Result.Error) {
            return LookupResult.InvalidCode(parseResult.error)
        }

        val code = (parseResult as Result.Success).data

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

    override fun generateInviteLink(clubCode: String, shelfName: String): String {
        return bookClubUseCases.generateInviteLink(clubCode, shelfName)
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
        return bookClubRepository.deleteBookClub(clubCode)
    }

    override suspend fun syncBookToClub(clubCode: String, book: Book): Result<Unit, DataError.Sync> {
        return bookClubRepository.syncBookToClub(clubCode, book)
    }

    override suspend fun removeBookFromClub(clubCode: String, bookId: String): Result<Unit, DataError.Sync> {
        return bookClubRepository.removeBookFromClub(clubCode, bookId)
    }

    override suspend fun updateClubStyle(clubCode: String, styleName: String): Result<Unit, DataError.Sync> {
        return bookClubRepository.updateClubStyle(clubCode, styleName)
    }

    override suspend fun clearAllMemberships(): Result<Unit, DataError.Local> {
        return bookClubRepository.clearAllMemberships()
    }

    override suspend fun renameBookClub(clubCode: String, newName: String): Result<Unit, DataError> {
        return bookClubRepository.renameBookClub(clubCode, newName)
    }
}
