package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.AddBookshelfDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.BookcaseShelf
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.ChangeStyleDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.RenameShelfDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.SettingsMenu
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.ShelfDisplayState
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components.createShelfCallbacks
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.preview.bookshelves
import uk.co.zlurgg.mybookshelf.core.presentation.ui.components.AboutDialog
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme
import uk.co.zlurgg.mybookshelf.update.presentation.components.UpdateDialog
import uk.co.zlurgg.mybookshelf.update.presentation.components.UpToDateDialog
import uk.co.zlurgg.mybookshelf.auth.presentation.components.SignOutDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookclub.components.BookClubPreviewDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookclub.components.InviteLinkDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookclub.components.JoinBookClubDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookclub.components.ShareOptionsDialog

@Composable
fun BookcaseScreenRoot(
    viewModel: BookcaseViewModel = koinViewModel(),
    onBookshelfClick: (Bookshelf) -> Unit,
    onBookDetailClick: (String, String) -> Unit,
    onAddBookshelfClick: (String, ShelfStyle) -> Unit,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    // Handle navigation to tutorial shelf when ID is set
    LaunchedEffect(state.tutorialShelfIdForNavigation) {
        state.tutorialShelfIdForNavigation?.let { shelfId ->
            // Find the shelf by ID and navigate
            val shelf = state.bookshelves.find { it.id == shelfId }
            shelf?.let { onBookshelfClick(it) }
            // Clear the navigation flag
            viewModel.onAction(BookcaseAction.ResetOperationState)
        }
    }

    // Handle navigation to tutorial book detail when IDs are set
    LaunchedEffect(state.tutorialBookForNavigation) {
        state.tutorialBookForNavigation?.let { (shelfId, bookId) ->
            onBookDetailClick(bookId, shelfId)
            // Clear the navigation flag
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

    // Handle sign out success - navigate to sign in screen
    LaunchedEffect(state.signedOutSuccessfully) {
        if (state.signedOutSuccessfully) {
            onSignOut()
        }
    }

    // Show sign out confirmation dialog
    if (state.showSignOutDialog) {
        SignOutDialog(
            onConfirm = { viewModel.onAction(BookcaseAction.ConfirmSignOut) },
            onDismiss = { viewModel.onAction(BookcaseAction.DismissSignOutDialog) }
        )
    }

    BookcaseScreen(
        state = state,
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
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BookcaseScreen(
    state: BookcaseState,
    showAddBookshelfDialog: Boolean,
    onShowAddBookshelfDialogChange: (Boolean) -> Unit,
    onAction: (BookcaseAction) -> Unit
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

    // Watch for operation success to close dialog
    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            onShowAddBookshelfDialogChange(false)
            onAction(BookcaseAction.ResetOperationState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isReorderMode)
                            stringResource(id = R.string.reorder_shelves_title)
                        else
                            stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Reorder shelves toggle - only shown when shelves exist
                    if (state.bookshelves.isNotEmpty()) {
                        IconButton(onClick = { onAction(BookcaseAction.ToggleReorderMode) }) {
                            Icon(
                                imageVector = if (state.isReorderMode) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = stringResource(
                                    if (state.isReorderMode) R.string.menu_lock_shelves
                                    else R.string.menu_reorder_shelves
                                )
                            )
                        }
                    }
                    SettingsMenu(
                        isSignedIn = state.isSignedIn,
                        onCheckForUpdates = { onAction(BookcaseAction.CheckForUpdates) },
                        onShowHelp = { onAction(BookcaseAction.OnTutorialShelfClick) },
                        onShowAbout = { showAboutDialog = true },
                        onJoinBookClub = { onAction(BookcaseAction.ShowJoinBookClubDialog) },
                        onSignIn = { onAction(BookcaseAction.OnSignInClick) },
                        onSignOut = { onAction(BookcaseAction.ShowSignOutDialog) }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onShowAddBookshelfDialogChange(true)  }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.fab_add_shelf))
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        if (!state.isLoading && state.bookshelves.isEmpty()) {
            LazyColumn(contentPadding = padding) {
                item {
                    Text(
                        text = stringResource(id = R.string.bookcase_empty_state_hint),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(
                    items = state.bookshelves,
                    key = { it.id }
                ) { shelf ->
                    val isTutorialShelf = shelf.name == BookshelfConstants.TUTORIAL_SHELF_NAME

                    val shelfCallbacks = remember(onAction, isTutorialShelf) {
                        createShelfCallbacks(onAction, isTutorialShelf)
                    }

                    BookcaseShelf(
                        shelf = shelf,
                        callbacks = shelfCallbacks,
                        displayState = ShelfDisplayState(
                            isReorderMode = state.isReorderMode,
                            isTutorialShelf = isTutorialShelf,
                            bookCountOverride = state.bookCounts[shelf.id] ?: 0,
                            currentUserId = state.currentUserId
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

    // Update Available Dialog
    if (state.showUpdateDialog && state.availableUpdate != null) {
        UpdateDialog(
            updateInfo = state.availableUpdate,
            onDownload = { onAction(BookcaseAction.DownloadUpdate) },
            onDismiss = { onAction(BookcaseAction.DismissUpdate) }
        )
    }

    // Up to Date Dialog
    if (state.showUpToDateDialog) {
        UpToDateDialog(
            currentVersionInfo = state.currentVersionInfo,
            currentVersionName = BuildConfig.VERSION_NAME,
            onDismiss = { onAction(BookcaseAction.DismissUpToDate) }
        )
    }

    // Share options dialog
    if (state.showShareOptionsDialog) {
        ShareOptionsDialog(
            onDismiss = { onAction(BookcaseAction.DismissShareOptions) },
            onShareCopy = { onAction(BookcaseAction.OnShareCopy) },
            onCreateBookClub = { onAction(BookcaseAction.OnCreateBookClub) }
        )
    }

    // Book club invite link dialog
    state.bookClubInviteLink?.let { inviteLink ->
        InviteLinkDialog(
            clubCode = state.bookClubCode ?: "",
            inviteLink = inviteLink,
            clubName = state.shelfToShare?.name ?: "",
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
    state.bookClubPreview?.let { bookClub ->
        BookClubPreviewDialog(
            bookClub = bookClub,
            onDismiss = { onAction(BookcaseAction.DismissBookClubPreview) },
            onJoin = { onAction(BookcaseAction.OnConfirmJoinBookClub) },
            isJoining = state.joinInProgress
        )
    }

    // Join Book Club success snackbar
    state.joinBookClubSuccess?.let { shelfName ->
        LaunchedEffect(shelfName) {
            snackbarHostState.showSnackbar(
                message = "Successfully joined \"$shelfName\"!"
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
            onAction = {},
            showAddBookshelfDialog = false,
            onShowAddBookshelfDialogChange = {}
        )
    }
}