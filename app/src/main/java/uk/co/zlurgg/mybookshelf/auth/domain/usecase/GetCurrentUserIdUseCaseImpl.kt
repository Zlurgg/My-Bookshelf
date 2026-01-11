package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider

class GetCurrentUserIdUseCaseImpl(
    private val currentUserProvider: CurrentUserProvider
) : GetCurrentUserIdUseCase {
    override operator fun invoke(): String? = currentUserProvider.getCurrentUserId()
}
