package uk.co.zlurgg.mybookshelf.update.domain.usecases

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdatePreferencesRepository

/**
 * Implementation that stores the version so the user won't be prompted again.
 */
class DismissUpdateUseCaseImpl(
    private val updatePreferencesRepository: UpdatePreferencesRepository
) : DismissUpdateUseCase {
    override suspend fun execute(version: String) {
        Timber.i("User dismissed update version: $version")
        when (val result = updatePreferencesRepository.setDismissedVersion(version)) {
            is Result.Success -> { /* Successfully saved */ }
            is Result.Error -> {
                Timber.w("Failed to save dismissed version: %s", result.error)
                // Non-critical - continue anyway
            }
        }
    }
}
