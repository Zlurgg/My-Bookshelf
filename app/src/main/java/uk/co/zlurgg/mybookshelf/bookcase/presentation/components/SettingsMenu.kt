package uk.co.zlurgg.mybookshelf.bookcase.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R

/**
 * Settings menu (3-dot overflow menu) for the Bookcase screen.
 * Contains app-wide actions like Help, About, and Join Book Club.
 */
@Composable
fun SettingsMenu(
    onShowHelp: () -> Unit,
    onShowAbout: () -> Unit,
    onShowThemeSelector: () -> Unit,
    onRateApp: () -> Unit,
    onJoinBookClub: () -> Unit,
    isSignedIn: Boolean,
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
            // Theme
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_theme)) },
                onClick = {
                    expanded = false
                    onShowThemeSelector()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = null
                    )
                }
            )

            // Rate This App
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_rate_this_app)) },
                onClick = {
                    expanded = false
                    onRateApp()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
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

            // Join Book Club (only shown when signed in)
            if (isSignedIn) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_join_book_club)) },
                    onClick = {
                        expanded = false
                        onJoinBookClub()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}
