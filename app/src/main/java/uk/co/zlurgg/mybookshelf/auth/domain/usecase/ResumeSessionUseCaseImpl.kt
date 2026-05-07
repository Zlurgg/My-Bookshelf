package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ResumeSessionUseCaseImpl(
    private val restoreBookClubMemberships: RestoreBookClubMembershipsUseCase,
) : ResumeSessionUseCase {

    override suspend operator fun invoke() {
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
    }

    companion object {
        private const val TAG = "ResumeSession"
    }
}
