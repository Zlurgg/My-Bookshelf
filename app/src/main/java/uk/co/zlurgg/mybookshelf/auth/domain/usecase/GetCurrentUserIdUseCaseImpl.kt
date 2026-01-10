package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider

class GetCurrentUserIdUseCaseImpl(
    private val currentUserProvider: CurrentUserProvider
) : GetCurrentUserIdUseCase {
    override fun execute(): String? = currentUserProvider.getCurrentUserId()
}
