package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome

/**
 * UseCase for marking the welcome screen as shown.
 *
 * Persists to both local storage and cloud (for signed-in users).
 */
interface MarkWelcomeShownUseCase {
    /**
     * Marks the welcome screen as shown for the current user.
     *
     * Writes to local DataStore immediately, and to Firestore for signed-in users.
     */
    suspend operator fun invoke()
}
