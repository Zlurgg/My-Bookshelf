package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.search_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R

@Composable
fun AdvancedSearchFilters(
    showAdvanced: Boolean,
    authorFilter: String,
    titleFilter: String,
    onToggleAdvanced: () -> Unit,
    onAuthorFilterChange: (String) -> Unit,
    onTitleFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Toggle button
        TextButton(
            onClick = onToggleAdvanced,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (showAdvanced)
                    stringResource(id = R.string.hide_advanced_search)
                else
                    stringResource(id = R.string.show_advanced_search),
                style = MaterialTheme.typography.bodySmall
            )
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
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = authorFilter,
                        onValueChange = onAuthorFilterChange,
                        label = { Text(stringResource(id = R.string.filter_by_author), style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text(stringResource(id = R.string.author_hint), style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = titleFilter,
                        onValueChange = onTitleFilterChange,
                        label = { Text(stringResource(id = R.string.filter_by_title), style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text(stringResource(id = R.string.title_hint), style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyMedium,
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
        onToggleAdvanced = {},
        onAuthorFilterChange = {},
        onTitleFilterChange = {}
    )
}