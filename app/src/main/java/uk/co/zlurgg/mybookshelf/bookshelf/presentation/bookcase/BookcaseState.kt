package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateInfo

data class BookcaseState(
    val bookshelves: List<Bookshelf> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val recentlyDeleted: Bookshelf? = null,
    val operationSuccess: Boolean = false,
    val bookCounts: Map<String, Int> = emptyMap(),
    val isReorderMode: Boolean = false,
    val showRenameDialog: Boolean = false,
    val shelfToRename: Bookshelf? = null,
    val renameError: String? = null,
    val showChangeStyleDialog: Boolean = false,
    val shelfToChangeStyle: Bookshelf? = null,
    val defaultShelfName: String = "New Bookshelf 1",
    val tutorialShelfIdForNavigation: String? = null,
    val tutorialBookForNavigation: Pair<String, String>? = null, // (shelfId, bookId)

    // Update checker state
    val availableUpdate: UpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val currentVersionInfo: UpdateInfo? = null,
    val showUpToDateDialog: Boolean = false,
    val isCheckingForUpdates: Boolean = false,

    // Sign out state
    val showSignOutDialog: Boolean = false,
    val signedOutSuccessfully: Boolean = false,
)