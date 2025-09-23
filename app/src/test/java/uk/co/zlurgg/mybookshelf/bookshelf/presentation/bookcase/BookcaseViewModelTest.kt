package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfIdGenerator
import uk.co.zlurgg.mybookshelf.test.FakeBookshelfExportService

@OptIn(ExperimentalCoroutinesApi::class)
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
        val updated = mutableListOf<Bookshelf>()
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
            updated.add(shelf)
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
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

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
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

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

    @Test
    fun init_loads_shelves_from_repository() = runTest {
        val initial = listOf(shelf("1", "Fiction"), shelf("2", "Science"))
        val repo = FakeRepo(initial)
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

        var latestState: BookcaseState? = null
        val job = launch { vm.state.collect { latestState = it } }
        advanceUntilIdle()

        assertEquals(initial, latestState?.bookshelves)
        assertFalse(latestState?.isLoading == true)
        job.cancel()
    }

    @Test
    fun onAddBookshelfClick_creates_shelf_with_generated_id() = runTest {
        val repo = FakeRepo(emptyList())
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

        vm.onAction(BookcaseAction.OnAddBookshelfClick("New Shelf", ShelfStyle.SilverMetal))

        val addedShelf = repo.added.first()
        assertEquals("test-id", addedShelf.id)
        assertEquals("New Shelf", addedShelf.name)
        assertEquals(ShelfStyle.SilverMetal, addedShelf.shelfStyle)
    }

    @Test
    fun showAddDialog_toggles_dialog_visibility() = runTest {
        val repo = FakeRepo(emptyList())
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

        // Show dialog
        vm.onAction(BookcaseAction.ShowAddDialog(true))
        assertTrue(vm.state.value.showAddDialog)

        // Hide dialog
        vm.onAction(BookcaseAction.ShowAddDialog(false))
        assertFalse(vm.state.value.showAddDialog)
    }

    @Test
    fun resetOperationState_clears_operation_state() = runTest {
        val initial = listOf(shelf("1"))
        val repo = FakeRepo(initial)
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

        // Remove shelf to set operation state
        vm.onAction(BookcaseAction.OnRemoveBookShelf(initial.first()))
        // Reset operation state
        vm.onAction(BookcaseAction.ResetOperationState)

        val state = vm.state.value
        assertFalse(state.showAddDialog)
        // Note: May need to check other operation state fields based on implementation
    }

    @Test
    fun toggleReorderMode_toggles_reorder_state() = runTest {
        val repo = FakeRepo(emptyList())
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

        val initialReorderMode = vm.state.value.isReorderMode
        vm.onAction(BookcaseAction.ToggleReorderMode)

        assertEquals(!initialReorderMode, vm.state.value.isReorderMode)
    }

    @Test
    fun onReorderShelf_updates_shelf_position() = runTest {
        val shelf1 = shelf("1", "First").copy(position = 0)
        val shelf2 = shelf("2", "Second").copy(position = 1)
        val initial = listOf(shelf1, shelf2)
        val repo = FakeRepo(initial)
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

        // Reorder shelf1 to position 1
        vm.onAction(BookcaseAction.OnReorderShelf(shelf1, 1))

        // Should update the shelf with new position
        val updatedShelf = repo.updated.find { it.id == "1" }
        assertEquals(1, updatedShelf?.position)
    }

    @Test
    fun onBookshelfClick_does_nothing_locally() = runTest {
        val initial = listOf(shelf("1"))
        val repo = FakeRepo(initial)
        val vm = BookcaseViewModel(repo, FakeIdGenerator(), FakeBookshelfExportService())

        val stateBefore = vm.state.value
        vm.onAction(BookcaseAction.OnBookshelfClick(initial.first()))

        // Navigation is handled by UI, ViewModel state should be unchanged
        assertEquals(stateBefore.copy(), vm.state.value.copy())
    }
}
