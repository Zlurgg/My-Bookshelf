package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface ShareTokenService {
    suspend fun generateToken(shelfJsonData: String): Result<String, DataError.Local>

    suspend fun getShelfDataByToken(token: String): Result<String, DataError.Local>

    suspend fun cleanupExpiredTokens(): Result<Unit, DataError.Local>
}
