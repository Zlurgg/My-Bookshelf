package uk.co.zlurgg.mybookshelf.bookshelf.domain.bookcase.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf


interface BookcaseRepository {
    fun getAllShelves(): Flow<List<Bookshelf>>
    suspend fun addShelf(shelf: Bookshelf)
    suspend fun removeShelf(shelfId: String)
    suspend fun updateShelf(shelf: Bookshelf)
}