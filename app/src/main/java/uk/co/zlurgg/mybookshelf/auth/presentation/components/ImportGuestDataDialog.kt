package uk.co.zlurgg.mybookshelf.auth.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.sync.domain.model.GuestDataInfo

/**
 * Dialog asking the user if they want to import their guest data
 * after signing in with a Google account.
 *
 * @param guestDataInfo Information about the guest data to import
 * @param onImport Callback when user chooses to import
 * @param onSkip Callback when user chooses to skip
 */
@Composable
fun ImportGuestDataDialog(
    guestDataInfo: GuestDataInfo,
    onImport: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = {
            Text(
                text = stringResource(R.string.import_guest_data_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.import_guest_data_message),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show counts of books and shelves
                if (guestDataInfo.bookCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.guest_data_books,
                            guestDataInfo.bookCount,
                            guestDataInfo.bookCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (guestDataInfo.shelfCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.guest_data_shelves,
                            guestDataInfo.shelfCount,
                            guestDataInfo.shelfCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.import_guest_data_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onImport) {
                Text(stringResource(R.string.action_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.action_skip))
            }
        }
    )
}
