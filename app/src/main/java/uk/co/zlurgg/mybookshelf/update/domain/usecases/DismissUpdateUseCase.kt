package uk.co.zlurgg.mybookshelf.update.domain.usecases

/**
 * Use case to dismiss a specific update version.
 */
interface DismissUpdateUseCase {
    suspend operator fun invoke(version: String)
}
