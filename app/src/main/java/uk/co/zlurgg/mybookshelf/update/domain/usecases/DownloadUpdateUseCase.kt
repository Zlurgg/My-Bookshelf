package uk.co.zlurgg.mybookshelf.update.domain.usecases

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateConfig
import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateInfo
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdateRepository

/**
 * Use case to download an APK update.
 */
interface DownloadUpdateUseCase {
    operator fun invoke(updateInfo: UpdateInfo): Long?
}

/**
 * Implementation that downloads APK via repository.
 * Returns the download ID for tracking progress.
 */
class DownloadUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val config: UpdateConfig,
) : DownloadUpdateUseCase {
    override operator fun invoke(updateInfo: UpdateInfo): Long? {
        val downloadUrl =
            updateInfo.apkDownloadUrl ?: run {
                Timber.e("No APK download URL available")
                return null
            }

        val fileName = "${config.appName}-${updateInfo.versionName}.apk"
        Timber.i("Starting download: $fileName")

        return updateRepository.downloadApk(downloadUrl, fileName)
    }
}
