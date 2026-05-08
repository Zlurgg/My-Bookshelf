package uk.co.zlurgg.mybookshelf.welcome.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences

class MarkWelcomeShownUseCaseImpl(
    private val welcomePreferences: WelcomePreferences,
) : MarkWelcomeShownUseCase {

    override suspend operator fun invoke() {
        welcomePreferences.setWelcomeShown()
        Timber.tag(TAG).d("Welcome marked as shown on device")
    }

    companion object {
        private const val TAG = "MarkWelcomeShown"
    }
}
