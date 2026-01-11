package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookclub.handlers

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.SyncResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.BookClubUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.JoinResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Handler for book club operations shared across ViewModels.
 * Extracts common logic for creating book clubs and generating invite links.
 */
class BookClubOperationsHandler(
    private val bookClubUseCases: BookClubUseCases
) {
    /**
     * Result of a successful book club creation.
     */
    data class BookClubCreationResult(
        val clubCode: String,
        val inviteLink: String
    )

    /**
     * Result of a book club lookup operation.
     */
    sealed class LookupResult {
        data class Found(val bookClub: BookClub, val code: String) : LookupResult()
        data class NotFound(val error: DataError) : LookupResult()
        data class InvalidCode(val error: DataError.Validation) : LookupResult()
    }

    // Store the last looked-up code for joining
    // @Volatile ensures visibility across coroutine dispatchers
    @Volatile
    private var lastLookedUpCode: String? = null

    /**
     * Creates a book club from a shelf and generates an invite link.
     *
     * @param shelfId The ID of the shelf to create a book club from
     * @param shelfName The name of the shelf (used for invite link)
     * @return Success with club code and invite link, or Error
     */
    suspend fun createBookClub(
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

    /**
     * Looks up a book club from a code or URL input.
     * Parses the input to extract the code, then fetches the club preview.
     *
     * @param codeOrUrl The club code or invite URL
     * @return LookupResult indicating found, not found, or invalid code
     */
    suspend fun lookupBookClub(codeOrUrl: String): LookupResult {
        // Parse the code from input (handles raw codes and URLs)
        val parseResult = bookClubUseCases.parseClubCode(codeOrUrl)
        if (parseResult is Result.Error) {
            return LookupResult.InvalidCode(parseResult.error)
        }

        val code = (parseResult as Result.Success).data

        // Fetch the club preview
        return when (val previewResult = bookClubUseCases.getBookClubPreview(code)) {
            is Result.Success -> {
                val bookClub = previewResult.data
                if (bookClub != null) {
                    lastLookedUpCode = code
                    LookupResult.Found(bookClub, code)
                } else {
                    LookupResult.NotFound(DataError.Sync.CLUB_NOT_FOUND)
                }
            }
            is Result.Error -> LookupResult.NotFound(previewResult.error)
        }
    }

    /**
     * Joins the most recently looked-up book club.
     *
     * @return Result with JoinResult on success, or DataError.Sync on failure
     */
    suspend fun joinBookClub(): Result<JoinResult, DataError.Sync> {
        val code = lastLookedUpCode
            ?: return Result.Error(DataError.Sync.CLUB_NOT_FOUND)

        return bookClubUseCases.joinBookClub(code)
    }

    /**
     * Joins a specific book club by code.
     *
     * @param code The club code to join
     * @return Result with JoinResult on success, or DataError.Sync on failure
     */
    suspend fun joinBookClub(code: String): Result<JoinResult, DataError.Sync> {
        lastLookedUpCode = code
        return bookClubUseCases.joinBookClub(code)
    }

    /**
     * Clears the stored lookup state.
     */
    fun clearLookupState() {
        lastLookedUpCode = null
    }

    /**
     * Generates an invite link for an existing book club.
     *
     * @param clubCode The club code
     * @param shelfName The name of the shelf (optional, defaults to "Book Club")
     * @return The invite link URL
     */
    fun generateInviteLink(clubCode: String, shelfName: String = "Book Club"): String {
        return bookClubUseCases.generateInviteLink(clubCode, shelfName)
    }

    /**
     * Syncs books from a book club to the local shelf.
     * Fetches new books added by other members and removes deleted ones.
     *
     * @param clubCode The book club code
     * @param localShelfId The local shelf ID to sync to
     * @return Result with SyncResult on success, or DataError.Sync on failure
     */
    suspend fun syncBooksFromClub(
        clubCode: String,
        localShelfId: String
    ): Result<SyncResult, DataError.Sync> {
        return bookClubUseCases.syncBookClub(clubCode, localShelfId)
    }

    /**
     * Leaves a book club.
     * Removes the user from the club and deletes the local shelf.
     *
     * @param shelfId The local shelf ID of the book club
     * @return Result with Unit on success, or DataError.Sync on failure
     */
    suspend fun leaveBookClub(shelfId: String): Result<Unit, DataError.Sync> {
        return bookClubUseCases.leaveBookClub(shelfId)
    }

    /**
     * Validates all local book club memberships against Firestore.
     * Cleans up any clubs that have been deleted by their creators.
     *
     * @return List of deleted club names (for user notification), or empty if all valid
     */
    suspend fun validateMemberships(): List<String> {
        return when (val result = bookClubUseCases.validateMemberships()) {
            is Result.Success -> result.data
            is Result.Error -> emptyList() // Silently fail - validation is best-effort
        }
    }
}
