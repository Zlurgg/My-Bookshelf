package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookcase.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf

class BookcaseRepositoryImpl(
//    private val remoteBookDataSource: RemoteBookDataSource
): BookcaseRepository {

    // Temporary in-memory storage (replace with Room/SQLite later)
    private val shelves = mutableListOf<Bookshelf>()
    private val shelvesFlow = MutableStateFlow(emptyList<Bookshelf>())

    override fun getAllShelves(): Flow<List<Bookshelf>> = shelvesFlow

    override suspend fun addShelf(shelf: Bookshelf) {
        shelves.add(shelf)
        shelvesFlow.value = shelves.toList()
    }

    override suspend fun removeShelf(shelfId: String) {
        shelves.removeAll { it.id == shelfId }
        shelvesFlow.value = shelves.toList()
    }

    override suspend fun updateShelf(shelf: Bookshelf) {
        val index = shelves.indexOfFirst { it.id == shelf.id }
        if (index != -1) {
            shelves[index] = shelf
            shelvesFlow.value = shelves.toList()
        }
    }
}