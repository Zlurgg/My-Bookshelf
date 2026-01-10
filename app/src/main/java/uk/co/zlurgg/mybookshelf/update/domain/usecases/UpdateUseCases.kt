package uk.co.zlurgg.mybookshelf.update.domain.usecases

/**
 * Facade aggregating all update-related use cases.
 * Used by ViewModels that need update checking functionality.
 */
data class UpdateUseCases(
    val checkForUpdate: CheckForUpdateUseCase,
    val downloadUpdate: DownloadUpdateUseCase,
    val dismissUpdate: DismissUpdateUseCase,
    val getCurrentVersionInfo: GetCurrentVersionInfoUseCase
)
