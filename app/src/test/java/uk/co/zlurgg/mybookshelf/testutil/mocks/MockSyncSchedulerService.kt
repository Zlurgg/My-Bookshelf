package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Mock implementation of SyncSchedulerService for testing.
 * Tracks method calls for verification in tests.
 */
class MockSyncSchedulerService : SyncSchedulerService {
    var schedulePeriodicSyncCallCount = 0
        private set

    var triggerImmediateSyncCallCount = 0
        private set

    var cancelAllSyncCallCount = 0
        private set

    override fun schedulePeriodicSync() {
        schedulePeriodicSyncCallCount++
    }

    override fun triggerImmediateSync() {
        triggerImmediateSyncCallCount++
    }

    override fun cancelAllSync() {
        cancelAllSyncCallCount++
    }

    fun reset() {
        schedulePeriodicSyncCallCount = 0
        triggerImmediateSyncCallCount = 0
        cancelAllSyncCallCount = 0
    }
}
