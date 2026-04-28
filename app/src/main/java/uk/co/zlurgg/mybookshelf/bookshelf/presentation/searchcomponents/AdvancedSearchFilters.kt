package uk.co.zlurgg.mybookshelf.bookshelf.presentation.searchcomponents

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R

/**
 * Search filter checkboxes that allow users to narrow search scope.
 * Based on OpenLibrary API behavior:
 * - Both checked (default): Use general q= parameter
 * - Only title checked: Use title= parameter
 * - Only author checked: Use author= parameter
 * - Both unchecked: Falls back to general q= parameter
 */
@Composable
fun SearchFilters(
    searchByTitle: Boolean,
    searchByAuthor: Boolean,
    onToggleTitle: () -> Unit,
    onToggleAuthor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title checkbox
        Checkbox(
            checked = searchByTitle,
            onCheckedChange = { onToggleTitle() }
        )
        Text(
            text = stringResource(id = R.string.search_by_title),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Author checkbox
        Checkbox(
            checked = searchByAuthor,
            onCheckedChange = { onToggleAuthor() }
        )
        Text(
            text = stringResource(id = R.string.search_by_author),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchFiltersPreview() {
    SearchFilters(
        searchByTitle = true,
        searchByAuthor = true,
        onToggleTitle = {},
        onToggleAuthor = {}
    )
}
