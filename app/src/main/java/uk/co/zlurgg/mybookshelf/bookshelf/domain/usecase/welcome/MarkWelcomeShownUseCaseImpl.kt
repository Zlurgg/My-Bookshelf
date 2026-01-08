package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences
import uk.co.zlurgg.mybookshelf.sync.domain.repository.UserPreferencesRepository

class MarkWelcomeShownUseCaseImpl(
    private val welcomePreferences: WelcomePreferences,
    private val currentUserProvider: CurrentUserProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) : MarkWelcomeShownUseCase {

    companion object {
        private const val TAG = "MarkWelcomeShown"
    }

    override suspend fun execute() {
        val userId = currentUserProvider.getCurrentUserId()

        // Write to local DataStore (immediate)
        welcomePreferences.setWelcomeShown(userId)
        Timber.tag(TAG).d("Welcome marked as shown locally for user: %s", userId ?: "guest")

        // Write to Firestore (best-effort for signed-in users)
        userId?.let { uid ->
            userPreferencesRepository.setWelcomeShown(uid, true)
            Timber.tag(TAG).d("Welcome marked as shown in cloud for user: %s", uid)
        }
    }
}