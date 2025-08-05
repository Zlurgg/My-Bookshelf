package uk.co.zlurgg.mybookshelf.bookshelf.presenation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.compose.viewmodel.koinViewModel
import uk.co.zlurgg.mybookshelf.app.NavigationRoute
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfScreenRoot
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfViewModel
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun MyBookShelfApp() {
    MyBookshelfTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = NavigationRoute.Bookshelf
        ) {
            composable<NavigationRoute.Bookshelf>() {
                val viewModel = koinViewModel<BookshelfViewModel>()

                BookshelfScreenRoot(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}