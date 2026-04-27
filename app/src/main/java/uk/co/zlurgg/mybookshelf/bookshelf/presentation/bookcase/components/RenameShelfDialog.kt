package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.runtime.Composable
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.presentation.components.ShelfNameInputDialog

/**
 * Thin wrapper around ShelfNameInputDialog for shelf renaming.
 * Displays inline error messages for better visibility.
 */
@Composable
fun RenameShelfDialog(
    currentName: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    ShelfNameInputDialog(
        currentName = currentName,
        errorMessage = errorMessage,
        isLoading = false,
        titleRes = R.string.dialog_rename_shelf_title,
        confirmTextRes = R.string.action_rename,
        onDismiss = onDismiss,
        onConfirm = onRename
    )
}
