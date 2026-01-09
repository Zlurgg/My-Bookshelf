package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.ShelfMaterial

@Composable
fun ChangeStyleDialog(
    currentStyle: ShelfStyle,
    onDismiss: () -> Unit,
    onChangeStyle: (ShelfStyle) -> Unit,
    isLoading: Boolean = false
) {
    var selectedStyle by remember { mutableStateOf(currentStyle) }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text(stringResource(id = R.string.dialog_change_style_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.dialog_change_style_message))
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ShelfStyle.entries.size) { index ->
                        val style = ShelfStyle.entries[index]
                        Card(
                            onClick = { selectedStyle = style },
                            border = if (selectedStyle == style) {
                                BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            } else {
                                null
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Image(
                                painter = ShelfMaterial.fromShelfStyle(style).painterSmall(),
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
                    enabled = selectedStyle != currentStyle,
                    onClick = { onChangeStyle(selectedStyle) }
                ) {
                    Text(stringResource(id = R.string.action_change))
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
