package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService

class GetSignedInUserUseCaseImpl(
    private val authService: AuthService,
) : GetSignedInUserUseCase {
    override operator fun invoke(): UserData? = authService.getSignedInUser()
}
