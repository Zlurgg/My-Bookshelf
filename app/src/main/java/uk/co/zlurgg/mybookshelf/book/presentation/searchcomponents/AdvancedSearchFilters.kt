package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R

/**
 * Search filter checkboxes and safe search toggle.
 *
 * Row 1: Title / Author / Subject checkboxes — at least one must remain checked.
 * Row 2: Safe Search switch — visually distinct as a content filter vs. search mode.
 */
@Composable
fun SearchFilters(
    searchByTitle: Boolean,
    searchByAuthor: Boolean,
    searchBySubject: Boolean,
    titleEnabled: Boolean,
    authorEnabled: Boolean,
    subjectEnabled: Boolean,
    safeSearchEnabled: Boolean,
    onToggleTitle: () -> Unit,
    onToggleAuthor: () -> Unit,
    onToggleSubject: () -> Unit,
    onToggleSafeSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = searchByTitle,
                enabled = titleEnabled,
                onCheckedChange = { onToggleTitle() }
            )
            Text(
                text = stringResource(id = R.string.search_by_title),
                style = MaterialTheme.typography.bodySmall
            )

            Checkbox(
                checked = searchByAuthor,
                enabled = authorEnabled,
                onCheckedChange = { onToggleAuthor() }
            )
            Text(
                text = stringResource(id = R.string.search_by_author),
                style = MaterialTheme.typography.bodySmall
            )

            Checkbox(
                checked = searchBySubject,
                enabled = subjectEnabled,
                onCheckedChange = { onToggleSubject() }
            )
            Text(
                text = stringResource(id = R.string.search_by_subject),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                // Align the row with the visible left edge of the checkboxes in
                // row 1. Material 3 Checkbox centers its glyph within a 40dp
                // touch surface, so the visible glyph starts ~12dp from the row.
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.safe_search_label),
                style = MaterialTheme.typography.bodySmall
            )
            Switch(
                checked = safeSearchEnabled,
                onCheckedChange = { onToggleSafeSearch() },
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchFiltersPreview() {
    SearchFilters(
        searchByTitle = true,
        searchByAuthor = true,
        searchBySubject = false,
        titleEnabled = true,
        authorEnabled = true,
        subjectEnabled = true,
        safeSearchEnabled = true,
        onToggleTitle = {},
        onToggleAuthor = {},
        onToggleSubject = {},
        onToggleSafeSearch = {}
    )
}
