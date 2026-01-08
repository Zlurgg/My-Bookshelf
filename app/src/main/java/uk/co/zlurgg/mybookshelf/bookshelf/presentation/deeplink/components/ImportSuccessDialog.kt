package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.components.MessageDialog

/**
 * Dialog displayed when shelf import completes successfully.
 * Thin wrapper around MessageDialog with success-specific messaging.
 */
@Composable
fun ImportSuccessDialog(onDismiss: () -> Unit) {
    MessageDialog(
        title = stringResource(id = R.string.import_success_title),
        message = stringResource(id = R.string.import_success_message),
        onDismiss = onDismiss,
    )
}
