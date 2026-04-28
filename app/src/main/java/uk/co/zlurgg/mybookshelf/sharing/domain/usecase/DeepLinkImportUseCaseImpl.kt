package uk.co.zlurgg.mybookshelf.sharing.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.sharing.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.flatMap
import uk.co.zlurgg.mybookshelf.core.domain.result.map

/**
 * Implementation of DeepLinkImportUseCase that orchestrates token validation and bookshelf import.
 * Refactored to use dedicated use cases following Clean Architecture principles.
 */
class DeepLinkImportUseCaseImpl(
    private val shareTokenService: ShareTokenService,
    private val checkImportConflictUseCase: CheckImportConflictUseCase,
    private val importBookshelfUseCase: ImportBookshelfUseCase
) : DeepLinkImportUseCase {

    companion object {
        private const val TAG = "DeepLinkImport"
    }

    override suspend fun importBookshelfFromToken(token: String): Result<ImportResult, DataError.Local> {
        val tokenPreview = token.take(10) + "..."
        Timber.tag(TAG).d("Processing deep link import with token: %s", tokenPreview)

        val result = shareTokenService.getShelfDataByToken(token)
            .flatMap { jsonData ->
                Timber.tag(TAG).d("Successfully fetched shelf data, checking for conflicts...")
                checkImportConflictUseCase(jsonData)
                    .flatMap { conflictingName ->
                        if (conflictingName != null) {
                            // Name conflict exists, return conflict info
                            Timber.tag(TAG).w("Name conflict detected: '%s'", conflictingName)
                            Result.Success(ImportResult.NameConflict(conflictingName, jsonData))
                        } else {
                            // No conflict, proceed with import
                            Timber.tag(TAG).d("No conflicts found, proceeding with import...")
                            importBookshelfUseCase(jsonData)
                                .map {
                                    Timber.tag(TAG).d("Import successful")
                                    ImportResult.Success
                                }
                        }
                    }
            }

        return when (result) {
            is Result.Success -> result
            is Result.Error -> {
                Timber.tag(TAG).e("Deep link import failed: %s", result.error)
                result
            }
        }
    }

    override suspend fun importBookshelfWithCustomName(
        jsonData: String,
        customName: String
    ): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("Importing bookshelf with custom name: '%s'", customName)

        // First check if the custom name also conflicts
        val result = checkImportConflictUseCase.checkShelfName(customName)
            .flatMap { conflictingName ->
                if (conflictingName != null) {
                    // Custom name still conflicts - return error
                    Timber.tag(TAG).w("Custom name '%s' still conflicts with existing shelf", customName)
                    Result.Error(DataError.Local.NAME_CONFLICT)
                } else {
                    // No conflict - proceed with import
                    Timber.tag(TAG).d("Custom name '%s' is available, proceeding with import...", customName)
                    importBookshelfUseCase(jsonData, customName)
                }
            }

        return when (result) {
            is Result.Success -> {
                Timber.tag(TAG).d("Successfully imported bookshelf with custom name: '%s'", customName)
                result
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to import with custom name '%s': %s", customName, result.error)
                result
            }
        }
    }
}
