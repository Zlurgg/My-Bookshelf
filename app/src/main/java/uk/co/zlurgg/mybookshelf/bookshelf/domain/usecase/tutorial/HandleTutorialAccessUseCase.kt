package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase to handle tutorial access from the help (?) button.
 * Orchestrates checking for existing tutorial, creating if needed,
 * and determining navigation behavior.
 */
interface HandleTutorialAccessUseCase {
    /**
     * Handle tutorial access request.
     *
     * If tutorial doesn't exist: Creates shelf and book silently, returns DoNotNavigate
     * If tutorial exists: Returns NavigateToBook with shelf and book IDs
     *
     * @return Result containing TutorialAccessResult with navigation instructions
     */
    suspend fun execute(): Result<TutorialAccessResult, DataError.Local>
}
