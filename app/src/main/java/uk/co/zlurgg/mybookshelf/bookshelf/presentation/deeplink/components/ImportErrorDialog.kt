package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.presentation.components.MessageDialog

/**
 * Dialog displayed when shelf import fails with an error.
 * Thin wrapper around MessageDialog with error-specific messaging.
 */
@Composable
fun ImportErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    MessageDialog(
        title = stringResource(id = R.string.import_error_title),
        message = errorMessage,
        onDismiss = onDismiss
    )
}
