package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.ClearUserDataUseCase
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.repository.BookClubRemoteDataSource
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncState
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class DeleteAccountUseCaseImpl(
    private val authService: AuthService,
    private val authStateRepository: AuthStateRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val syncScheduler: SyncSchedulerService,
    private val syncRepository: SyncRepository,
    private val clearUserData: ClearUserDataUseCase,
    private val clubOperations: ClubOperations,
    private val bookClubRemoteDataSource: BookClubRemoteDataSource,
) : DeleteAccountUseCase {

    @Suppress("TooGenericExceptionCaught", "ReturnCount") // Multi-step cascade with early returns on failure
    override suspend operator fun invoke(): Result<Unit, DataError> {
        Timber.tag(TAG).d("=== DELETE ACCOUNT START ===")

        // 1. Verify user is signed in
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            Timber.tag(TAG).e("No signed-in user")
            return Result.Error(DataError.Local.AUTH_FAILED)
        }

        // 2. Wait for active sync to complete, then cancel sync workers
        Timber.tag(TAG).d("Waiting for sync to complete...")
        try {
            waitForSyncIdle(userId)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Sync wait failed, proceeding with cancellation")
        }
        syncScheduler.cancelAllSync()

        // 3. Clean up book clubs
        Timber.tag(TAG).d("Cleaning up book clubs...")
        val clubCleanupResult = cleanUpBookClubs(userId)
        if (clubCleanupResult is Result.Error) {
            Timber.tag(TAG).e("Club cleanup failed: %s", clubCleanupResult.error)
            return clubCleanupResult
        }

        // 4. Delete user's Firestore data (must fully succeed before Auth deletion)
        Timber.tag(TAG).d("Deleting remote user data...")
        val remoteResult = syncRepository.deleteAllRemoteData(userId)
        if (remoteResult is Result.Error) {
            Timber.tag(TAG).e("Remote data deletion failed: %s", remoteResult.error)
            return remoteResult
        }

        // 5. Delete Firebase Auth account (point of no return)
        Timber.tag(TAG).d("Deleting Firebase Auth account...")
        val authResult = authService.deleteAccount()
        if (authResult is Result.Error) {
            Timber.tag(TAG).e("Auth deletion failed: %s", authResult.error)
            return authResult
        }

        // 6. Clear local data + sync metadata
        cleanUpLocalData(userId)

        Timber.tag(TAG).d("=== DELETE ACCOUNT COMPLETE ===")
        return Result.Success(Unit)
    }

    /**
     * Retries account deletion after re-authentication.
     * Must only be called after [invoke] returned [DataError.Local.REQUIRES_RECENT_LOGIN],
     * meaning remote data is already deleted.
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount") // Multi-step cascade with early returns on failure
    override suspend fun retryAfterReAuth(idToken: String): Result<Unit, DataError> {
        Timber.tag(TAG).d("=== RETRY AFTER RE-AUTH START ===")

        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            Timber.tag(TAG).e("No signed-in user for re-auth retry")
            return Result.Error(DataError.Local.AUTH_FAILED)
        }

        // Precondition guard: if remote data still exists, fall back to full invoke
        if (syncRepository.hasRemoteData(userId)) {
            Timber.tag(TAG).w("Remote data still exists, falling back to full deletion")
            return invoke()
        }

        // Re-authenticate
        Timber.tag(TAG).d("Re-authenticating...")
        val reAuthResult = authService.reauthenticate(idToken)
        if (reAuthResult is Result.Error) {
            Timber.tag(TAG).e("Re-authentication failed: %s", reAuthResult.error)
            return reAuthResult
        }

        // Delete Firebase Auth account
        Timber.tag(TAG).d("Deleting Firebase Auth account after re-auth...")
        val authResult = authService.deleteAccount()
        if (authResult is Result.Error) {
            Timber.tag(TAG).e("Auth deletion failed after re-auth: %s", authResult.error)
            return authResult
        }

        // Clear local data
        cleanUpLocalData(userId)

        Timber.tag(TAG).d("=== RETRY AFTER RE-AUTH COMPLETE ===")
        return Result.Success(Unit)
    }

    private suspend fun waitForSyncIdle(userId: String) {
        withTimeoutOrNull(SYNC_WAIT_TIMEOUT_MS) {
            syncRepository.observeSyncState(userId).first { state ->
                state !is SyncState.Syncing
            }
        } ?: Timber.tag(TAG).w("Sync wait timed out after %d ms", SYNC_WAIT_TIMEOUT_MS)
    }

    @Suppress("ReturnCount") // Early returns per club operation failure
    private suspend fun cleanUpBookClubs(userId: String): Result<Unit, DataError> {
        // Delete clubs created by this user
        when (val createdResult = bookClubRemoteDataSource.getClubsCreatedByUser(userId)) {
            is Result.Success -> {
                for (clubCode in createdResult.data) {
                    Timber.tag(TAG).d("Deleting club created by user: %s", clubCode)
                    val deleteResult = clubOperations.deleteBookClub(clubCode)
                    if (deleteResult is Result.Error) {
                        Timber.tag(TAG).e("Failed to delete club %s: %s", clubCode, deleteResult.error)
                        return deleteResult
                    }
                }
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get clubs created by user: %s", createdResult.error)
                return createdResult
            }
        }

        // Remove user from clubs they are a member of
        when (val memberResult = bookClubRemoteDataSource.getClubMembershipsForUser(userId)) {
            is Result.Success -> {
                for (clubCode in memberResult.data) {
                    Timber.tag(TAG).d("Removing user from club: %s", clubCode)
                    val removeResult = bookClubRemoteDataSource.removeUserFromClub(clubCode, userId)
                    if (removeResult is Result.Error) {
                        Timber.tag(TAG).e("Failed to remove from club %s: %s", clubCode, removeResult.error)
                        return removeResult
                    }
                }
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get club memberships: %s", memberResult.error)
                return memberResult
            }
        }

        // Clear local club memberships
        val clearResult = clubOperations.clearAllMemberships()
        if (clearResult is Result.Error) {
            Timber.tag(TAG).e("Failed to clear local memberships: %s", clearResult.error)
            return clearResult
        }

        return Result.Success(Unit)
    }

    private suspend fun cleanUpLocalData(userId: String) {
        Timber.tag(TAG).d("Clearing local data for: %s", userId)
        when (val clearResult = clearUserData(userId)) {
            is Result.Success -> Timber.tag(TAG).d("Cleared %d local items", clearResult.data)
            is Result.Error -> Timber.tag(TAG).w("Failed to clear local data: %s", clearResult.error)
        }

        syncRepository.clearSyncData(userId)

        when (val stateResult = authStateRepository.setSignedInState(false)) {
            is Result.Success -> { /* State saved successfully */ }
            is Result.Error -> Timber.tag(TAG).w("Failed to save auth state: %s", stateResult.error)
        }
    }

    companion object {
        private const val TAG = "DeleteAccount"
        private const val SYNC_WAIT_TIMEOUT_MS = 10_000L
    }
}
