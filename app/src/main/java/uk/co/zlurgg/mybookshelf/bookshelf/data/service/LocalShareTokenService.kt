package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfIdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import java.util.concurrent.ConcurrentHashMap

/**
 * Local implementation of ShareTokenService using in-memory storage.
 * In a production app, this would typically use a database or cloud storage.
 */
class LocalShareTokenService(
    private val tokenGenerator: BookshelfIdGenerator,
    private val timeProvider: TimeProvider
) : ShareTokenService {

    // In-memory storage for demo purposes
    // In production, this would be in a database with proper expiration
    private val tokenStorage = ConcurrentHashMap<String, TokenData>()

    private data class TokenData(
        val shelfData: String,
        val createdAt: Long,
        val expiresAt: Long = createdAt + EXPIRATION_TIME_MS
    )

    companion object {
        private const val EXPIRATION_TIME_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    override suspend fun generateToken(shelfJsonData: String): Result<String, DataError.Local> {
        return try {
            // Generate unique token
            val token = tokenGenerator.generateId()
            val now = timeProvider.currentTimeMillis()

            // Store the data with the token
            tokenStorage[token] = TokenData(
                shelfData = shelfJsonData,
                createdAt = now
            )

            Result.Success(token)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getShelfDataByToken(token: String): Result<String, DataError.Local> {
        return try {
            val tokenData = tokenStorage[token]
                ?: return Result.Error(DataError.Local.UNKNOWN)

            val now = timeProvider.currentTimeMillis()
            if (now > tokenData.expiresAt) {
                // Token expired, remove it
                tokenStorage.remove(token)
                return Result.Error(DataError.Local.UNKNOWN)
            }

            Result.Success(tokenData.shelfData)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun cleanupExpiredTokens(): Result<Unit, DataError.Local> {
        return try {
            val now = timeProvider.currentTimeMillis()
            val expiredTokens = tokenStorage.filterValues { it.expiresAt < now }.keys
            expiredTokens.forEach { tokenStorage.remove(it) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}