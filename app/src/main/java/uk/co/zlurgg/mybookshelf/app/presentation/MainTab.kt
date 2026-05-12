package uk.co.zlurgg.mybookshelf.app.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.app.NavigationRoute

enum class MainTab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelResId: Int,
    val route: String
) {
    MY_SHELVES(
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        labelResId = R.string.tab_my_shelves,
        route = NavigationRoute.Bookcase.ROUTE
    ),
    BOOK_CLUBS(
        selectedIcon = Icons.Filled.Groups,
        unselectedIcon = Icons.Outlined.Groups,
        labelResId = R.string.tab_book_clubs,
        route = NavigationRoute.BookClubs.ROUTE
    ),
    LIBRARY(
        selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
        unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
        labelResId = R.string.tab_library,
        route = NavigationRoute.Library.ROUTE
    )
}
