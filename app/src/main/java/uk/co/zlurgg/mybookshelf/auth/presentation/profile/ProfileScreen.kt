package uk.co.zlurgg.mybookshelf.auth.presentation.profile

import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.auth.data.service.GoogleCredentialFetcher
import uk.co.zlurgg.mybookshelf.auth.presentation.profile.components.DeleteAccountConfirmDialog
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun ProfileScreenRoot(
    viewModel: ProfileViewModel = koinViewModel(),
    credentialFetcher: GoogleCredentialFetcher = koinInject(),
    onNavigateToSignIn: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current

    LaunchedEffect(state.navigateToSignIn) {
        if (state.navigateToSignIn) {
            onNavigateToSignIn()
            viewModel.onAction(ProfileAction.ResetNavigation)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(ProfileAction.DismissError)
        }
    }

    ProfileScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onReAuthFetchCredential = {
            val currentActivity = activity
                ?: return@ProfileScreen
            val result = credentialFetcher.fetch(currentActivity)
            if (result is Result.Success) {
                viewModel.onAction(ProfileAction.OnReAuthCompleted(result.data))
            } else if (result is Result.Error) {
                viewModel.onAction(ProfileAction.DismissReAuth)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileState,
    snackbarHostState: SnackbarHostState,
    onAction: (ProfileAction) -> Unit,
    onBack: () -> Unit,
    onReAuthFetchCredential: suspend () -> Unit = {},
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
                onClick = { onAction(ProfileAction.ShowSignOutDialog) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_sign_out))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Delete account button
            Button(
                onClick = { onAction(ProfileAction.RequestDeleteAccount) },
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
        }
    }

    // Sign out confirmation dialog
    if (state.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { onAction(ProfileAction.DismissSignOutDialog) },
            title = { Text(stringResource(R.string.sign_out_title)) },
            text = { Text(stringResource(R.string.sign_out_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(ProfileAction.ConfirmSignOut) }) {
                    Text(stringResource(R.string.sign_out_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ProfileAction.DismissSignOutDialog) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Delete account confirmation dialog
    if (state.showDeleteConfirmDialog) {
        DeleteAccountConfirmDialog(
            onConfirm = { onAction(ProfileAction.ConfirmDeleteAccount) },
            onDismiss = { onAction(ProfileAction.DismissDeleteConfirm) },
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

    // Re-auth dialog — auto-triggers credential fetch when shown
    if (state.showReAuthDialog) {
        AlertDialog(
            onDismissRequest = { onAction(ProfileAction.DismissReAuth) },
            title = { Text(stringResource(R.string.profile_reauth_title)) },
            text = { Text(stringResource(R.string.profile_reauth_message)) },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { onAction(ProfileAction.DismissReAuth) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )

        LaunchedEffect(Unit) {
            onReAuthFetchCredential()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    MyBookshelfTheme {
        ProfileScreen(
            state = ProfileState(
                userName = "John Doe",
                userEmail = "john@example.com",
                isSignedIn = true,
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBack = {},
        )
    }
}
