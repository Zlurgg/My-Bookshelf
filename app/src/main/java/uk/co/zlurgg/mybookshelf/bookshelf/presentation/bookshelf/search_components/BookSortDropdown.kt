package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.search_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
fun BookSortDropdown(
    selectedSort: BookSearchSort,
    showSort: Boolean,
    onSortChange: (BookSearchSort) -> Unit,
    onToggleSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Toggle button
        TextButton(
            onClick = onToggleSort,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (showSort) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (showSort)
                    stringResource(id = R.string.hide_sort_options)
                else
                    stringResource(id = R.string.show_sort_options),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Sort dropdown (animated)
        AnimatedVisibility(
            visible = showSort,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            OutlinedTextField(
                value = selectedSort.displayName,
                onValueChange = { /* Read-only */ },
                label = { Text(stringResource(id = R.string.sort_by), style = MaterialTheme.typography.bodySmall) },
                readOnly = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
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
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                BookSearchSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.displayName) },
                        onClick = {
                            onSortChange(sort)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookSortDropdownPreview() {
    BookSortDropdown(
        selectedSort = BookSearchSort.BEST_MATCH,
        showSort = true,
        onSortChange = {},
        onToggleSort = {}
    )
}