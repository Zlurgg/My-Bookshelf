package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.search_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort

@Composable
fun AdvancedSearchFilters(
    showAdvanced: Boolean,
    authorFilter: String,
    titleFilter: String,
    selectedSort: BookSearchSort,
    onToggleAdvanced: () -> Unit,
    onAuthorFilterChange: (String) -> Unit,
    onTitleFilterChange: (String) -> Unit,
    onSortChange: (BookSearchSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Toggle button - right aligned
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onToggleAdvanced) {
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = if (showAdvanced)
                        stringResource(id = R.string.hide_advanced_search)
                    else
                        stringResource(id = R.string.show_advanced_search),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Advanced filters (animated)
        AnimatedVisibility(
            visible = showAdvanced,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(6.dp)
                ) {
                    // Sort dropdown
                    var sortExpanded by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = selectedSort.displayName,
                        onValueChange = { /* Read-only */ },
                        label = { Text(stringResource(id = R.string.sort_by), style = MaterialTheme.typography.labelSmall) },
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        trailingIcon = {
                            IconButton(onClick = { sortExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(id = R.string.sort_options),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        BookSearchSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.displayName, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    onSortChange(sort)
                                    sortExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = authorFilter,
                        onValueChange = { newValue ->
                            // UX: Prevent typing beyond 100 chars (UseCase also validates)
                            if (newValue.length <= 100) {
                                onAuthorFilterChange(newValue)
                            }
                        },
                        label = { Text(stringResource(id = R.string.filter_by_author), style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text(stringResource(id = R.string.author_hint), style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = titleFilter,
                        onValueChange = { newValue ->
                            // UX: Prevent typing beyond 200 chars (UseCase also validates)
                            if (newValue.length <= 200) {
                                onTitleFilterChange(newValue)
                            }
                        },
                        label = { Text(stringResource(id = R.string.filter_by_title), style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text(stringResource(id = R.string.title_hint), style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdvancedSearchFiltersPreview() {
    AdvancedSearchFilters(
        showAdvanced = true,
        authorFilter = "Tolkien",
        titleFilter = "Lord of the Rings",
        selectedSort = BookSearchSort.BEST_MATCH,
        onToggleAdvanced = {},
        onAuthorFilterChange = {},
        onTitleFilterChange = {},
        onSortChange = {}
    )
}