package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
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
 * Contains app-wide actions like Check for Updates, Help, About, and Sign In/Out.
 */
@Composable
fun SettingsMenu(
    isSignedIn: Boolean,
    onShowHelp: () -> Unit,
    onShowAbout: () -> Unit,
    onJoinBookClub: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
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

            // Sign In or Sign Out based on auth state
            if (isSignedIn) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_sign_out)) },
                    onClick = {
                        expanded = false
                        onSignOut()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null
                        )
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_sign_in)) },
                    onClick = {
                        expanded = false
                        onSignIn()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}
