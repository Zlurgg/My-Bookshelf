package uk.co.zlurgg.mybookshelf.auth.domain.usecase

data class SignInUseCases(
    val signIn: SignInUseCase,
    val signOut: SignOutUseCase,
    val checkSignInStatus: CheckSignInStatusUseCase,
)
