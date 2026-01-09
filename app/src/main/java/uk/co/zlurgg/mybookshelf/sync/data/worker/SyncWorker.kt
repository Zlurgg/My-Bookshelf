package uk.co.zlurgg.mybookshelf.sync.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository
import uk.co.zlurgg.mybookshelf.core.domain.result.Result as SyncResult

/**
 * WorkManager worker that performs background sync operations.
 *
 * This worker:
 * - Checks if user is signed in before syncing
 * - Performs full sync (push + pull)
 * - Returns retry on transient failures
 * - Returns success on permanent failures (to prevent infinite retries)
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val authService: AuthService by inject()
    private val syncRepository: SyncRepository by inject()

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("=== SYNC WORKER START (attempt %d) ===", runAttemptCount)

        // Check if user is signed in
        val user = authService.getSignedInUser()
        if (user == null) {
            Timber.tag(TAG).d("No signed-in user, skipping sync")
            return Result.success()
        }

        val userId = user.userId
        Timber.tag(TAG).d("Starting sync for user: %s", userId)

        return when (val syncResult = syncRepository.performSync(userId)) {
            is SyncResult.Success -> {
                val data = syncResult.data
                Timber.tag(TAG).d(
                    "=== SYNC WORKER COMPLETE === Pushed: %d, Pulled: %d, Conflicts: %d",
                    data.pushedCount,
                    data.pulledCount,
                    data.conflictCount
                )
                Result.success()
            }

            is SyncResult.Error -> {
                handleSyncError(syncResult.error)
            }
        }
    }

    private fun handleSyncError(error: DataError.Sync): Result {
        Timber.tag(TAG).w("Sync failed with error: %s", error)

        return when (error) {
            // Transient errors - retry with backoff
            DataError.Sync.NETWORK_ERROR,
            DataError.Sync.SYNC_IN_PROGRESS -> {
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    Timber.tag(TAG).d("Will retry sync (attempt %d of %d)", runAttemptCount + 1, MAX_RETRY_ATTEMPTS)
                    Result.retry()
                } else {
                    Timber.tag(TAG).w("Max retries exceeded, giving up")
                    Result.failure()
                }
            }

            // Permanent errors - don't retry
            DataError.Sync.NOT_SIGNED_IN,
            DataError.Sync.PERMISSION_DENIED,
            DataError.Sync.QUOTA_EXCEEDED,
            // Book Club errors - not applicable to sync worker, but need to handle
            DataError.Sync.GENERATION_FAILED,
            DataError.Sync.CLUB_NOT_FOUND,
            DataError.Sync.ALREADY_MEMBER,
            DataError.Sync.NOT_MEMBER,
            DataError.Sync.CREATOR_CANNOT_LEAVE,
            DataError.Sync.MAX_BOOK_CLUBS_REACHED,
            DataError.Sync.INVALID_INPUT -> {
                Timber.tag(TAG).e("Permanent sync error, not retrying: %s", error)
                Result.failure()
            }

            // Other errors - treat as transient but limit retries
            DataError.Sync.CONFLICT_UNRESOLVED,
            DataError.Sync.MIGRATION_FAILED,
            DataError.Sync.DOCUMENT_NOT_FOUND,
            DataError.Sync.UNKNOWN -> {
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_work"
        const val WORK_NAME_PERIODIC = "sync_work_periodic"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
