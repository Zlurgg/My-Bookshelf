package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.components

import androidx.compose.foundation.layout.Column
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.toMaterial
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
    onAddShelf: (String, uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var selected = remember { mutableStateOf(uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle.DarkWood) }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.dialog_add_shelf_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.field_shelf_name_label)) },
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.field_shelf_style_label))
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle.entries.forEach { style ->
                        androidx.compose.material3.Card(
                            onClick = { selected.value = style },
                            border = if (selected.value == style) androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary) else null,
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = style.toMaterial().painter(),
                                contentDescription = null,
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
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
                    enabled = name.isNotBlank(),
                    onClick = { onAddShelf(name.trim(), selected.value) }
                ) {
                    Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.action_add))
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(androidx.compose.ui.res.stringResource(id = uk.co.zlurgg.mybookshelf.R.string.action_cancel))
            }
        }
    )
}