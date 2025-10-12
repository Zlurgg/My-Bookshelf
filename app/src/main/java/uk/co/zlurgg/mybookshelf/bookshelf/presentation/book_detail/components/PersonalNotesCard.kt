package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Card for personal notes about the book.
 * Allows up to 5000 characters of personal notes.
 * This data is NOT exported for privacy.
 */
@Composable
fun PersonalNotesCard(
    notes: String?,
    onNotesChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "My Notes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes ?: "",
                onValueChange = { newNotes ->
                    if (newNotes.length <= 5000) {
                        // Always send the value (even empty string) so UseCase can distinguish
                        // between "no update" and "clear field"
                        onNotesChange(newNotes.trim().ifEmpty { "" })
                    }
                },
                label = { Text("Personal notes (private)") },
                placeholder = { Text("Add your thoughts about this book...") },
                supportingText = {
                    val currentLength = notes?.length ?: 0
                    Text("$currentLength / 5000 characters")
                },
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
