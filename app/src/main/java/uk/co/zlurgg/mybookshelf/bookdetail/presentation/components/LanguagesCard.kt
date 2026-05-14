package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailUiConstants

/**
 * Card displaying languages the book is available in.
 * Shows languages as chips in a flow layout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguagesCard(
    languages: List<String>,
    modifier: Modifier = Modifier
) {
    if (languages.isEmpty()) return

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(BookDetailUiConstants.CardContentPadding)
        ) {
            Text(
                text = stringResource(R.string.languages_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(BookDetailUiConstants.SectionSpacing))

            FlowRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                languages.forEach { language ->
                    AssistChip(
                        onClick = { /* No action */ },
                        label = { Text(language.uppercase()) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}
