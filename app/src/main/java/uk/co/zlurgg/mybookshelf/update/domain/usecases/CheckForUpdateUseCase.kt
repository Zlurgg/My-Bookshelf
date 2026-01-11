package uk.co.zlurgg.mybookshelf.update.domain.usecases

import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateInfo

/**
 * Use case to check for available app updates.
 */
interface CheckForUpdateUseCase {
    suspend operator fun invoke(forceCheck: Boolean = false): UpdateInfo?
}
