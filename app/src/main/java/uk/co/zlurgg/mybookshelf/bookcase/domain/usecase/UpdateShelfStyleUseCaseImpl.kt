package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Implementation of UpdateShelfStyleUseCase.
 * Updates the shelf's style in the repository.
 * For book clubs, only the creator can change the style, and the change is synced to Firestore.
 */
class UpdateShelfStyleUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val clubOperations: ClubOperations,
    private val authService: AuthService,
    private val syncSchedulerService: SyncSchedulerService,
) : UpdateShelfStyleUseCase {

    companion object {
        private const val TAG = "UpdateShelfStyle"
    }

    override suspend operator fun invoke(shelfId: String, newStyle: ShelfStyle): Result<Unit, DataError.Local> {
        // Get the shelf to update
        val shelfToUpdate = when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data ?: return Result.Error(DataError.Local.NOT_FOUND)
            is Result.Error -> return getResult
        }

        // Permission check for book clubs - only creator can change style
        if (shelfToUpdate.isBookClub) {
            val currentUser = authService.getSignedInUser()
            if (currentUser == null || shelfToUpdate.clubCreatorId != currentUser.userId) {
                return Result.Error(DataError.Local.PERMISSION_DENIED)
            }

            // Update Firestore for book clubs
            val clubCode = shelfToUpdate.clubCode
            if (clubCode != null) {
                val updateResult = clubOperations.updateClubStyle(clubCode, newStyle.name)
                if (updateResult is Result.Error) {
                    Timber.tag(TAG).w("Failed to sync style to Firestore: %s", updateResult.error)
                    // Continue with local update even if Firestore fails
                }
            }
        }

        // Update the shelf with new style locally
        val updatedShelf = shelfToUpdate.copy(shelfStyle = newStyle)
        return when (val result = bookcaseRepository.updateShelf(updatedShelf)) {
            is Result.Success -> {
                if (!shelfToUpdate.isBookClub) {
                    Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: UpdateShelfStyle")
                    syncSchedulerService.triggerImmediateSync()
                }
                result
            }
            is Result.Error -> result
        }
    }
}
