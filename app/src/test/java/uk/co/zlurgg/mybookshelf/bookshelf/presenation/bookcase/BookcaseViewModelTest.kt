package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial

class BookcaseViewModelTest {

    private class FakeRepo(initial: List<Bookshelf>) : BookcaseRepository {
        private val shelvesFlow = MutableStateFlow(initial)
        val removed = mutableListOf<String>()
        val added = mutableListOf<Bookshelf>()
        override fun getAllShelves(): Flow<List<Bookshelf>> = shelvesFlow
        override fun getBookCountForShelf(shelfId: String): Flow<Int> = MutableStateFlow(0)
        override suspend fun addShelf(shelf: Bookshelf) {
            added.add(shelf)
            shelvesFlow.value = shelvesFlow.value + shelf
        }
        override suspend fun removeShelf(shelfId: String) {
            removed.add(shelfId)
            shelvesFlow.value = shelvesFlow.value.filterNot { it.id == shelfId }
        }
        override suspend fun updateShelf(shelf: Bookshelf) {
            shelvesFlow.value = shelvesFlow.value.map { if (it.id == shelf.id) shelf else it }
        }
    }

    private fun shelf(id: String, name: String = "S") = Bookshelf(
        id = id,
        name = name,
        books = emptyList(),
        shelfMaterial = ShelfMaterial.DarkWood
    )

    @Test
    fun removeShelf_updatesState_andCallsRepository() = runBlocking {
        val initial = listOf(shelf("1"), shelf("2"))
        val repo = FakeRepo(initial)
        val vm = BookcaseViewModel(repo)

        // Remove shelf 1
        val toRemove = initial.first()
        vm.onAction(BookcaseAction.OnRemoveBookShelf(toRemove))

        // Optimistic UI update should remove it immediately
        val state = vm.state.value
        assertTrue(state.bookshelves.none { it.id == toRemove.id })
        // Repository should have been invoked
        assertEquals(listOf("1"), repo.removed)
    }

    @Test
    fun undoRemove_reinserts_andPersists() = runBlocking {
        val initial = listOf(shelf("1"), shelf("2"))
        val repo = FakeRepo(initial)
        val vm = BookcaseViewModel(repo)

        val toRemove = initial.first()
        vm.onAction(BookcaseAction.OnRemoveBookShelf(toRemove))
        // Undo
        vm.onAction(BookcaseAction.OnUndoRemove(toRemove))

        // State should contain restored shelf
        val state = vm.state.value
        assertTrue(state.bookshelves.any { it.id == toRemove.id })
        // Repo add called
        assertEquals(listOf(toRemove), repo.added)
    }
}
