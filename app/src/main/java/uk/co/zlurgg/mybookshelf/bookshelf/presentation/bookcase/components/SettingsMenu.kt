package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import uk.co.zlurgg.mybookshelf.R

/**
 * Settings menu (3-dot overflow menu) for the Bookcase screen.
 * Contains app-wide actions like Check for Updates, Help, and About.
 */
@Composable
fun SettingsMenu(
    isReorderMode: Boolean,
    hasShelvesToReorder: Boolean,
    onToggleReorderMode: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onShowHelp: () -> Unit,
    onShowAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.settings_menu)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            // Reorder Shelves - only shown when shelves exist
            if (hasShelvesToReorder) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isReorderMode)
                                stringResource(R.string.menu_lock_shelves)
                            else
                                stringResource(R.string.menu_reorder_shelves)
                        )
                    },
                    onClick = {
                        expanded = false
                        onToggleReorderMode()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null
                        )
                    }
                )
            }

            // Check for Updates
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_check_for_updates)) },
                onClick = {
                    expanded = false
                    onCheckForUpdates()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null
                    )
                }
            )

            // Help & Tutorial
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_help)) },
                onClick = {
                    expanded = false
                    onShowHelp()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null
                    )
                }
            )

            // About
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_about)) },
                onClick = {
                    expanded = false
                    onShowAbout()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                }
            )
        }
    }
}
