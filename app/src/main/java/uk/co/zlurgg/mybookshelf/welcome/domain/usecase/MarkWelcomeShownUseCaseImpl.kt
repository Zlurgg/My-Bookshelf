package uk.co.zlurgg.mybookshelf.welcome.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences

class MarkWelcomeShownUseCaseImpl(
    private val welcomePreferences: WelcomePreferences,
    private val currentUserProvider: CurrentUserProvider,
) : MarkWelcomeShownUseCase {

    override suspend operator fun invoke() {
        val userId = currentUserProvider.getCurrentUserId()
        welcomePreferences.setWelcomeShown(userId)
        Timber.tag(TAG).d("Welcome marked as shown locally for user: %s", userId ?: "guest")
    }

    companion object {
        private const val TAG = "MarkWelcomeShown"
    }
}
