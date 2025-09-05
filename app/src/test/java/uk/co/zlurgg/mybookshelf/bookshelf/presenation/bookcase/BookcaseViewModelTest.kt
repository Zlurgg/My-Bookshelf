package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfIdGenerator

@RunWith(RobolectricTestRunner::class)
class BookcaseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeIdGenerator : BookshelfIdGenerator {
        override fun generateId(): String = "test-id"
    }

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
        shelfStyle = ShelfStyle.DarkWood
    )

    @Test
    fun removeShelf_updatesState_andCallsRepository() = runTest {
        val initial = listOf(shelf("1"), shelf("2"))
        val repo = FakeRepo(initial)
        val vm = BookcaseViewModel(repo, FakeIdGenerator())

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
    fun undoRemove_reinserts_andPersists() = runTest {
        val initial = listOf(shelf("1"), shelf("2"))
        val repo = FakeRepo(initial)
        val vm = BookcaseViewModel(repo, FakeIdGenerator())

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
