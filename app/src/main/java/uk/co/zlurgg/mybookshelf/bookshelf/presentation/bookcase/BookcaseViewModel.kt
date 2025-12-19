package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.JoinResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.TutorialAccessResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.BookClubOperationsHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfManagementHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfOperationsHandler
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.update.domain.usecases.CheckForUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DismissUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DownloadUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.GetCurrentVersionInfoUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class BookcaseViewModel(
    private val shelfOperations: ShelfOperationsHandler,
    private val shelfManagement: ShelfManagementHandler,
    private val bookcaseUseCases: BookcaseUseCases,
    private val bookClubOperations: BookClubOperationsHandler,
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val downloadUpdateUseCase: DownloadUpdateUseCase,
    private val dismissUpdateUseCase: DismissUpdateUseCase,
    private val getCurrentVersionInfoUseCase: GetCurrentVersionInfoUseCase,
    private val checkSignInStatusUseCase: CheckSignInStatusUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BookcaseState())
    val state: StateFlow<BookcaseState> = _state.asStateFlow()

    init {
        loadBookshelves()
        checkSignInStatus()
    }

    private fun checkSignInStatus() {
        viewModelScope.launch {
            val isSignedIn = checkSignInStatusUseCase.execute()
            _state.update { it.copy(isSignedIn = isSignedIn) }
        }
    }

    fun onAction(action: BookcaseAction) {
        when (action) {
            is BookcaseAction.OnAddBookshelfClick -> {
                addBookshelf(action.name, action.style)
            }

            is BookcaseAction.ShowAddDialog -> {
                _state.update { it.copy(showAddDialog = action.showDialog) }
            }

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

            is BookcaseAction.ToggleReorderMode -> {
                _state.update { it.copy(isReorderMode = !it.isReorderMode) }
            }

            is BookcaseAction.OnReorderShelf -> {
                reorderShelf(action.bookshelf, action.newPosition)
            }

            is BookcaseAction.OnRemoveBookShelf -> {
                // Optimistic UI update
                _state.update { it.withShelfDeleted(action.bookshelf) }

                // Persist deletion
                viewModelScope.launch {
                    when (val deleteResult = shelfOperations.deleteShelf(action.bookshelf.id)) {
                        is Result.Success -> {
                            // Success - optimistic update already applied
                        }
                        is Result.Error -> {
                            // Revert UI on failure
                            _state.update { it.withShelfDeleteError(action.bookshelf, deleteResult.error) }
                        }
                    }
                }
            }

            is BookcaseAction.OnUndoRemove -> {
                val toRestore = state.value.recentlyDeleted
                if (toRestore != null) {
                    viewModelScope.launch {
                        when (val restoreResult = shelfOperations.restoreShelf(toRestore)) {
                            is Result.Success -> {
                                _state.update { it.withShelfRestored(toRestore) }
                            }
                            is Result.Error -> {
                                _state.update { it.withError(restoreResult.error, "restore shelf") }
                            }
                        }
                    }
                }
            }

            is BookcaseAction.OnBookshelfClick -> {
                // Navigation is handled by the screen root
            }

            is BookcaseAction.OnTutorialShelfClick -> {
                openTutorialShelf()
            }

            is BookcaseAction.ShowRenameDialog -> {
                _state.update {
                    it.copy(
                        showRenameDialog = true,
                        shelfToRename = action.bookshelf
                    )
                }
            }

            is BookcaseAction.DismissRenameDialog -> {
                _state.update {
                    it.copy(
                        showRenameDialog = false,
                        shelfToRename = null,
                        renameError = null
                    )
                }
            }

            is BookcaseAction.OnRenameShelf -> {
                renameShelf(action.shelfId, action.newName)
            }

            is BookcaseAction.ShowChangeStyleDialog -> {
                _state.update {
                    it.copy(
                        showChangeStyleDialog = true,
                        shelfToChangeStyle = action.bookshelf
                    )
                }
            }

            is BookcaseAction.DismissChangeStyleDialog -> {
                _state.update {
                    it.copy(
                        showChangeStyleDialog = false,
                        shelfToChangeStyle = null
                    )
                }
            }

            is BookcaseAction.OnChangeStyle -> {
                changeShelfStyle(action.shelfId, action.newStyle)
            }

            is BookcaseAction.OnShareShelfClick -> {
                // Show share options dialog instead of directly sharing
                _state.update { it.copy(showShareOptionsDialog = true, shelfToShare = action.shelf) }
            }

            is BookcaseAction.OnDuplicateShelfClick -> {
                duplicateShelf(action.shelf)
            }

            // Share Options Actions (Book Club)
            is BookcaseAction.DismissShareOptions -> {
                _state.update { it.copy(showShareOptionsDialog = false, shelfToShare = null) }
            }

            is BookcaseAction.OnShareCopy -> {
                _state.update { it.copy(showShareOptionsDialog = false) }
                state.value.shelfToShare?.let { shareShelf(it) }
            }

            is BookcaseAction.OnCreateBookClub -> {
                _state.update { it.copy(showShareOptionsDialog = false) }
                state.value.shelfToShare?.let { createBookClub(it) }
            }

            is BookcaseAction.DismissInviteLink -> {
                _state.update { it.copy(bookClubInviteLink = null, bookClubCode = null, shelfToShare = null) }
            }

            // Settings Menu Actions
            is BookcaseAction.CheckForUpdates -> {
                checkForUpdates()
            }

            is BookcaseAction.DownloadUpdate -> {
                downloadUpdate()
            }

            is BookcaseAction.DismissUpdate -> {
                dismissUpdate()
            }

            is BookcaseAction.DismissUpToDate -> {
                _state.update {
                    it.copy(showUpToDateDialog = false, currentVersionInfo = null)
                }
            }

            // Auth Actions
            is BookcaseAction.OnSignInClick -> {
                _state.update { it.copy(navigateToSignIn = true) }
            }

            is BookcaseAction.ResetNavigateToSignIn -> {
                _state.update { it.copy(navigateToSignIn = false) }
            }

            is BookcaseAction.ShowSignOutDialog -> {
                _state.update { it.copy(showSignOutDialog = true) }
            }

            is BookcaseAction.DismissSignOutDialog -> {
                _state.update { it.copy(showSignOutDialog = false) }
            }

            is BookcaseAction.ConfirmSignOut -> {
                signOut()
            }

            // Join Book Club Actions
            is BookcaseAction.ShowJoinBookClubDialog -> {
                _state.update {
                    it.copy(
                        showJoinBookClubDialog = true,
                        joinLookupError = null
                    )
                }
            }

            is BookcaseAction.DismissJoinBookClubDialog -> {
                _state.update {
                    it.copy(
                        showJoinBookClubDialog = false,
                        joinLookupError = null,
                        pendingInviteCode = null
                    )
                }
                bookClubOperations.clearLookupState()
            }

            is BookcaseAction.OnLookupBookClub -> {
                lookupBookClub(action.codeOrUrl)
            }

            is BookcaseAction.DismissBookClubPreview -> {
                _state.update {
                    it.copy(
                        bookClubPreview = null,
                        showJoinBookClubDialog = true
                    )
                }
            }

            is BookcaseAction.OnConfirmJoinBookClub -> {
                confirmJoinBookClub()
            }

            is BookcaseAction.DismissJoinSuccess -> {
                _state.update { it.copy(joinBookClubSuccess = null) }
            }

            is BookcaseAction.HandleInviteLink -> {
                handleInviteLink(action.code)
            }
        }
    }

    private fun addBookshelf(name: String, style: ShelfStyle) {
        viewModelScope.launch {
            when (val result = shelfOperations.createShelf(name, style, state.value.bookshelves)) {
                is Result.Success -> {
                    _state.update { it.withShelfAdded(result.data) }
                }
                is Result.Error -> {
                    _state.update { it.withError(result.error, "add shelf") }
                }
            }
        }
    }

    private fun loadBookshelves() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            bookcaseUseCases.getAllShelves.execute()
                .catch { e ->
                    val error = if (e is Exception) {
                        ErrorMapper.mapExceptionToDataError(e)
                    } else {
                        DataError.Local.UNKNOWN
                    }
                    _state.update { it.withError(error, "load shelves") }
                }
                .collect { bookcase ->
                    _state.update {
                        it.copy(
                            bookshelves = bookcase.bookshelves,
                            bookCounts = bookcase.bookCounts,
                            isLoading = false,
                            errorMessage = null,
                            defaultShelfName = "New Bookshelf ${calculateNextShelfNumber(bookcase.bookshelves)}"
                        )
                    }
                }
        }
    }

    private fun reorderShelf(shelf: Bookshelf, newPosition: Int) {
        viewModelScope.launch {
            val currentShelves = state.value.bookshelves

            when (val result = shelfManagement.reorderShelf(shelf, newPosition, currentShelves)) {
                is Result.Success -> {
                    // Optimistic UI update with the reordered shelves
                    _state.update { it.copy(bookshelves = result.data) }
                }
                is Result.Error -> {
                    // Revert on error by reloading from database
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
                    // Update the shelf name in the current state
                    _state.update {
                        it.updateShelfInList(shelfId) { shelf -> shelf.copy(name = newName) }
                          .closeRenameDialog()
                    }
                }
                is Result.Error -> {
                    // Set inline error and keep dialog open so user can see it
                    _state.update { it.withRenameError(renameResult.error) }
                }
            }
        }
    }

    private fun changeShelfStyle(shelfId: String, newStyle: ShelfStyle) {
        viewModelScope.launch {
            when (val styleResult = shelfManagement.updateShelfStyle(shelfId, newStyle)) {
                is Result.Success -> {
                    // Update the shelf style in the current state
                    _state.update {
                        it.updateShelfInList(shelfId) { shelf -> shelf.copy(shelfStyle = newStyle) }
                          .closeStyleDialog()
                    }
                }
                is Result.Error -> {
                    // Show error message
                    _state.update { it.withError(styleResult.error, "change shelf style") }
                }
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
                                    tutorialBookForNavigation = Pair(
                                        accessResult.shelfId,
                                        accessResult.bookId
                                    )
                                )
                            }
                        }
                        is TutorialAccessResult.DoNotNavigate -> {
                            // Tutorial created silently, no navigation needed
                        }
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "open tutorial")
                        )
                    }
                }
            }
        }
    }

    private fun shareShelf(shelf: Bookshelf) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val shareResult = shelfOperations.shareShelf(shelf.id)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            operationSuccess = true
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.withError(shareResult.error, "share shelf") }
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
                            operationSuccess = true
                        )
                    }
                    // Shelf list will update automatically via reactive flow
                }
                is Result.Error -> {
                    _state.update { it.withError(duplicateResult.error, "duplicate shelf") }
                }
            }
        }
    }

    private fun createBookClub(shelf: Bookshelf) {
        viewModelScope.launch {
            _state.update { it.copy(isCreatingBookClub = true, errorMessage = null) }

            when (val createResult = bookClubOperations.createBookClub(shelf.id, shelf.name)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isCreatingBookClub = false,
                            bookClubCode = createResult.data.clubCode,
                            bookClubInviteLink = createResult.data.inviteLink
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isCreatingBookClub = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(createResult.error, "create book club")
                        )
                    }
                }
            }
        }
    }

    // ============================================================================
    // Join Book Club Methods
    // ============================================================================

    private fun lookupBookClub(codeOrUrl: String) {
        viewModelScope.launch {
            _state.update { it.copy(joinLookupLoading = true, joinLookupError = null) }

            when (val lookupResult = bookClubOperations.lookupBookClub(codeOrUrl)) {
                is BookClubOperationsHandler.LookupResult.Found -> {
                    _state.update {
                        it.copy(
                            joinLookupLoading = false,
                            showJoinBookClubDialog = false,
                            bookClubPreview = lookupResult.bookClub
                        )
                    }
                }
                is BookClubOperationsHandler.LookupResult.NotFound -> {
                    _state.update {
                        it.copy(
                            joinLookupLoading = false,
                            joinLookupError = ErrorFormatter.formatDataErrorMessage(lookupResult.error, "find book club")
                        )
                    }
                }
                is BookClubOperationsHandler.LookupResult.InvalidCode -> {
                    _state.update {
                        it.copy(
                            joinLookupLoading = false,
                            joinLookupError = ErrorFormatter.formatDataErrorMessage(lookupResult.error, "validate code")
                        )
                    }
                }
            }
        }
    }

    private fun confirmJoinBookClub() {
        viewModelScope.launch {
            _state.update { it.copy(joinInProgress = true) }

            when (val joinResult = bookClubOperations.joinBookClub()) {
                is Result.Success -> {
                    when (val result = joinResult.data) {
                        is JoinResult.Success -> {
                            _state.update {
                                it.copy(
                                    joinInProgress = false,
                                    bookClubPreview = null,
                                    joinBookClubSuccess = result.shelfName
                                )
                            }
                        }
                        is JoinResult.AlreadyMember -> {
                            _state.update {
                                it.copy(
                                    joinInProgress = false,
                                    bookClubPreview = null,
                                    errorMessage = ErrorFormatter.formatDataErrorMessage(
                                        DataError.Sync.ALREADY_MEMBER,
                                        "join book club"
                                    )
                                )
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            joinInProgress = false,
                            bookClubPreview = null,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(joinResult.error, "join book club")
                        )
                    }
                }
            }

            bookClubOperations.clearLookupState()
        }
    }

    private fun handleInviteLink(code: String) {
        // Show the join dialog with the code pre-filled, then auto-trigger lookup
        _state.update {
            it.copy(
                pendingInviteCode = code,
                showJoinBookClubDialog = true,
                joinLookupError = null
            )
        }
        // Auto-trigger the lookup
        lookupBookClub(code)
    }

    private fun calculateNextShelfNumber(shelves: List<Bookshelf>): Int {
        val newBookshelfPattern = Regex("^New Bookshelf (\\d+)$")
        val existingNumbers = shelves.mapNotNull { shelf ->
            newBookshelfPattern.matchEntire(shelf.name)?.groupValues?.get(1)?.toIntOrNull()
        }
        return (existingNumbers.maxOrNull() ?: 0) + 1
    }

    // ============================================================================
    // State Update Helpers (Private Extensions)
    // ============================================================================

    private fun BookcaseState.withError(error: DataError, operation: String): BookcaseState {
        return copy(
            isLoading = false,
            errorMessage = ErrorFormatter.formatDataErrorMessage(error, operation)
        )
    }

    private fun BookcaseState.withShelfAdded(newShelf: Bookshelf): BookcaseState {
        val newShelves = bookshelves + newShelf
        return copy(
            bookshelves = newShelves,
            isLoading = false,
            operationSuccess = true,
            showAddDialog = false,
            defaultShelfName = "New Bookshelf ${calculateNextShelfNumber(newShelves)}"
        )
    }

    private fun BookcaseState.withShelfDeleted(shelf: Bookshelf): BookcaseState {
        return copy(
            bookshelves = bookshelves - shelf,
            recentlyDeleted = shelf
        )
    }

    private fun BookcaseState.withShelfDeleteError(shelf: Bookshelf, error: DataError): BookcaseState {
        return copy(
            bookshelves = bookshelves + shelf,
            recentlyDeleted = null,
            errorMessage = ErrorFormatter.formatDataErrorMessage(error, "remove shelf")
        )
    }

    private fun BookcaseState.withShelfRestored(shelf: Bookshelf): BookcaseState {
        return copy(
            bookshelves = bookshelves + shelf,
            recentlyDeleted = null,
            operationSuccess = true
        )
    }

    private fun BookcaseState.updateShelfInList(
        shelfId: String,
        transform: (Bookshelf) -> Bookshelf
    ): BookcaseState {
        val updatedShelves = bookshelves.map { shelf ->
            if (shelf.id == shelfId) transform(shelf) else shelf
        }
        return copy(bookshelves = updatedShelves)
    }

    private fun BookcaseState.closeRenameDialog(): BookcaseState {
        return copy(
            showRenameDialog = false,
            shelfToRename = null,
            renameError = null,
            operationSuccess = true,
            errorMessage = null
        )
    }

    private fun BookcaseState.closeStyleDialog(): BookcaseState {
        return copy(
            showChangeStyleDialog = false,
            shelfToChangeStyle = null,
            operationSuccess = true,
            errorMessage = null
        )
    }

    private fun BookcaseState.withRenameError(error: DataError): BookcaseState {
        return copy(renameError = ErrorFormatter.formatDataErrorMessage(error, "rename shelf"))
    }

    // ============================================================================
    // Update Checker Methods
    // ============================================================================

    private fun checkForUpdates() {
        viewModelScope.launch {
            _state.update { it.copy(isCheckingForUpdates = true) }

            // Always force check for manual updates (ignores dismissed versions)
            val updateInfo = checkForUpdateUseCase(forceCheck = true)

            if (updateInfo != null) {
                Timber.i("Update available: %s", updateInfo.versionName)
                _state.update {
                    it.copy(
                        availableUpdate = updateInfo,
                        showUpdateDialog = true,
                        isCheckingForUpdates = false
                    )
                }
            } else {
                // No update available - show "up to date" dialog
                Timber.d("No update available, fetching current version info")
                val currentInfo = getCurrentVersionInfoUseCase()
                _state.update {
                    it.copy(
                        currentVersionInfo = currentInfo,
                        showUpToDateDialog = true,
                        isCheckingForUpdates = false
                    )
                }
            }
        }
    }

    private fun downloadUpdate() {
        viewModelScope.launch {
            val updateInfo = _state.value.availableUpdate ?: return@launch
            Timber.i("Starting download for version %s", updateInfo.versionName)

            val downloadId = downloadUpdateUseCase(updateInfo)
            if (downloadId != null) {
                _state.update {
                    it.copy(
                        showUpdateDialog = false,
                        availableUpdate = null
                    )
                }
            } else {
                _state.update {
                    it.copy(errorMessage = "Failed to start download")
                }
            }
        }
    }

    private fun dismissUpdate() {
        viewModelScope.launch {
            val updateInfo = _state.value.availableUpdate ?: return@launch
            Timber.d("User dismissed update %s", updateInfo.versionName)

            dismissUpdateUseCase(updateInfo.versionName)
            _state.update {
                it.copy(
                    showUpdateDialog = false,
                    availableUpdate = null
                )
            }
        }
    }

    // ============================================================================
    // Sign Out Methods
    // ============================================================================

    private fun signOut() {
        viewModelScope.launch {
            Timber.tag(TAG).d("User confirmed sign out")
            _state.update { it.copy(isLoading = true) }

            when (val result = signOutUseCase.execute()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showSignOutDialog = false,
                            signedOutSuccessfully = true
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showSignOutDialog = false,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(result.error, "sign out")
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "BookcaseVM"
    }
}

