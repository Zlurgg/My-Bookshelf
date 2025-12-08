package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome

/**
 * UseCase for determining if the welcome screen should be shown.
 *
 * Checks per-user welcome state - each user sees welcome on their first sign-in,
 * even if another user has already seen it on the same device.
 */
interface ShouldShowWelcomeUseCase {
    /**
     * Checks if the welcome screen should be shown for the current user.
     *
     * @return true if welcome should be shown, false if already shown to this user
     */
    suspend fun execute(): Boolean
}
