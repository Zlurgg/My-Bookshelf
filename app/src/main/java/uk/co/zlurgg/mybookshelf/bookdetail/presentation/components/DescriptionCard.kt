package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailUiConstants

private const val COLLAPSE_THRESHOLD = 150
private const val MAX_LINES_COLLAPSED = 3

@Composable
fun DescriptionCard(
    description: String?,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    outlined: Boolean = false
) {
    if (description.isNullOrBlank()) return

    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .padding(BookDetailUiConstants.CardContentPadding)
                .animateContentSize()
        ) {
            Text(
                text = stringResource(R.string.description_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(BookDetailUiConstants.SectionSpacing))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else MAX_LINES_COLLAPSED,
                overflow = TextOverflow.Ellipsis
            )

            if (description.length > COLLAPSE_THRESHOLD) {
                Spacer(modifier = Modifier.height(BookDetailUiConstants.SmallSpacing))

                Text(
                    text = if (isExpanded) {
                        stringResource(R.string.description_show_less)
                    } else {
                        stringResource(R.string.description_show_more)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }
        }
    }

    if (outlined) {
        OutlinedCard(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            cardContent()
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = BookDetailUiConstants.CardElevation)
        ) {
            cardContent()
        }
    }
}
