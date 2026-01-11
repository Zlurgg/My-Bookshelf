package uk.co.zlurgg.mybookshelf.auth.domain.usecase

interface CheckSignInStatusUseCase {
    suspend operator fun invoke(): Boolean
}
