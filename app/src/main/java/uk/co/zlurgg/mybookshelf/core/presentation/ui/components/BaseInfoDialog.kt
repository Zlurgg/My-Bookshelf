package uk.co.zlurgg.mybookshelf.core.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable base dialog for displaying informational content.
 * Used by AboutDialog, HelpDialog, etc.
 *
 * @param title Dialog title
 * @param buttonText Button text (usually "OK" or "Got it")
 * @param onDismiss Callback when dialog is dismissed
 * @param scrollable Whether the content should be scrollable
 * @param content Composable content to display
 */
@Composable
fun BaseInfoDialog(
    title: String,
    buttonText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (scrollable) Modifier.verticalScroll(rememberScrollState())
                        else Modifier
                    ),
                content = content
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = buttonText,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.padding(16.dp)
    )
}

/**
 * Reusable section component for dialogs.
 * Displays a title and content with consistent styling.
 */
@Composable
fun DialogContentSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
