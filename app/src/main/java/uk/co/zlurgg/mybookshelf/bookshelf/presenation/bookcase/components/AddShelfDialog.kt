package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.components

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
import androidx.compose.ui.unit.dp


@Composable
fun AddShelfDialog(
    onDismiss: () -> Unit,
    onAddShelf: (String) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            // Only allow dismiss if not loading
            if (!isLoading) onDismiss()
        },
        title = { Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.dialog_add_shelf_title)) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.field_shelf_name_label)) },
                    enabled = !isLoading // Disable during loading
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
                    enabled = name.isNotBlank(),
                    onClick = {
                        onAddShelf(name)
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.action_add))
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isLoading // Disable during loading
            ) {
                Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.action_cancel))
            }
        }
    )
}