package uk.co.zlurgg.mybookshelf.bookcase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.TutorialAccessResult
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers.BookcaseClubActionHandler
import uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers.ShelfManagementHandler
import uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers.ShelfOperationsHandler
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

@OptIn(ExperimentalCoroutinesApi::class)
class BookcaseViewModel(
    private val shelfOperations: ShelfOperationsHandler,
    private val shelfManagement: ShelfManagementHandler,
    private val bookcaseUseCases: BookcaseUseCases,
    private val bookClubOperations: ClubOperations,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(BookcaseState())
    val state: StateFlow<BookcaseState> = _state.asStateFlow()

    private val clubActions = BookcaseClubActionHandler(
        state = _state,
        bookClubOperations = bookClubOperations,
        shelfOperations = shelfOperations,
        scope = viewModelScope,
    )

    init {
        loadBookshelves()
        checkSignInStatus()
        validateBookClubMemberships()
    }

    private fun checkSignInStatus() {
        viewModelScope.launch {
            val isSignedIn = authUseCases.checkSignInStatus()
            val currentUserId = authUseCases.getCurrentUserId()
            _state.update { it.copy(isSignedIn = isSignedIn, currentUserId = currentUserId) }
        }
    }

    private fun validateBookClubMemberships() {
        viewModelScope.launch {
            val deletedClubNames = bookClubOperations.validateMemberships()
            if (deletedClubNames.isNotEmpty()) {
                _state.update { it.copy(deletedBookClubNames = deletedClubNames) }
            }
        }
    }

    fun onAction(action: BookcaseAction) {
        when (action) {
            // Shelf actions
            is BookcaseAction.OnAddBookshelfClick -> addBookshelf(action.name, action.style)
            is BookcaseAction.ShowAddDialog -> _state.update { it.copy(showAddDialog = action.showDialog) }
            is BookcaseAction.ResetOperationState -> {
                _state.update {
                    it.copy(
                        operationSuccess = false,
                        errorMessage = null,
                        tutorialShelfIdForNavigation = null,
                        tutorialBookForNavigation = null
                    )
                }
            }
            is BookcaseAction.ToggleReorderMode -> _state.update { it.copy(isReorderMode = !it.isReorderMode) }
            is BookcaseAction.OnReorderShelf -> reorderShelf(action.bookshelf, action.newPosition)
            is BookcaseAction.OnRemoveBookShelf -> removeShelf(action.bookshelf)
            is BookcaseAction.OnUndoRemove -> undoRemove()
            is BookcaseAction.OnBookshelfClick -> { /* Navigation handled by screen root */ }
            is BookcaseAction.OnTutorialShelfClick -> openTutorialShelf()
            is BookcaseAction.ShowRenameDialog -> {
                _state.update { it.copy(showRenameDialog = true, shelfToRename = action.bookshelf) }
            }
            is BookcaseAction.DismissRenameDialog -> {
                _state.update { it.copy(showRenameDialog = false, shelfToRename = null, renameError = null) }
            }
            is BookcaseAction.OnRenameShelf -> renameShelf(action.shelfId, action.newName)
            is BookcaseAction.ShowChangeStyleDialog -> {
                _state.update { it.copy(showChangeStyleDialog = true, shelfToChangeStyle = action.bookshelf) }
            }
            is BookcaseAction.DismissChangeStyleDialog -> {
                _state.update { it.copy(showChangeStyleDialog = false, shelfToChangeStyle = null) }
            }
            is BookcaseAction.OnChangeStyle -> changeShelfStyle(action.shelfId, action.newStyle)
            is BookcaseAction.OnDuplicateShelfClick -> duplicateShelf(action.shelf)
            is BookcaseAction.ResetSwitchToPersonalTab -> _state.update { it.copy(switchToPersonalTab = false) }
            is BookcaseAction.ResetSwitchToBookClubsTab -> _state.update { it.copy(switchToBookClubsTab = false) }
            is BookcaseAction.DismissShelfLimitDialog -> _state.update { it.copy(showShelfLimitDialog = false) }

            // Auth actions
            is BookcaseAction.OnSignInClick -> _state.update { it.copy(navigateToSignIn = true) }
            is BookcaseAction.ResetNavigateToSignIn -> _state.update { it.copy(navigateToSignIn = false) }

            // Book club actions — delegated to handler
            is BookcaseAction.OnCreateBookClub,
            is BookcaseAction.OnInviteToClub,
            is BookcaseAction.DismissInviteLink,
            is BookcaseAction.ShowDeleteBookClubDialog,
            is BookcaseAction.DismissDeleteBookClubDialog,
            is BookcaseAction.ConfirmDeleteBookClub,
            is BookcaseAction.ShowLeaveBookClubDialog,
            is BookcaseAction.DismissLeaveBookClubDialog,
            is BookcaseAction.ConfirmLeaveBookClub,
            is BookcaseAction.ShowJoinBookClubDialog,
            is BookcaseAction.DismissJoinBookClubDialog,
            is BookcaseAction.OnLookupBookClub,
            is BookcaseAction.DismissBookClubPreview,
            is BookcaseAction.OnConfirmJoinBookClub,
            is BookcaseAction.DismissJoinSuccess,
            is BookcaseAction.HandleInviteLink,
            is BookcaseAction.DismissDeletedBookClubsNotification,
            is BookcaseAction.DismissBookClubLimitDialog,
            -> clubActions.handleAction(action)
        }
    }

    private fun addBookshelf(name: String, style: ShelfStyle) {
        viewModelScope.launch {
            when (val result = shelfOperations.createShelf(name, style, state.value.bookshelves)) {
                is Result.Success -> _state.update { it.withShelfAdded(result.data) }
                is Result.Error -> {
                    if (result.error == DataError.Local.MAX_SHELVES_REACHED) {
                        _state.update { it.copy(showShelfLimitDialog = true) }
                    } else {
                        _state.update { it.withError(result.error, "add shelf") }
                    }
                }
            }
        }
    }

    private fun loadBookshelves() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            bookcaseUseCases.getAllShelves()
                .catch { e ->
                    val error = if (e is Exception) {
                        ErrorMapper.mapExceptionToDataError(e)
                    } else {
                        DataError.Local.UNKNOWN
                    }
                    _state.update { it.withError(error, "load shelves") }
                }
                .collect { bookcase ->
                    val personalCount = bookcase.bookshelves.count {
                        !it.isBookClub && it.name != BookshelfConstants.TUTORIAL_SHELF_NAME
                    }
                    val clubCount = bookcase.bookshelves.count { it.isBookClub }

                    _state.update {
                        it.copy(
                            bookshelves = bookcase.bookshelves,
                            bookCounts = bookcase.bookCounts,
                            isLoading = false,
                            errorMessage = null,
                            defaultShelfName = "New Bookshelf ${calculateNextShelfNumber(bookcase.bookshelves)}",
                            personalShelfCount = personalCount,
                            bookClubCount = clubCount
                        )
                    }
                }
        }
    }

    private fun removeShelf(bookshelf: Bookshelf) {
        _state.update { it.withShelfDeleted(bookshelf) }

        viewModelScope.launch {
            when (val deleteResult = shelfOperations.deleteShelf(bookshelf.id)) {
                is Result.Success -> { /* Optimistic update already applied */ }
                is Result.Error -> _state.update { it.withShelfDeleteError(bookshelf, deleteResult.error) }
            }
        }
    }

    private fun undoRemove() {
        val toRestore = state.value.recentlyDeleted ?: return
        viewModelScope.launch {
            when (val restoreResult = shelfOperations.restoreShelf(toRestore)) {
                is Result.Success -> _state.update { it.withShelfRestored(toRestore) }
                is Result.Error -> _state.update { it.withError(restoreResult.error, "restore shelf") }
            }
        }
    }

    private fun reorderShelf(shelf: Bookshelf, newPosition: Int) {
        viewModelScope.launch {
            when (val result = shelfManagement.reorderShelf(shelf, newPosition, state.value.bookshelves)) {
                is Result.Success -> _state.update { it.copy(bookshelves = result.data) }
                is Result.Error -> {
                    _state.update { it.withError(result.error, "reorder shelves") }
                    loadBookshelves()
                }
            }
        }
    }

    private fun renameShelf(shelfId: String, newName: String) {
        viewModelScope.launch {
            when (val renameResult = shelfManagement.renameShelf(shelfId, newName)) {
                is Result.Success -> {
                    _state.update {
                        it.updateShelfInList(shelfId) { shelf -> shelf.copy(name = newName) }
                            .closeRenameDialog()
                    }
                }
                is Result.Error -> _state.update { it.withRenameError(renameResult.error) }
            }
        }
    }

    private fun changeShelfStyle(shelfId: String, newStyle: ShelfStyle) {
        viewModelScope.launch {
            when (val styleResult = shelfManagement.updateShelfStyle(shelfId, newStyle)) {
                is Result.Success -> {
                    _state.update {
                        it.updateShelfInList(shelfId) { shelf -> shelf.copy(shelfStyle = newStyle) }
                            .closeStyleDialog()
                    }
                }
                is Result.Error -> _state.update { it.withError(styleResult.error, "change shelf style") }
            }
        }
    }

    private fun openTutorialShelf() {
        viewModelScope.launch {
            when (val result = shelfManagement.accessTutorialShelf()) {
                is Result.Success -> {
                    when (val accessResult = result.data) {
                        is TutorialAccessResult.NavigateToBook -> {
                            _state.update {
                                it.copy(
                                    tutorialBookForNavigation = Pair(accessResult.shelfId, accessResult.bookId)
                                )
                            }
                        }
                        is TutorialAccessResult.DoNotNavigate -> { /* Silent */ }
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "open tutorial"))
                    }
                }
            }
        }
    }

    private fun duplicateShelf(shelf: Bookshelf) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val duplicateResult = shelfOperations.duplicateShelf(shelf.id)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            operationSuccess = true,
                            switchToPersonalTab = shelf.isBookClub
                        )
                    }
                }
                is Result.Error -> _state.update { it.withError(duplicateResult.error, "duplicate shelf") }
            }
        }
    }
}
