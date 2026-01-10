package uk.co.zlurgg.mybookshelf.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.auth.presentation.components.ContinueAsGuestButton
import uk.co.zlurgg.mybookshelf.auth.presentation.components.DevSignInButton
import uk.co.zlurgg.mybookshelf.auth.presentation.components.ImportGuestDataDialog
import uk.co.zlurgg.mybookshelf.auth.presentation.components.SignInButton
import uk.co.zlurgg.mybookshelf.auth.presentation.components.WelcomeHeader

@Composable
fun SignInScreenRoot(
    viewModel: SignInViewModel = koinViewModel(),
    onNavigate: (PostSignInDestination) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation when destination is determined
    LaunchedEffect(state.navigateToDestination) {
        state.navigateToDestination?.let { destination ->
            onNavigate(destination)
            viewModel.onAction(SignInAction.ResetState)
        }
    }

    // Show error snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // Show guest data import dialog if needed
    state.guestDataInfo?.let { guestDataInfo ->
        if (state.showGuestDataImportDialog) {
            ImportGuestDataDialog(
                guestDataInfo = guestDataInfo,
                onImport = { viewModel.onAction(SignInAction.ImportGuestData) },
                onSkip = { viewModel.onAction(SignInAction.SkipGuestDataImport) }
            )
        }
    }

    SignInScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSignInClick = { viewModel.onAction(SignInAction.SignIn) },
        onDevSignInClick = { userNumber -> viewModel.onAction(SignInAction.DevSignIn(userNumber)) },
        onContinueAsGuestClick = { viewModel.onAction(SignInAction.ContinueAsGuest) }
    )
}

@Composable
private fun SignInScreen(
    state: SignInState,
    snackbarHostState: SnackbarHostState,
    onSignInClick: () -> Unit,
    onDevSignInClick: (userNumber: Int) -> Unit,
    onContinueAsGuestClick: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WelcomeHeader()

            Spacer(modifier = Modifier.height(64.dp))

            // Google sign-in (release builds only - doesn't work with emulator)
            if (!BuildConfig.DEBUG) {
                SignInButton(
                    onClick = onSignInClick,
                    isLoading = state.isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dev sign-in button (debug builds only)
            DevSignInButton(
                onClick = onDevSignInClick,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            ContinueAsGuestButton(
                onClick = onContinueAsGuestClick,
                enabled = !state.isLoading
            )
        }
    }
}
