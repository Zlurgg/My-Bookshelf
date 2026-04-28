package uk.co.zlurgg.mybookshelf.sharing.data.service

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.sharing.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.util.Base64Encoder

/**
 * URL-encoded implementation of ShareTokenService that embeds shelf data directly in share links.
 *
 * This implementation replaces the previous token-based storage system with self-contained URLs.
 * Benefits:
 * - No storage required (no memory, database, or server)
 * - Links never expire
 * - Works even after sender closes the app
 * - Simpler architecture
 *
 * The "token" is actually the GZip-compressed and Base64-encoded shelf data itself.
 */
class UrlEncodedShareTokenService : ShareTokenService {

    companion object {
        private const val TAG = "UrlEncodedShareToken"
    }

    /**
     * Generates a "token" by encoding the shelf JSON data with GZip compression and Base64.
     *
     * @param shelfJsonData The JSON representation of the bookshelf to share
     * @return Success with encoded data string, or Error if encoding fails
     */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun generateToken(shelfJsonData: String): Result<String, DataError.Local> {
        return try {
            val encoded = Base64Encoder.encode(shelfJsonData)
            Result.Success(encoded)
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                ?: DataError.Local.UNKNOWN
            Timber.tag(TAG).e(e, "Token generation failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }

    /**
     * Retrieves shelf data by decoding the "token" (which is actually the encoded data itself).
     *
     * @param token The Base64-encoded GZip-compressed shelf data
     * @return Success with decoded JSON string, or Error if decoding fails
     */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun getShelfDataByToken(token: String): Result<String, DataError.Local> {
        return try {
            val decoded = Base64Encoder.decode(token)
            Result.Success(decoded)
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                ?: DataError.Local.UNKNOWN
            Timber.tag(TAG).e(e, "Token decoding failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }

    /**
     * Cleanup is a no-op since there's no storage to clean up.
     * Kept for interface compatibility.
     *
     * @return Always returns Success
     */
    override suspend fun cleanupExpiredTokens(): Result<Unit, DataError.Local> {
        // No-op: self-contained URLs don't require cleanup
        return Result.Success(Unit)
    }
}
