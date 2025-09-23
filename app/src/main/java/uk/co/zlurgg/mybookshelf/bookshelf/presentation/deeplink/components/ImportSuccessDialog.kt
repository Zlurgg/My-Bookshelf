package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R

@Composable
fun ImportSuccessDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.import_success_title)) },
        text = { Text(stringResource(id = R.string.import_success_message)) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_ok))
            }
        }
    )
}