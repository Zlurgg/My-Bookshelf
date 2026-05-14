package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailUiConstants

/**
 * Card for toggling purchased status.
 * Shows: Switch to mark book as owned/unowned.
 */
@Composable
fun PurchasedToggleCard(
    purchased: Boolean,
    onPurchaseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = BookDetailUiConstants.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BookDetailUiConstants.CardContentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (purchased) {
                    stringResource(R.string.book_owned)
                } else {
                    stringResource(R.string.book_unowned)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(BookDetailUiConstants.SmallSpacing))

            Switch(
                checked = purchased,
                onCheckedChange = { onPurchaseToggle() }
            )
        }
    }
}
