package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider

/**
 * UseCase for retrieving the current signed-in user's ID.
 * Returns null if no user is signed in (guest mode).
 */
interface GetCurrentUserIdUseCase {
    fun execute(): String?
}

class GetCurrentUserIdUseCaseImpl(
    private val currentUserProvider: CurrentUserProvider
) : GetCurrentUserIdUseCase {
    override fun execute(): String? = currentUserProvider.getCurrentUserId()
}
