package uk.co.zlurgg.mybookshelf.bookshelf.presenation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uk.co.zlurgg.mybookshelf.app.NavigationRoute
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail.BookDetailsScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseAction
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfAction
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfViewModel
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun MyBookShelfApp() {
    MyBookshelfTheme {
        val navController = rememberNavController()
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
                            // Navigate to Bookshelf screen
                            navController.navigate(NavigationRoute.Bookshelf.createRoute(shelf.id))
                        },
                        onAddBookshelfClick = { name ->
                            viewModel.onAction(BookcaseAction.OnAddBookshelfClick(name))
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

                    val viewModel = koinViewModel<BookshelfViewModel>(
                        parameters = { parametersOf(shelfId) }
                    )

                    BookshelfScreenRoot(
                        viewModel = viewModel,
                        onAddBookClick = { book ->
                            viewModel.onAction(BookshelfAction.OnAddBookClick(book = book))
                        },
                        onBookClick = { book ->
                            // Persist clicked book so details can load it by ID; navigate with shelfId
                            viewModel.onAction(BookshelfAction.OnBookClick(book))
                            // Close the search dialog to avoid stacking/looping under details
                            viewModel.onAction(BookshelfAction.OnDismissSearchDialog)
                            navController.navigate(NavigationRoute.BookDetail.createRoute(book.id, shelfId)) {
                                launchSingleTop = true
                            }
                        },
                        onBackClick = { navController.popBackStack() },

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
                    ) ?: ""

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
    }
}