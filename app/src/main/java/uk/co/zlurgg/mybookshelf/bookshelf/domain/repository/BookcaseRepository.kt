package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf

interface BookcaseRepository {
    fun getAllShelves(): Flow<List<Bookshelf>>
    fun getBookCountForShelf(shelfId: String): Flow<Int>
    suspend fun addShelf(shelf: Bookshelf)
    suspend fun removeShelf(shelfId: String)
    suspend fun updateShelf(shelf: Bookshelf)
}