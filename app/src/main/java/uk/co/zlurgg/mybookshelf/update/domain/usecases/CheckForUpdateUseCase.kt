package uk.co.zlurgg.mybookshelf.update.domain.usecases

import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateInfo

/**
 * Use case to check for available app updates.
 */
interface CheckForUpdateUseCase {
    suspend fun execute(forceCheck: Boolean = false): UpdateInfo?
}
