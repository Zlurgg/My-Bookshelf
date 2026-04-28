package uk.co.zlurgg.mybookshelf.welcome.domain.usecase

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences

class ShouldShowWelcomeUseCaseImpl(
    private val welcomePreferences: WelcomePreferences,
    private val currentUserProvider: CurrentUserProvider
) : ShouldShowWelcomeUseCase {

    override suspend operator fun invoke(): Boolean {
        val userId = currentUserProvider.getCurrentUserId()
        val hasShown = welcomePreferences.hasShownWelcome(userId).first()
        return !hasShown
    }
}
