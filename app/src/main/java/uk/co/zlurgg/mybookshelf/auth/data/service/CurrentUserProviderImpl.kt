package uk.co.zlurgg.mybookshelf.auth.data.service

import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider

/**
 * Implementation that gets the current user ID from the AuthService.
 */
class CurrentUserProviderImpl(
    private val authService: AuthService,
) : CurrentUserProvider {
    override fun getCurrentUserId(): String? {
        return authService.getSignedInUser()?.userId
    }
}
