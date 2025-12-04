package uk.co.zlurgg.mybookshelf.sync.domain.service

/**
 * Domain interface for scheduling sync operations.
 *
 * This abstraction allows the domain layer to request sync scheduling
 * without depending on Android-specific WorkManager implementation.
 */
interface SyncSchedulerService {

    /**
     * Schedules periodic background sync.
     * Call this after successful sign-in.
     */
    fun schedulePeriodicSync()

    /**
     * Triggers an immediate sync operation.
     * Call this after sign-in, pull-to-refresh, or after making changes.
     */
    fun triggerImmediateSync()

    /**
     * Cancels all scheduled sync operations.
     * Call this on sign-out.
     */
    fun cancelAllSync()
}
