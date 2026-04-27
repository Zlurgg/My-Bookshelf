package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import kotlinx.coroutines.flow.first
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Implementation of ValidateBookClubMembershipsUseCase.
 *
 * Validates all local book club memberships against Firestore. If a club was
 * deleted by its creator, the shelf is converted to a personal shelf so the
 * member doesn't lose access to their books.
 */
class ValidateBookClubMembershipsUseCaseImpl(
    private val authService: AuthService,
    private val bookClubRepository: BookClubRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val syncSchedulerService: SyncSchedulerService
) : ValidateBookClubMembershipsUseCase {

    override suspend fun invoke(): Result<List<String>, DataError.Sync> {
        // Only validate if user is signed in
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).d("User not signed in, skipping validation")
            return Result.Success(emptyList())
        }

        // Get all local book club memberships
        val memberships = bookClubRepository.observeMyBookClubs().first()
        if (memberships.isEmpty()) {
            Timber.tag(TAG).d("No local book club memberships to validate")
            return Result.Success(emptyList())
        }

        Timber.tag(TAG).d("Validating %d book club memberships", memberships.size)

        val convertedShelfNames = mutableListOf<String>()

        for (membership in memberships) {
            val clubCode = membership.clubCode
            val localShelfId = membership.localShelfId

            // Get the local shelf to get its name for the notification
            val localShelf = when (val getResult = bookcaseRepository.getShelfById(localShelfId)) {
                is Result.Success -> getResult.data
                is Result.Error -> null
            }
            val shelfName = localShelf?.name ?: "Unknown Club"

            // Check if club still exists in Firestore
            when (val clubResult = bookClubRepository.getBookClub(clubCode)) {
                is Result.Success -> {
                    if (clubResult.data == null) {
                        // Club was deleted - convert to personal shelf (keeps books)
                        Timber.tag(
                            TAG
                        ).d("Club '%s' (%s) was deleted, converting to personal shelf", shelfName, clubCode)
                        bookClubRepository.convertClubToPersonalShelf(clubCode)
                        convertedShelfNames.add(shelfName)
                    }
                    // Club exists, nothing to do
                }
                is Result.Error -> {
                    // Network error or other issue - don't convert, just log
                    Timber.tag(TAG).w("Failed to validate club %s: %s", clubCode, clubResult.error)
                }
            }
        }

        if (convertedShelfNames.isNotEmpty()) {
            Timber.tag(
                TAG
            ).d("Converted %d deleted clubs to personal shelves: %s", convertedShelfNames.size, convertedShelfNames)
            // Trigger sync to upload converted shelves to user's personal Firestore
            Timber.tag(
                SyncConstants.TAG_SYNC_TRIGGER
            ).d("Sync triggered by: ValidateBookClubMemberships (converted %d clubs)", convertedShelfNames.size)
            syncSchedulerService.triggerImmediateSync()
        }

        return Result.Success(convertedShelfNames)
    }

    companion object {
        private const val TAG = "ValidateBookClubs"
    }
}
