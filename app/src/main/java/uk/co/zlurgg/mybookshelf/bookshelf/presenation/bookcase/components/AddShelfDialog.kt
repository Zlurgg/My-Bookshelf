package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle


@Composable
fun AddShelfDialog(
    onDismiss: () -> Unit,
    onAddShelf: (String, ShelfStyle) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var selected = remember { mutableStateOf(ShelfStyle.DarkWood) }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text(stringResource(id = R.string.dialog_add_shelf_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.field_shelf_name_label)) },
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(id = R.string.field_shelf_style_label))
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ShelfStyle.entries.size) { index ->
                        val style = ShelfStyle.entries[index]
                        Card(
                            onClick = { selected.value = style },
                            border = if (selected.value == style) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Image(
                                painter = style.toMaterial().painter(),
                                contentDescription = style.name,
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
                    Text(stringResource(id = R.string.action_add))
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