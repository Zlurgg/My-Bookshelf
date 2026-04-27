package uk.co.zlurgg.mybookshelf.book.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R

/**
 * Reusable dialog component for shelf name input operations (rename, import conflict resolution).
 * Displays inline error messages within the dialog to ensure visibility.
 *
 * @param currentName The initial name value (empty for new shelf, populated for rename/conflict)
 * @param errorMessage Optional error message to display inline below the TextField
 * @param isLoading Whether an operation is in progress (disables interaction)
 * @param titleRes String resource ID for dialog title
 * @param confirmTextRes String resource ID for confirm button text
 * @param prefixMessage Optional message to display above the TextField (for context/explanations)
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when user confirms with trimmed name
 */
@Composable
fun ShelfNameInputDialog(
    currentName: String,
    errorMessage: String?,
    isLoading: Boolean,
    titleRes: Int,
    confirmTextRes: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    prefixMessage: String? = null
) {
    var name by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text(stringResource(id = titleRes)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Optional prefix message (e.g., for conflict explanations)
                if (prefixMessage != null) {
                    Text(
                        text = prefixMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.field_shelf_name_label)) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    isError = errorMessage != null
                )

                // Inline error message - visible within dialog
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    enabled = name.isNotBlank() && name.trim() != currentName.trim(),
                    onClick = { onConfirm(name.trim()) }
                ) {
                    Text(stringResource(id = confirmTextRes))
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
