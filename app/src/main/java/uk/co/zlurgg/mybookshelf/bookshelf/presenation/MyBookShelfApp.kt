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
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search.BookSearchScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search.BookSearchViewModel
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
            startDestination = NavigationRoute.BookshelfGraph
        ) {
            // TODO User starts on bookcase, option to add new shelf or edit existing -> bookshelf add books via search using api / send bookshelf -> book details
            // Can search from anywhere takes you to the book search screen for the results and can add to either existing or create new shelf from there.
            navigation<NavigationRoute.BookshelfGraph>(
                startDestination = NavigationRoute.Bookcase
            ) {
                composable<NavigationRoute.Bookcase>() {
                    val viewModel = koinViewModel<BookcaseViewModel>()

                    BookcaseScreenRoot(
                        viewModel = viewModel,
                        onBookShelfClick = { bookshelf ->
                            NavigationRoute.Bookshelf(bookshelf.id)
                        }
                    )
                }
                composable<NavigationRoute.BookSearch>() {
                    val viewModel = koinViewModel<BookSearchViewModel>()

                    BookSearchScreenRoot(
                        viewModel = viewModel,
                        onBookClick = { book ->
//                            viewModel.onSelectBook(book)
//                            navController.navigate(
//                                NavigationRoute.BookDetail(book.id)
//                            )
                        },
                    )
                }
                composable<NavigationRoute.Bookshelf>() {
                    val viewModel = koinViewModel<BookshelfViewModel>()

                    BookshelfScreenRoot(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                composable<NavigationRoute.BookDetail>() {
                    val viewModel = koinViewModel<BookDetailViewModel>()

                    BookDetailsScreenRoot(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}