package uk.co.zlurgg.mybookshelf.bookshelf.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R

/**
 * Generic dialog component for displaying simple messages (success, error, info).
 * Provides consistent UX for all message-type dialogs across the app.
 *
 * @param title Dialog title text
 * @param message Dialog message text
 * @param onDismiss Callback when dialog is dismissed via button or back press
 */
@Composable
fun MessageDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_ok))
            }
        },
    )
}
