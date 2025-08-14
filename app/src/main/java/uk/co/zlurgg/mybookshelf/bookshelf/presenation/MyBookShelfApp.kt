package uk.co.zlurgg.mybookshelf.bookshelf.presenation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import org.koin.compose.viewmodel.koinViewModel
import uk.co.zlurgg.mybookshelf.app.NavigationRoute
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail.BookDetailsScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfViewModel
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun MyBookShelfApp() {
    MyBookshelfTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = NavigationRoute.MyBookshelfGraph
        ) {
            // TODO User starts on bookcase, option to add new shelf or edit existing -> bookshelf add books via search using api / send bookshelf -> book details
            // Can search from anywhere takes you to the book search screen for the results and can add to either existing or create new shelf from there.
            navigation<NavigationRoute.MyBookshelfGraph>(
                startDestination = NavigationRoute.Bookcase
            ) {
                // Bookcase screen → Bookshelf
                composable<NavigationRoute.Bookcase> {
                    val viewModel = koinViewModel<BookcaseViewModel>()
                    BookcaseScreenRoot(
                        viewModel = viewModel,
                        onBookshelfClick = { shelf ->
                            navController.navigate(NavigationRoute.Bookshelf(shelf.id))
                        },
                        onAddBookshelfClick = {

                        }
                    )
                }
                // Bookshelf screen → Book details
                composable<NavigationRoute.Bookshelf> {
                    val viewModel = koinViewModel<BookshelfViewModel>()
                    BookshelfScreenRoot(
                        viewModel = viewModel,
                        onBookClick = { book ->
                            navController.navigate(NavigationRoute.BookDetail(book.id))
                        },
                        onBackClick = { navController.popBackStack() },
                    )
                }

                // Book details screen → back
                composable<NavigationRoute.BookDetail> {
                    val viewModel = koinViewModel<BookDetailViewModel>()
                    BookDetailsScreenRoot(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}