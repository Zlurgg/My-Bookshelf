package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.SyncUserPreferencesUseCase

class ResumeSessionUseCaseImpl(
    private val syncUserPreferences: SyncUserPreferencesUseCase,
    private val restoreBookClubMemberships: RestoreBookClubMembershipsUseCase,
    private val syncScheduler: SyncSchedulerService,
) : ResumeSessionUseCase {

    override suspend operator fun invoke() {
        syncUserPreferences()

        when (val result = restoreBookClubMemberships()) {
            is Result.Success -> {
                Timber.tag(TAG).d(
                    "Book club memberships restored: %d restored, %d failed",
                    result.data.restoredCount,
                    result.data.failedCount
                )
            }
            is Result.Error -> {
                Timber.tag(TAG).w("Failed to restore book club memberships: %s", result.error)
            }
        }

        // Defensive: re-establish periodic sync in case WorkManager cleared it
        // (e.g., app data cleared, OS killed the worker). Normally a no-op
        // since schedulePeriodicSync uses ExistingPeriodicWorkPolicy.KEEP.
        syncScheduler.schedulePeriodicSync()
        syncScheduler.triggerImmediateSync()
    }

    companion object {
        private const val TAG = "ResumeSession"
    }
}
