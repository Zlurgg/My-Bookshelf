package uk.co.zlurgg.mybookshelf.sync.data.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.UserPreferencesFirestoreDto

/**
 * Remote user preferences operations.
 */
interface UserPreferencesDataSource {
    suspend fun getUserPreferences(userId: String): Result<UserPreferencesFirestoreDto?, DataError.Sync>
    suspend fun setUserPreferences(userId: String, preferences: UserPreferencesFirestoreDto): Result<Unit, DataError.Sync>
}
