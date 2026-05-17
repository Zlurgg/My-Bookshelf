package uk.co.zlurgg.mybookshelf.bookcase.presentation

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter

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

    // Auth state
    val isSignedIn: Boolean = false,
    val currentUserId: String? = null,
    val navigateToSignIn: Boolean = false,

    // Book Club invite state
    val showCreateBookClubDialog: Boolean = false,
    val isCreatingBookClub: Boolean = false,
    val bookClubCode: String? = null,
    val bookClubName: String? = null,
    val isNewlyCreatedBookClub: Boolean = true,

    // Delete shelf confirmation
    val showDeleteShelfDialog: Boolean = false,

    // Delete Book Club confirmation
    val showDeleteBookClubDialog: Boolean = false,
    val shelfToDelete: Bookshelf? = null,

    // Leave Book Club confirmation
    val showLeaveBookClubDialog: Boolean = false,
    val shelfToLeave: Bookshelf? = null,

    // Join Book Club - Step 1: Code entry
    val showJoinBookClubDialog: Boolean = false,
    val joinLookupLoading: Boolean = false,
    val joinLookupError: String? = null,

    // Join Book Club - Step 2: Preview
    val bookClubPreview: BookClubPreview? = null,
    val joinInProgress: Boolean = false,

    // Join Book Club - Success
    val joinBookClubSuccess: String? = null,

    // For invite link - pre-filled code
    val pendingInviteCode: String? = null,

    // Tab navigation after operations
    val switchToPersonalTab: Boolean = false,
    val switchToBookClubsTab: Boolean = false,
    val pendingSwitchToBookClubsTab: Boolean = false,

    // Converted book clubs notification (clubs deleted by creator, converted to personal shelves)
    val deletedBookClubNames: List<String> = emptyList(),
    val deletedBookClubsMessage: String? = null,

    // Member counts for book club shelf cards (clubCode -> memberCount)
    val clubMemberCounts: Map<String, Int> = emptyMap(),

    // Shelf limits
    val personalShelfCount: Int = 0,
    val bookClubCount: Int = 0,
    val showShelfLimitDialog: Boolean = false,
    val showBookClubLimitDialog: Boolean = false,
)

// ============================================================================
// State Reducers (Extensions)
// ============================================================================

internal fun BookcaseState.withError(error: DataError, operation: String): BookcaseState {
    return copy(
        isLoading = false,
        errorMessage = ErrorFormatter.formatDataErrorMessage(error, operation)
    )
}

internal fun BookcaseState.withShelfAdded(newShelf: Bookshelf): BookcaseState {
    val newShelves = bookshelves + newShelf
    return copy(
        bookshelves = newShelves,
        isLoading = false,
        operationSuccess = true,
        showAddDialog = false,
        defaultShelfName = "New Bookshelf ${calculateNextShelfNumber(newShelves)}"
    )
}

internal fun BookcaseState.withShelfDeleted(shelf: Bookshelf): BookcaseState {
    return copy(
        bookshelves = bookshelves - shelf,
        recentlyDeleted = shelf
    )
}

internal fun BookcaseState.withShelfDeleteError(shelf: Bookshelf, error: DataError): BookcaseState {
    return copy(
        bookshelves = bookshelves + shelf,
        recentlyDeleted = null,
        errorMessage = ErrorFormatter.formatDataErrorMessage(error, "remove shelf")
    )
}

internal fun BookcaseState.withShelfRestored(shelf: Bookshelf): BookcaseState {
    return copy(
        bookshelves = bookshelves + shelf,
        recentlyDeleted = null,
        operationSuccess = true
    )
}

internal fun BookcaseState.updateShelfInList(
    shelfId: String,
    transform: (Bookshelf) -> Bookshelf,
): BookcaseState {
    val updatedShelves = bookshelves.map { shelf ->
        if (shelf.id == shelfId) transform(shelf) else shelf
    }
    return copy(bookshelves = updatedShelves)
}

internal fun BookcaseState.closeRenameDialog(): BookcaseState {
    return copy(
        showRenameDialog = false,
        shelfToRename = null,
        renameError = null,
        operationSuccess = true,
        errorMessage = null
    )
}

internal fun BookcaseState.closeStyleDialog(): BookcaseState {
    return copy(
        showChangeStyleDialog = false,
        shelfToChangeStyle = null,
        operationSuccess = true,
        errorMessage = null
    )
}

internal fun BookcaseState.withRenameError(error: DataError): BookcaseState {
    return copy(renameError = ErrorFormatter.formatDataErrorMessage(error, "rename shelf"))
}

data class BookClubPreview(
    val clubName: String,
    val clubCode: String,
    val memberCount: Int
)

internal fun calculateNextShelfNumber(shelves: List<Bookshelf>): Int {
    val newBookshelfPattern = Regex("^New Bookshelf (\\d+)$")
    val existingNumbers = shelves.mapNotNull { shelf ->
        newBookshelfPattern.matchEntire(shelf.name)?.groupValues?.get(1)?.toIntOrNull()
    }
    return (existingNumbers.maxOrNull() ?: 0) + 1
}
