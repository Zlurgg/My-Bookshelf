package uk.co.zlurgg.mybookshelf.auth.domain.usecase

/**
 * Facade aggregating all authentication-related use cases.
 * Used by ViewModels that need auth functionality.
 */
data class AuthUseCases(
    val signIn: SignInUseCase,
    val signOut: SignOutUseCase,
    val checkSignInStatus: CheckSignInStatusUseCase,
    val getCurrentUserId: GetCurrentUserIdUseCase,
    val getSignedInUser: GetSignedInUserUseCase,
)
