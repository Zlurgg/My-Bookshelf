package uk.co.zlurgg.mybookshelf.update.domain.usecases

import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateInfo

/**
 * Use case to download an APK update.
 */
interface DownloadUpdateUseCase {
    fun execute(updateInfo: UpdateInfo): Long?
}
