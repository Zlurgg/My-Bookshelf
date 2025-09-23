package uk.co.zlurgg.mybookshelf.bookshelf.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uk.co.zlurgg.mybookshelf.app.NavigationRoute
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.BookDetailsScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseAction
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfAction
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.shared.SharedMyBookshelfViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.toMaterial
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.DeepLinkAction
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.DeepLinkViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components.ImportErrorDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components.ImportLoadingDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components.ImportNameConflictDialog
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components.ImportSuccessDialog
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun MyBookShelfApp(deepLinkIntent: Intent? = null) {
    MyBookshelfTheme {
        val navController = rememberNavController()
        val deepLinkViewModel = koinViewModel<DeepLinkViewModel>()
        val deepLinkState = deepLinkViewModel.state.collectAsStateWithLifecycle().value

        // Handle deep links at the app composition level
        LaunchedEffect(deepLinkIntent) {
            deepLinkIntent?.data?.let { uri ->
                handleDeepLink(uri, deepLinkViewModel)
            }
        }

        NavHost(
            navController = navController,
            startDestination = NavigationRoute.MyBookshelfGraph.ROUTE
        ) {
            navigation(
                route = NavigationRoute.MyBookshelfGraph.ROUTE,
                startDestination = NavigationRoute.Bookcase.createRoute()
            ) {
                composable(
                    route = NavigationRoute.Bookcase.ROUTE,
                    arguments = listOf(
                        navArgument(NavigationRoute.Bookcase.ARG_NEW_SHELF) {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val viewModel = koinViewModel<BookcaseViewModel>()
                    val isNewShelf = backStackEntry.arguments?.getBoolean(
                        NavigationRoute.Bookcase.ARG_NEW_SHELF
                    ) ?: false

                    BookcaseScreenRoot(
                        viewModel = viewModel,
                        onBookshelfClick = { shelf ->
                            navController.navigate(NavigationRoute.Bookshelf.createRoute(shelf.id))
                        },
                        onAddBookshelfClick = { name, style ->
                            viewModel.onAction(BookcaseAction.OnAddBookshelfClick(name, style))
                        }
                    )

                    // Show add dialog if we're coming back from creating a new shelf
                    if (isNewShelf) {
                        LaunchedEffect(Unit) {
                            // Trigger showing the add dialog
                            viewModel.onAction(BookcaseAction.ShowAddDialog(true))
                        }
                    }
                }

                composable(
                    route = NavigationRoute.Bookshelf.ROUTE,
                    arguments = listOf(
                        navArgument(NavigationRoute.Bookshelf.KEY_ID) {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val shelfId = backStackEntry.arguments?.getString(
                        NavigationRoute.Bookshelf.KEY_ID
                    ) ?: ""

                    val sharedVm = koinViewModel<SharedMyBookshelfViewModel>()
                    LaunchedEffect(shelfId) { sharedVm.selectShelf(shelfId) }
                    val selectedShelf = sharedVm.selectedShelf.collectAsStateWithLifecycle().value

                    val viewModel = koinViewModel<BookshelfViewModel>(
                        parameters = { parametersOf(shelfId) }
                    )

                    BookshelfScreenRoot(
                        viewModel = viewModel,
                        onAddBookClick = { book ->
                            viewModel.onAction(BookshelfAction.OnAddBookClick(book = book))
                        },
                        onBookClick = { book ->
                            viewModel.onAction(BookshelfAction.OnBookClick(book))
                            viewModel.onAction(BookshelfAction.OnDismissSearchDialog)
                            navController.navigate(NavigationRoute.BookDetail.createRoute(book.id, shelfId)) {
                                launchSingleTop = true
                            }
                        },
                        onBackClick = { navController.popBackStack() },
                        shelfName = selectedShelf?.name,
                        shelfMaterial = selectedShelf?.shelfStyle?.toMaterial(),
                    )
                }

                composable(
                    route = NavigationRoute.BookDetail.ROUTE,
                    arguments = listOf(
                        navArgument(NavigationRoute.BookDetail.KEY_ID) {
                            type = NavType.StringType
                        },
                        navArgument(NavigationRoute.BookDetail.KEY_SHELF_ID) {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString(
                        NavigationRoute.BookDetail.KEY_ID
                    ) ?: ""
                    val shelfIdArg = backStackEntry.arguments?.getString(
                        NavigationRoute.BookDetail.KEY_SHELF_ID
                    ).takeIf { !it.isNullOrBlank() }

                    val viewModel = koinViewModel<BookDetailViewModel>(
                        parameters = { parametersOf(bookId, shelfIdArg) }
                    )

                    BookDetailsScreenRoot(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        // Deep link import dialogs
        when {
            deepLinkState.isLoading -> {
                ImportLoadingDialog()
            }
            deepLinkState.importSuccessful -> {
                ImportSuccessDialog(
                    onDismiss = {
                        deepLinkViewModel.onAction(DeepLinkAction.DismissSuccess)
                    }
                )
            }
            deepLinkState.error != null -> {
                ImportErrorDialog(
                    errorMessage = deepLinkState.error,
                    onDismiss = {
                        deepLinkViewModel.onAction(DeepLinkAction.DismissError)
                    }
                )
            }
            deepLinkState.nameConflict != null -> {
                ImportNameConflictDialog(
                    existingName = deepLinkState.nameConflict.existingName,
                    isLoading = deepLinkState.isLoading,
                    onDismiss = {
                        deepLinkViewModel.onAction(DeepLinkAction.DismissNameConflict)
                    },
                    onResolveConflict = { newName ->
                        deepLinkViewModel.onAction(
                            DeepLinkAction.ResolveNameConflictWithNewName(
                                jsonData = deepLinkState.nameConflict.jsonData,
                                newName = newName
                            )
                        )
                    }
                )
            }
        }
    }
}

private fun handleDeepLink(uri: Uri, deepLinkViewModel: DeepLinkViewModel) {
    if (uri.scheme == "mybookshelf" && uri.host == "share") {
        val token = uri.path?.removePrefix("/") // Remove leading slash
        if (!token.isNullOrBlank()) {
            deepLinkViewModel.onAction(DeepLinkAction.ImportFromToken(token))
        }
    }
}