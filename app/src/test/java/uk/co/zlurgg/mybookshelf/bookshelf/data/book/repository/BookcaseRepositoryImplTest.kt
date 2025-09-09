package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

class BookcaseRepositoryImplTest {

    private class FakeDao : BookshelfDao {
        val shelves = LinkedHashMap<String, BookshelfEntity>()
        private val shelvesFlow = MutableStateFlow<List<BookshelfEntity>>(emptyList())
        private val bookCountFlows = mutableMapOf<String, MutableStateFlow<Int>>()

        var deleteCrossRefsCalledBeforeDeleteShelf = false
        private var deleteCrossRefsCalled = false
        private var deleteShelfCalled = false

        override suspend fun upsert(book: BookEntity) { /* not used */ }
        override suspend fun getBookById(id: String): BookEntity? = null
        override suspend fun deleteBook(id: String) { /* not used */ }

        override suspend fun upsertShelf(shelf: BookshelfEntity) {
            shelves[shelf.id] = shelf
            shelvesFlow.value = shelves.values.sortedBy { it.name }
        }

        override fun getAllShelves(): Flow<List<BookshelfEntity>> = shelvesFlow

        override suspend fun deleteShelf(id: String) {
            deleteShelfCalled = true
            if (deleteCrossRefsCalled && !deleteCrossRefsCalledBeforeDeleteShelf) {
                deleteCrossRefsCalledBeforeDeleteShelf = true
            }
            shelves.remove(id)
            shelvesFlow.value = shelves.values.sortedBy { it.name }
        }

        override suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef) { /* not used */ }
        override suspend fun deleteCrossRef(shelfId: String, bookId: String) { /* not used */ }
        override suspend fun deleteAllCrossRefsForShelf(shelfId: String) {
            deleteCrossRefsCalled = true
        }

        override fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>> = MutableStateFlow(emptyList())

        override fun getBookCountForShelf(shelfId: String): Flow<Int> =
            bookCountFlows.getOrPut(shelfId) { MutableStateFlow(0) }

        override fun isBookInAnyShelf(bookId: String): Flow<Boolean> = MutableStateFlow(false)
        override fun getShelvesForBook(bookId: String): Flow<List<String>> = MutableStateFlow(emptyList())

        fun setCount(shelfId: String, count: Int) {
            bookCountFlows.getOrPut(shelfId) { MutableStateFlow(0) }.value = count
        }
    }

    @Test
    fun add_and_list_shelves_maps_to_domain() = runBlocking {
        val dao = FakeDao()
        val repo = BookcaseRepositoryImpl(dao)
        repo.addShelf(Bookshelf(id = "s1", name = "A", books = emptyList(), shelfStyle = ShelfStyle.DarkWood))
        repo.addShelf(Bookshelf(id = "s2", name = "B", books = emptyList(), shelfStyle = ShelfStyle.GreyMetal))

        val list = repo.getAllShelves().first()
        assertEquals(listOf("A", "B"), list.map { it.name })
    }

    @Test
    fun removeShelf_deletes_crossrefs_first_then_shelf() = runBlocking {
        val dao = FakeDao()
        val repo = BookcaseRepositoryImpl(dao)
        repo.removeShelf("s1")
        assertTrue(dao.deleteCrossRefsCalledBeforeDeleteShelf)
    }

    @Test
    fun getBookCount_passthrough_flow() = runBlocking {
        val dao = FakeDao()
        val repo = BookcaseRepositoryImpl(dao)
        dao.setCount("s1", 3)
        val count = repo.getBookCountForShelf("s1").first()
        assertEquals(3, count)
    }
}
