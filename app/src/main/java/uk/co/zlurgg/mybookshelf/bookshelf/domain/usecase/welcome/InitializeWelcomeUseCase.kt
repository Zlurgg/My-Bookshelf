package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for initializing welcome experience on first app launch.
 *
 * Creates a "Tutorial Bookshelf" with random style on first launch only.
 * Contains all business logic for welcome initialization.
 */
interface InitializeWelcomeUseCase {
    suspend operator fun invoke(): Result<Unit, DataError>
}
