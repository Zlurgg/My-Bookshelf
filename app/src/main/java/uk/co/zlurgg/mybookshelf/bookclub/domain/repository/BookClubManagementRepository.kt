package uk.co.zlurgg.mybookshelf.bookclub.domain.repository

import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Club lifecycle operations: create, get, delete, rename, update style, leave, convert.
 */
interface BookClubManagementRepository {
    suspend fun createBookClub(shelfId: String): Result<String, DataError.Sync>
    suspend fun getBookClub(
        code: String,
    ): Result<BookClub?, DataError.Sync>
    suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync>
    suspend fun renameBookClub(code: String, newName: String): Result<Unit, DataError.Sync>
    suspend fun updateClubStyle(code: String, style: String): Result<Unit, DataError.Sync>
    suspend fun leaveBookClub(code: String): Result<Unit, DataError.Sync>
    suspend fun convertClubToPersonalShelf(code: String): Result<Unit, DataError.Sync>
}
