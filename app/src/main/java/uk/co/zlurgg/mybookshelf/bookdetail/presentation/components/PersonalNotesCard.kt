package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailUiConstants

private val NotesMaxHeight = 240.dp
private const val NOTES_MAX_CHARS = 5000
private const val NOTES_MIN_LINES = 4
private const val NOTES_MAX_LINES = 10

@Composable
fun PersonalNotesCard(
    notes: String, // "" = no notes
    onNotesChange: (String) -> Unit, // Pass "" to clear notes
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = BookDetailUiConstants.CardElevation)
    ) {
        Column(
            modifier = Modifier.padding(BookDetailUiConstants.CardContentPadding)
        ) {
            Text(
                text = stringResource(R.string.personal_notes_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(BookDetailUiConstants.SectionSpacing))

            OutlinedTextField(
                value = notes,
                onValueChange = { newNotes ->
                    if (newNotes.length <= NOTES_MAX_CHARS) {
                        // Call action immediately - ViewModel handles debouncing
                        onNotesChange(newNotes)
                    }
                },
                label = { Text(stringResource(R.string.personal_notes_label)) },
                placeholder = { Text(stringResource(R.string.personal_notes_placeholder)) },
                supportingText = {
                    val currentLength = notes.length
                    Text(stringResource(R.string.personal_notes_character_count, currentLength))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { /* Keyboard dismisses automatically */ }
                ),
                minLines = NOTES_MIN_LINES,
                maxLines = NOTES_MAX_LINES,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = NotesMaxHeight)
            )
        }
    }
}
