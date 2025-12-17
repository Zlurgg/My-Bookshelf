package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers

import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.BookClubUseCases
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
        return when (val createResult = bookClubUseCases.createBookClub.execute(shelfId)) {
            is Result.Success -> {
                val clubCode = createResult.data
                val inviteLink = bookClubUseCases.generateInviteLink.execute(clubCode, shelfName)
                Result.Success(BookClubCreationResult(clubCode, inviteLink))
            }
            is Result.Error -> Result.Error(createResult.error)
        }
    }
}
