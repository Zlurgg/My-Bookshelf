package uk.co.zlurgg.mybookshelf.auth.domain.usecase

interface CheckSignInStatusUseCase {
    suspend fun execute(): Boolean
}
