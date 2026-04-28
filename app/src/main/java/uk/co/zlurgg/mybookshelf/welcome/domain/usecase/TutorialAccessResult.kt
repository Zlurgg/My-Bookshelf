package uk.co.zlurgg.mybookshelf.welcome.domain.usecase

/**
 * Result type for tutorial access operations.
 * Indicates whether navigation should occur after accessing the tutorial.
 */
sealed interface TutorialAccessResult {
    /**
     * Tutorial was created silently. No navigation should occur.
     */
    data object DoNotNavigate : TutorialAccessResult

    /**
     * Tutorial exists. Navigate to the tutorial book detail screen.
     * @param shelfId The ID of the tutorial shelf
     * @param bookId The ID of the tutorial book
     */
    data class NavigateToBook(val shelfId: String, val bookId: String) : TutorialAccessResult
}
