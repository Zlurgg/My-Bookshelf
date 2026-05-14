package uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookcase.presentation.BookcaseAction
import uk.co.zlurgg.mybookshelf.bookcase.presentation.BookcaseState
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockCreateShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockDeleteShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockDuplicateShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetAllShelvesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockGetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRenameShelfUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockReorderShelvesUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockUpdateShelfStyleUseCase
import uk.co.zlurgg.mybookshelf.testutil.mocks.StubClubOperations

@OptIn(ExperimentalCoroutinesApi::class)
class BookcaseClubActionHandlerTest {

    private lateinit var state: MutableStateFlow<BookcaseState>
    private lateinit var handler: BookcaseClubActionHandler

    private var createBookClubResult: Result<ClubOperations.BookClubCreationResult, DataError.Sync> =
        Result.Success(ClubOperations.BookClubCreationResult("test-club-code"))

    private val testClubOperations = object : ClubOperations by StubClubOperations() {
        override suspend fun createBookClub(
            name: String,
            shelfStyle: String,
            sourceShelfId: String?,
        ): Result<ClubOperations.BookClubCreationResult, DataError.Sync> = createBookClubResult
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        state = MutableStateFlow(BookcaseState())
        createBookClubResult = Result.Success(
            ClubOperations.BookClubCreationResult("test-club-code")
        )

        val useCases = BookcaseUseCases(
            getAllShelves = MockGetAllShelvesUseCase(),
            createShelf = MockCreateShelfUseCase(),
            deleteShelf = MockDeleteShelfUseCase(),
            reorderShelves = MockReorderShelvesUseCase(),
            getShelfById = MockGetShelfByIdUseCase(),
            renameShelf = MockRenameShelfUseCase(),
            updateShelfStyle = MockUpdateShelfStyleUseCase(),
            duplicateShelf = MockDuplicateShelfUseCase(),
        )

        handler = BookcaseClubActionHandler(
            state = state,
            bookClubOperations = testClubOperations,
            shelfOperations = ShelfOperationsHandler(useCases),
            scope = CoroutineScope(testDispatcher),
        )
    }

    // ==================== Item 2: Loading Indicator Tests ====================

    @Test
    fun `createBookClubDirect closes dialog on success`() = runTest {
        state.value = BookcaseState(showCreateBookClubDialog = true)

        handler.handleAction(BookcaseAction.OnCreateBookClubDirect("Test Club", ShelfStyle.DarkWood))

        assertFalse("Dialog should close on success", state.value.showCreateBookClubDialog)
        assertFalse("Should no longer be creating", state.value.isCreatingBookClub)
        assertEquals("test-club-code", state.value.bookClubCode)
    }

    @Test
    fun `createBookClubDirect closes dialog on max clubs reached`() = runTest {
        createBookClubResult = Result.Error(DataError.Sync.MAX_BOOK_CLUBS_REACHED)
        state.value = BookcaseState(showCreateBookClubDialog = true)

        handler.handleAction(BookcaseAction.OnCreateBookClubDirect("Test Club", ShelfStyle.DarkWood))

        assertFalse("Dialog should close on limit reached", state.value.showCreateBookClubDialog)
        assertTrue("Limit dialog should show", state.value.showBookClubLimitDialog)
    }

    @Test
    fun `createBookClubDirect keeps dialog open on other errors`() = runTest {
        createBookClubResult = Result.Error(DataError.Sync.UNKNOWN)
        state.value = BookcaseState(showCreateBookClubDialog = true)

        handler.handleAction(BookcaseAction.OnCreateBookClubDirect("Test Club", ShelfStyle.DarkWood))

        assertTrue("Dialog should stay open on error", state.value.showCreateBookClubDialog)
        assertFalse("Should no longer be creating", state.value.isCreatingBookClub)
        assertTrue("Error message should be set", state.value.errorMessage != null)
    }

    // ==================== Item 4: Cross-Tab Navigation Tests ====================

    @Test
    fun `createBookClub from shelf sets switchToBookClubsTab`() = runTest {
        val shelf = Bookshelf(
            id = "shelf-1",
            name = "My Shelf",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
        )

        handler.handleAction(BookcaseAction.OnCreateBookClub(shelf))

        assertTrue("Should switch to book clubs tab", state.value.switchToBookClubsTab)
    }

    @Test
    fun `createBookClubDirect does not set switchToBookClubsTab`() = runTest {
        handler.handleAction(BookcaseAction.OnCreateBookClubDirect("Test Club", ShelfStyle.DarkWood))

        assertFalse(
            "Should NOT switch tabs - user is already on Book Clubs",
            state.value.switchToBookClubsTab
        )
    }
}
