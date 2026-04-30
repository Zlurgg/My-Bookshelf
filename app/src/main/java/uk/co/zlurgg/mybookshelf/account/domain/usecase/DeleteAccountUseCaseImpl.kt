package uk.co.zlurgg.mybookshelf.account.domain.usecase

import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class DeleteAccountUseCaseImpl(
    private val currentUserProvider: CurrentUserProvider,
    private val syncScheduler: SyncSchedulerService,
    private val syncRepository: SyncRepository,
    private val clubOperations: ClubOperations,
    private val authService: AuthService,
) : DeleteAccountUseCase {

    @Suppress("ReturnCount")
    override suspend operator fun invoke(): Result<Unit, DataError> {
        val userId = currentUserProvider.getCurrentUserId()
            ?: return Result.Error(DataError.Local.AUTH_FAILED)

        syncScheduler.cancelAllSync()

        // Delete clubs created by this user
        val createdResult = clubOperations.getClubsCreatedByUser(userId)
        if (createdResult is Result.Error) return createdResult
        val createdCodes = (createdResult as Result.Success).data

        for (code in createdCodes) {
            val deleteResult = clubOperations.deleteBookClub(code)
            if (deleteResult is Result.Error) return deleteResult
        }

        // Remove user from clubs they're a member of (excluding ones they created)
        val memberResult = clubOperations.getClubMembershipsForUser(userId)
        if (memberResult is Result.Error) return memberResult
        val memberCodes = (memberResult as Result.Success).data - createdCodes.toSet()

        for (code in memberCodes) {
            val removeResult = clubOperations.removeUserFromClub(code, userId)
            if (removeResult is Result.Error) return removeResult
        }

        // Delete Firestore data (books, shelves, preferences)
        val deleteDataResult = syncRepository.deleteAllRemoteData(userId)
        if (deleteDataResult is Result.Error) return deleteDataResult

        // Delete Firebase Auth account
        return authService.deleteAccount()
    }

    override suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError> {
        val reAuthResult = authService.reauthenticate(idToken)
        if (reAuthResult is Result.Error) return reAuthResult
        return authService.deleteAccount()
    }
}
