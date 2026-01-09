package uk.co.zlurgg.mybookshelf.update.domain.usecases

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateInfo
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdateRepository

/**
 * Use case to get release info for the current installed version.
 */
interface GetCurrentVersionInfoUseCase {
    suspend operator fun invoke(): UpdateInfo?
}

/**
 * Implementation that fetches changelog and version details from GitHub
 * for display in "Up to Date" dialog.
 */
class GetCurrentVersionInfoUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val currentVersion: String
) : GetCurrentVersionInfoUseCase {
    override suspend operator fun invoke(): UpdateInfo? {
        Timber.d("Fetching release info for current version: $currentVersion")
        return updateRepository.getReleaseByVersion(currentVersion).getOrNull()
    }
}
