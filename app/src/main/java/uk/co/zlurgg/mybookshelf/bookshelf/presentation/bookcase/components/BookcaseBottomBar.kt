package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseTab

@Composable
fun BookcaseBottomBar(
    selectedTab: BookcaseTab,
    onTabSelected: (BookcaseTab) -> Unit,
) {
    NavigationBar {
        BookcaseTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (tab == selectedTab) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelResId),
                    )
                },
                label = { Text(stringResource(tab.labelResId)) },
            )
        }
    }
}
