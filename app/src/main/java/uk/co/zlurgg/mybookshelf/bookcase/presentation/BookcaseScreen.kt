package uk.co.zlurgg.mybookshelf.bookcase.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.app.presentation.theme.ThemeAction
import uk.co.zlurgg.mybookshelf.app.presentation.theme.ThemeViewModel
import uk.co.zlurgg.mybookshelf.core.presentation.ui.components.ThemeSelectorDialog
import uk.co.zlurgg.mybookshelf.core.presentation.util.launchInAppReview
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.auth.presentation.components.SignInRequiredDialog
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookcase.presentation.components.AddBookshelfDialog
import uk.co.zlurgg.mybookshelf.bookcase.presentation.components.BookcaseShelf
import uk.co.zlurgg.mybookshelf.bookcase.presentation.components.ChangeStyleDialog
import uk.co.zlurgg.mybookshelf.bookcase.presentation.components.RenameShelfDialog
import uk.co.zlurgg.mybookshelf.bookcase.presentation.components.SettingsMenu
import uk.co.zlurgg.mybookshelf.bookcase.presentation.components.ShelfDisplayState
import uk.co.zlurgg.mybookshelf.bookcase.presentation.components.ShelfLimitDialog
import uk.co.zlurgg.mybookshelf.bookcase.presentation.components.createShelfCallbacks
import uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers.ShelfOperationsHandler
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookclub.presentation.components.BookClubPreviewDialog
import uk.co.zlurgg.mybookshelf.bookclub.presentation.components.DeleteBookClubDialog
import uk.co.zlurgg.mybookshelf.bookclub.presentation.components.ClubInviteDialog
import uk.co.zlurgg.mybookshelf.bookclub.presentation.components.JoinBookClubDialog
import uk.co.zlurgg.mybookshelf.bookclub.presentation.components.LeaveBookClubDialog
import uk.co.zlurgg.mybookshelf.book.presentation.preview.bookshelves
import uk.co.zlurgg.mybookshelf.core.presentation.ui.components.AboutDialog
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun BookcaseScreenRoot(
    viewModel: BookcaseViewModel = koinViewModel(),
    isBookClub: Boolean = false,
    onBookshelfClick: (Bookshelf) -> Unit,
    onBookDetailClick: (String, String) -> Unit,
    onAddBookshelfClick: (String, ShelfStyle) -> Unit,
    onSignIn: () -> Unit = {},
    onAccountClick: (Boolean) -> Unit = {},
    onSwitchToBookClubs: () -> Unit = {},
    onSwitchToPersonalTab: () -> Unit = {},
    createClubForShelfId: String? = null,
    onCreateClubConsumed: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val selectedTab = if (isBookClub) BookcaseTab.BOOK_CLUBS else BookcaseTab.MY_SHELVES
    var showSignInRequiredDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val activity = LocalActivity.current as ComponentActivity
    val themeViewModel = koinViewModel<ThemeViewModel>(
        viewModelStoreOwner = activity
    )
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()

    // Create book club for shelf navigated from BookshelfScreen FAB
    LaunchedEffect(createClubForShelfId, state.bookshelves) {
        if (createClubForShelfId != null && state.bookshelves.isNotEmpty()) {
            onCreateClubConsumed()
            val shelf = state.bookshelves.find { it.id == createClubForShelfId }
            if (shelf != null) {
                if (!state.isSignedIn) {
                    showSignInRequiredDialog = true
                } else {
                    viewModel.onAction(BookcaseAction.OnCreateBookClub(shelf))
                }
            }
        }
    }

    // Handle navigation to tutorial shelf when ID is set
    LaunchedEffect(state.tutorialShelfIdForNavigation) {
        state.tutorialShelfIdForNavigation?.let { shelfId ->
            val shelf = state.bookshelves.find { it.id == shelfId }
            shelf?.let { onBookshelfClick(it) }
            viewModel.onAction(BookcaseAction.ResetOperationState)
        }
    }

    // Handle navigation to tutorial book detail when IDs are set
    LaunchedEffect(state.tutorialBookForNavigation) {
        state.tutorialBookForNavigation?.let { (shelfId, bookId) ->
            onBookDetailClick(bookId, shelfId)
            viewModel.onAction(BookcaseAction.ResetOperationState)
        }
    }

    // Handle sign in navigation for guest users
    LaunchedEffect(state.navigateToSignIn) {
        if (state.navigateToSignIn) {
            onSignIn()
            viewModel.onAction(BookcaseAction.ResetNavigateToSignIn)
        }
    }

    // Handle cross-tab navigation after club creation (from-shelf path)
    LaunchedEffect(state.switchToBookClubsTab) {
        if (state.switchToBookClubsTab) {
            onSwitchToBookClubs()
            viewModel.onAction(BookcaseAction.ResetSwitchToBookClubsTab)
        }
    }

    // Handle cross-tab navigation after duplicating a club shelf
    LaunchedEffect(state.switchToPersonalTab) {
        if (state.switchToPersonalTab) {
            onSwitchToPersonalTab()
            viewModel.onAction(BookcaseAction.ResetSwitchToPersonalTab)
        }
    }

    // Sign in required dialog for guest users attempting Book Clubs
    if (showSignInRequiredDialog) {
        SignInRequiredDialog(
            title = stringResource(R.string.sign_in_required_book_clubs_title),
            message = stringResource(R.string.sign_in_required_book_clubs_message),
            onSignIn = {
                showSignInRequiredDialog = false
                onSignIn()
            },
            onDismiss = { showSignInRequiredDialog = false }
        )
    }

    BookcaseScreen(
        state = state,
        selectedTab = selectedTab,
        showAddBookshelfDialog = showDialog,
        onShowAddBookshelfDialogChange = { showDialog = it },
        onAction = { action ->
            when (action) {
                is BookcaseAction.OnBookshelfClick -> {
                    onBookshelfClick(action.bookshelf)
                }
                is BookcaseAction.OnAddBookshelfClick -> {
                    onAddBookshelfClick(action.name, action.style)
                }
                is BookcaseAction.ShowAddDialog -> {
                    showDialog = action.showDialog
                }
                is BookcaseAction.OnCreateBookClub -> {
                    if (!state.isSignedIn) {
                        showSignInRequiredDialog = true
                    } else {
                        viewModel.onAction(action)
                    }
                }
                is BookcaseAction.ShowCreateBookClubDialog -> {
                    if (action.showDialog && !state.isSignedIn) {
                        showSignInRequiredDialog = true
                    } else {
                        viewModel.onAction(action)
                    }
                }
                is BookcaseAction.ShowJoinBookClubDialog -> {
                    if (!state.isSignedIn) {
                        showSignInRequiredDialog = true
                    } else {
                        viewModel.onAction(action)
                    }
                }
                else -> viewModel.onAction(action)
            }
        },
        onAccountClick = { onAccountClick(state.isSignedIn) },
        onShowThemeSelector = { showThemeDialog = true },
        onRateApp = { launchInAppReview(activity) },
    )

    // Theme Selector Dialog
    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentTheme = themeState.themeMode,
            onThemeSelected = { mode ->
                themeViewModel.onAction(ThemeAction.SetThemeMode(mode))
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BookcaseScreen(
    state: BookcaseState,
    selectedTab: BookcaseTab,
    showAddBookshelfDialog: Boolean,
    onShowAddBookshelfDialogChange: (Boolean) -> Unit,
    onAction: (BookcaseAction) -> Unit,
    onAccountClick: () -> Unit = {},
    onShowThemeSelector: () -> Unit = {},
    onRateApp: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Show error snackbar if needed
    if (state.errorMessage != null) {
        LaunchedEffect(snackbarHostState) {
            snackbarHostState.showSnackbar(state.errorMessage)
            // Clear error after showing
            onAction(BookcaseAction.ResetOperationState)
        }
    }

    // Show converted book clubs notification (clubs deleted by owner, converted to personal shelves)
    if (state.deletedBookClubsMessage != null) {
        LaunchedEffect(state.deletedBookClubsMessage) {
            snackbarHostState.showSnackbar(state.deletedBookClubsMessage)
            onAction(BookcaseAction.DismissDeletedBookClubsNotification)
        }
    }

    // Watch for operation success to close dialog
    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            onShowAddBookshelfDialogChange(false)
            onAction(BookcaseAction.ResetOperationState)
        }
    }

    // Filter shelves based on selected tab
    val displayedShelves = remember(state.bookshelves, selectedTab) {
        state.bookshelves.filter { shelf ->
            shelf.isBookClub == (selectedTab == BookcaseTab.BOOK_CLUBS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            state.isReorderMode -> stringResource(id = R.string.reorder_shelves_title)
                            else -> stringResource(id = selectedTab.labelResId)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Reorder shelves toggle - only shown when shelves exist in current tab
                    if (displayedShelves.isNotEmpty()) {
                        IconButton(onClick = { onAction(BookcaseAction.ToggleReorderMode) }) {
                            Icon(
                                imageVector = if (state.isReorderMode) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = stringResource(
                                    if (state.isReorderMode) {
                                        R.string.menu_lock_shelves
                                    } else {
                                        R.string.menu_reorder_shelves
                                    }
                                )
                            )
                        }
                    }
                    // Profile icon
                    IconButton(onClick = onAccountClick) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.cd_profile)
                        )
                    }
                    SettingsMenu(
                        onShowHelp = { onAction(BookcaseAction.OnTutorialShelfClick) },
                        onShowAbout = { showAboutDialog = true },
                        onShowThemeSelector = onShowThemeSelector,
                        onRateApp = onRateApp,
                        onJoinBookClub = { onAction(BookcaseAction.ShowJoinBookClubDialog) },
                        isSignedIn = state.isSignedIn,
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == BookcaseTab.BOOK_CLUBS && state.isSignedIn) {
                Row {
                    FloatingActionButton(
                        onClick = { onAction(BookcaseAction.ShowJoinBookClubDialog) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = stringResource(R.string.fab_join_book_club)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FloatingActionButton(
                        onClick = { onAction(BookcaseAction.ShowCreateBookClubDialog(true)) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.fab_create_book_club)
                        )
                    }
                }
            } else if (selectedTab == BookcaseTab.MY_SHELVES) {
                FloatingActionButton(
                    onClick = { onShowAddBookshelfDialogChange(true) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.fab_add_shelf)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        // Counter showing current/max shelves
        val maxCount = if (selectedTab == BookcaseTab.BOOK_CLUBS) {
            BookClub.MAX_BOOK_CLUBS
        } else {
            ShelfOperationsHandler.MAX_PERSONAL_SHELVES
        }
        val currentCount = if (selectedTab == BookcaseTab.BOOK_CLUBS) {
            state.bookClubCount
        } else {
            state.personalShelfCount
        }

        if (!state.isLoading && displayedShelves.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = stringResource(R.string.shelf_limit_counter, currentCount, maxCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val isClubTab = selectedTab == BookcaseTab.BOOK_CLUBS
                val emptyIcon = if (isClubTab) Icons.Outlined.Groups else Icons.Outlined.CollectionsBookmark
                val titleRes = when {
                    isClubTab && !state.isSignedIn -> R.string.bookcase_empty_book_clubs_guest_title
                    isClubTab -> R.string.bookcase_empty_book_clubs_title
                    else -> R.string.bookcase_empty_personal_title
                }
                val subtitleRes = when {
                    isClubTab && !state.isSignedIn -> R.string.bookcase_empty_book_clubs_guest_subtitle
                    isClubTab -> R.string.bookcase_empty_book_clubs_subtitle
                    else -> R.string.bookcase_empty_personal_subtitle
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = emptyIcon,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(subtitleRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = padding) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.shelf_limit_counter, currentCount, maxCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(
                    items = displayedShelves,
                    key = { it.id }
                ) { shelf ->
                    val isTutorialShelf = shelf.name == BookshelfConstants.TUTORIAL_SHELF_NAME

                    val shelfCallbacks = remember(onAction, isTutorialShelf, state.currentUserId) {
                        createShelfCallbacks(onAction, isTutorialShelf, state.currentUserId)
                    }

                    BookcaseShelf(
                        shelf = shelf,
                        callbacks = shelfCallbacks,
                        displayState = ShelfDisplayState(
                            isReorderMode = state.isReorderMode,
                            isTutorialShelf = isTutorialShelf,
                            bookCountOverride = state.bookCounts[shelf.id] ?: 0,
                            currentUserId = state.currentUserId,
                            memberCount = shelf.clubCode?.let { state.clubMemberCounts[it] }
                        ),
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showAddBookshelfDialog) {
        AddBookshelfDialog(
            onDismiss = {
                if (!state.isLoading) onShowAddBookshelfDialogChange(false)
            },
            onAddShelf = { shelfName, style ->
                onAction(BookcaseAction.OnAddBookshelfClick(shelfName, style))
            },
            isLoading = state.isLoading,
            defaultName = state.defaultShelfName
        )
    }

    if (state.showCreateBookClubDialog) {
        AddBookshelfDialog(
            onDismiss = { onAction(BookcaseAction.ShowCreateBookClubDialog(false)) },
            onAddShelf = { name, style ->
                onAction(BookcaseAction.OnCreateBookClubDirect(name, style))
            },
            isLoading = state.isCreatingBookClub,
            defaultName = stringResource(R.string.default_book_club_name),
            title = stringResource(R.string.dialog_create_book_club_title)
        )
    }

    if (state.showRenameDialog && state.shelfToRename != null) {
        RenameShelfDialog(
            currentName = state.shelfToRename.name,
            errorMessage = state.renameError,
            onDismiss = {
                onAction(BookcaseAction.DismissRenameDialog)
            },
            onRename = { newName ->
                onAction(BookcaseAction.OnRenameShelf(state.shelfToRename.id, newName))
            }
        )
    }

    if (state.showChangeStyleDialog && state.shelfToChangeStyle != null) {
        ChangeStyleDialog(
            currentStyle = state.shelfToChangeStyle.shelfStyle,
            onDismiss = {
                onAction(BookcaseAction.DismissChangeStyleDialog)
            },
            onChangeStyle = { newStyle ->
                onAction(BookcaseAction.OnChangeStyle(state.shelfToChangeStyle.id, newStyle))
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = { showAboutDialog = false }
        )
    }

    // Book club invite code dialog
    state.bookClubCode?.let { code ->
        ClubInviteDialog(
            clubCode = code,
            clubName = state.bookClubName ?: "",
            isNewClub = state.isNewlyCreatedBookClub,
            onDismiss = { onAction(BookcaseAction.DismissInviteLink) }
        )
    }

    // Join Book Club dialog - code entry
    if (state.showJoinBookClubDialog) {
        JoinBookClubDialog(
            onDismiss = { onAction(BookcaseAction.DismissJoinBookClubDialog) },
            onLookup = { codeOrUrl -> onAction(BookcaseAction.OnLookupBookClub(codeOrUrl)) },
            isLoading = state.joinLookupLoading,
            errorMessage = state.joinLookupError,
            initialCode = state.pendingInviteCode ?: ""
        )
    }

    // Book Club Preview dialog - confirmation before joining
    state.bookClubPreview?.let { preview ->
        BookClubPreviewDialog(
            clubName = preview.clubName,
            memberCount = preview.memberCount,
            onDismiss = { onAction(BookcaseAction.DismissBookClubPreview) },
            onJoin = { onAction(BookcaseAction.OnConfirmJoinBookClub) },
            isJoining = state.joinInProgress
        )
    }

    // Delete shelf confirmation dialog
    if (state.showDeleteShelfDialog && state.shelfToDelete != null) {
        AlertDialog(
            onDismissRequest = { onAction(BookcaseAction.DismissDeleteShelfDialog) },
            title = { Text(stringResource(R.string.delete_shelf_title)) },
            text = {
                Text(stringResource(R.string.delete_shelf_message, state.shelfToDelete.name))
            },
            confirmButton = {
                TextButton(onClick = { onAction(BookcaseAction.ConfirmDeleteShelf) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(BookcaseAction.DismissDeleteShelfDialog) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Book Club confirmation dialog
    if (state.showDeleteBookClubDialog && state.shelfToDelete != null) {
        DeleteBookClubDialog(
            clubName = state.shelfToDelete.name,
            onConfirm = { onAction(BookcaseAction.ConfirmDeleteBookClub) },
            onDismiss = { onAction(BookcaseAction.DismissDeleteBookClubDialog) }
        )
    }

    // Leave Book Club confirmation dialog
    if (state.showLeaveBookClubDialog && state.shelfToLeave != null) {
        LeaveBookClubDialog(
            clubName = state.shelfToLeave.name,
            onConfirm = { onAction(BookcaseAction.ConfirmLeaveBookClub) },
            onDismiss = { onAction(BookcaseAction.DismissLeaveBookClubDialog) }
        )
    }

    // Shelf limit reached dialog
    if (state.showShelfLimitDialog) {
        ShelfLimitDialog(
            title = stringResource(R.string.shelf_limit_reached_title),
            message = stringResource(R.string.shelf_limit_reached_message, ShelfOperationsHandler.MAX_PERSONAL_SHELVES),
            onDismiss = { onAction(BookcaseAction.DismissShelfLimitDialog) }
        )
    }

    // Book club limit reached dialog
    if (state.showBookClubLimitDialog) {
        ShelfLimitDialog(
            title = stringResource(R.string.book_club_limit_reached_title),
            message = stringResource(R.string.book_club_limit_reached_message, BookClub.MAX_BOOK_CLUBS),
            onDismiss = { onAction(BookcaseAction.DismissBookClubLimitDialog) }
        )
    }

    // Join Book Club success snackbar
    state.joinBookClubSuccess?.let { shelfName ->
        val successMessage = stringResource(R.string.join_book_club_success, shelfName)
        LaunchedEffect(shelfName) {
            snackbarHostState.showSnackbar(
                message = successMessage
            )
            onAction(BookcaseAction.DismissJoinSuccess)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookcaseScreenPreview() {
    MyBookshelfTheme {
        BookcaseScreen(
            state = BookcaseState(
                bookshelves = bookshelves,
            ),
            selectedTab = BookcaseTab.MY_SHELVES,
            onAction = {},
            showAddBookshelfDialog = false,
            onShowAddBookshelfDialogChange = {}
        )
    }
}
