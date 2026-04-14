package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf

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
    val showSignOutDialog: Boolean = false,
    val signedOutSuccessfully: Boolean = false,
    val navigateToSignIn: Boolean = false,

    // Book Club invite state
    val isCreatingBookClub: Boolean = false,
    val bookClubInviteLink: String? = null,
    val bookClubCode: String? = null,
    val bookClubName: String? = null,
    val isNewlyCreatedBookClub: Boolean = true,

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
    val bookClubPreview: BookClub? = null,
    val joinInProgress: Boolean = false,

    // Join Book Club - Success
    val joinBookClubSuccess: String? = null,

    // For invite link - pre-filled code
    val pendingInviteCode: String? = null,

    // Tab navigation after operations
    val switchToPersonalTab: Boolean = false,
    val switchToBookClubsTab: Boolean = false,

    // Converted book clubs notification (clubs deleted by creator, converted to personal shelves)
    val deletedBookClubNames: List<String> = emptyList(),

    // Shelf limits
    val personalShelfCount: Int = 0,
    val bookClubCount: Int = 0,
    val showShelfLimitDialog: Boolean = false,
    val showBookClubLimitDialog: Boolean = false,
)
