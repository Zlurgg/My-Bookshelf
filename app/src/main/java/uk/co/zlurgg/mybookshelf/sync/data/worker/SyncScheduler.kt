package uk.co.zlurgg.mybookshelf.sync.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling of sync work via WorkManager.
 *
 * Provides:
 * - Periodic background sync every 15 minutes
 * - Immediate one-time sync (e.g., on sign-in, pull-to-refresh)
 * - Sync cancellation
 * - Sync status observation
 */
class SyncScheduler(
    private val context: Context
) : SyncSchedulerService {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedules periodic sync to run every 15 minutes when network is available.
     * Uses KEEP policy to not replace existing work.
     */
    override fun schedulePeriodicSync() {
        Timber.tag(TAG).d("Scheduling periodic sync (every %d minutes)", SYNC_INTERVAL_MINUTES)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
            FLEX_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG_PERIODIC)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )

        Timber.tag(TAG).d("Periodic sync scheduled")
    }

    /**
     * Triggers an immediate one-time sync.
     * Useful for:
     * - After sign-in
     * - Pull-to-refresh
     * - When user makes changes
     */
    override fun triggerImmediateSync() {
        Timber.tag(TAG).d("Triggering immediate sync")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG_IMMEDIATE)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeSyncRequest
        )

        Timber.tag(TAG).d("Immediate sync enqueued")
    }

    /**
     * Cancels all scheduled sync work.
     * Call this on sign-out.
     */
    override fun cancelAllSync() {
        Timber.tag(TAG).d("Cancelling all sync work")
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME_PERIODIC)
        Timber.tag(TAG).d("All sync work cancelled")
    }

    /**
     * Cancels only the periodic sync.
     * The immediate sync can still run.
     */
    fun cancelPeriodicSync() {
        Timber.tag(TAG).d("Cancelling periodic sync")
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME_PERIODIC)
    }

    /**
     * Observes the sync work state.
     * Returns a Flow of whether sync is currently running.
     */
    fun observeSyncRunning(): Flow<Boolean> {
        return workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME)
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.RUNNING }
            }
    }

    /**
     * Checks if periodic sync is currently scheduled.
     */
    fun isPeriodicSyncScheduled(): Flow<Boolean> {
        return workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME_PERIODIC)
            .map { workInfos ->
                workInfos.any {
                    it.state == WorkInfo.State.ENQUEUED ||
                        it.state == WorkInfo.State.RUNNING
                }
            }
    }

    companion object {
        private const val TAG = "SyncScheduler"

        // Sync interval: 15 minutes (minimum for periodic work)
        private const val SYNC_INTERVAL_MINUTES = 15L

        // Flex interval: 5 minutes (work can run within last 5 minutes of interval)
        private const val FLEX_INTERVAL_MINUTES = 5L

        // Backoff delay for retries: 30 seconds initial, exponential
        private const val BACKOFF_DELAY_SECONDS = 30L

        // Tags for work identification
        private const val TAG_PERIODIC = "sync_periodic"
        private const val TAG_IMMEDIATE = "sync_immediate"
    }
}
