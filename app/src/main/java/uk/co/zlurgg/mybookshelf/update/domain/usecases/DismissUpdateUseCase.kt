package uk.co.zlurgg.mybookshelf.update.domain.usecases

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdatePreferencesRepository

/**
 * Use case to dismiss a specific update version.
 */
interface DismissUpdateUseCase {
    suspend operator fun invoke(version: String)
}

/**
 * Implementation that stores the version so the user won't be prompted again.
 */
class DismissUpdateUseCaseImpl(
    private val updatePreferencesRepository: UpdatePreferencesRepository,
) : DismissUpdateUseCase {
    override suspend operator fun invoke(version: String) {
        Timber.i("User dismissed update version: $version")
        updatePreferencesRepository.setDismissedVersion(version)
    }
}
