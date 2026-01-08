package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.components.ShelfNameInputDialog

/**
 * Dialog displayed when importing a shelf with a name that already exists.
 * Thin wrapper around ShelfNameInputDialog with conflict-specific messaging.
 */
@Composable
fun ImportNameConflictDialog(
    existingName: String,
    isLoading: Boolean,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onResolveConflict: (String) -> Unit,
) {
    ShelfNameInputDialog(
        currentName = existingName,
        errorMessage = errorMessage,
        isLoading = isLoading,
        titleRes = R.string.import_name_conflict_title,
        confirmTextRes = R.string.action_import,
        prefixMessage =
            stringResource(
                id = R.string.import_name_conflict_message,
                existingName,
            ),
        onDismiss = onDismiss,
        onConfirm = onResolveConflict,
    )
}
