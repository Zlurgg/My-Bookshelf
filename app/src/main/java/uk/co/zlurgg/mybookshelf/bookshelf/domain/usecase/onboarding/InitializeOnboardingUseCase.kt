package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.onboarding

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for initializing onboarding experience on first app launch.
 *
 * Creates a "Tutorial Bookshelf" with random style on first launch only.
 * Contains all business logic for onboarding initialization.
 */
interface InitializeOnboardingUseCase {
    suspend fun execute(): Result<Unit, DataError>
}