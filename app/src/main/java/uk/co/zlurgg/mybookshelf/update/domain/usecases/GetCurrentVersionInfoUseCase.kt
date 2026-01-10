package uk.co.zlurgg.mybookshelf.update.domain.usecases

import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateInfo

/**
 * Use case to get release info for the current installed version.
 */
interface GetCurrentVersionInfoUseCase {
    suspend fun execute(): UpdateInfo?
}
