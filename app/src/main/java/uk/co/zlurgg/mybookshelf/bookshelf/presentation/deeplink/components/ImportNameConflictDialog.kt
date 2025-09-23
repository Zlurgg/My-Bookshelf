package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R

@Composable
fun ImportNameConflictDialog(
    existingName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onResolveConflict: (String) -> Unit
) {
    var newName by remember { mutableStateOf("$existingName (Copy)") }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text(stringResource(id = R.string.import_name_conflict_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        id = R.string.import_name_conflict_message,
                        existingName
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(id = R.string.import_new_name_label)) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
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
                    enabled = newName.isNotBlank(),
                    onClick = { onResolveConflict(newName.trim()) }
                ) {
                    Text(stringResource(id = R.string.action_import))
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
}