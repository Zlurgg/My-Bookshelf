package uk.co.zlurgg.mybookshelf.account.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.account.presentation.components.DeleteAccountConfirmDialog
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    state: AccountState,
    snackbarHostState: SnackbarHostState,
    onAction: (AccountAction) -> Unit,
    onSendFeedback: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Profile avatar
            if (state.profilePictureUrl != null) {
                AsyncImage(
                    model = state.profilePictureUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User name
            state.userName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            // User email
            state.userEmail?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign out button
            OutlinedButton(
                onClick = { onAction(AccountAction.ShowSignOutDialog) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_sign_out))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Send feedback button
            OutlinedButton(
                onClick = onSendFeedback,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_send_feedback))
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Delete account button
            Button(
                onClick = { onAction(AccountAction.RequestDeleteAccount) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.profile_delete_account))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Sign out confirmation dialog
    if (state.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { onAction(AccountAction.DismissSignOutDialog) },
            title = { Text(stringResource(R.string.sign_out_title)) },
            text = { Text(stringResource(R.string.sign_out_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(AccountAction.ConfirmSignOut) }) {
                    Text(stringResource(R.string.sign_out_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AccountAction.DismissSignOutDialog) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Delete account confirmation dialog
    if (state.showDeleteConfirmDialog) {
        DeleteAccountConfirmDialog(
            onConfirm = { onAction(AccountAction.ConfirmDeleteAccount) },
            onDismiss = { onAction(AccountAction.DismissDeleteConfirm) },
        )
    }

    // Deleting progress dialog
    if (state.isDeleting) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible */ },
            confirmButton = { },
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.profile_deleting),
                        textAlign = TextAlign.Center,
                    )
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenPreview() {
    MyBookshelfTheme {
        AccountScreen(
            state = AccountState(
                userName = "John Doe",
                userEmail = "john@example.com",
                isSignedIn = true,
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onSendFeedback = {},
            onBack = {},
        )
    }
}
