package uk.co.zlurgg.mybookshelf.auth.domain.usecase

/**
 * UseCase for retrieving the current signed-in user's ID.
 * Returns null if no user is signed in (guest mode).
 */
interface GetCurrentUserIdUseCase {
    operator fun invoke(): String?
}
