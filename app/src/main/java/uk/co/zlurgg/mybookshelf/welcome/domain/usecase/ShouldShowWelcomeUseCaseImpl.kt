package uk.co.zlurgg.mybookshelf.welcome.domain.usecase

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences

class ShouldShowWelcomeUseCaseImpl(
    private val welcomePreferences: WelcomePreferences,
) : ShouldShowWelcomeUseCase {

    override suspend operator fun invoke(): Boolean {
        return !welcomePreferences.hasShownWelcome().first()
    }
}
