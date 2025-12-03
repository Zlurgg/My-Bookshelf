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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.auth.presentation.components.SignInButton
import uk.co.zlurgg.mybookshelf.auth.presentation.components.WelcomeHeader

@Composable
fun SignInScreenRoot(
    viewModel: SignInViewModel = koinViewModel(),
    onSignInSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle navigation on successful sign-in
    LaunchedEffect(state.isSignInSuccessful) {
        if (state.isSignInSuccessful) {
            onSignInSuccess()
            viewModel.onAction(SignInAction.ResetState)
        }
    }

    // Show error snackbar
    LaunchedEffect(state.signInError) {
        state.signInError?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    SignInScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSignInClick = { viewModel.onAction(SignInAction.SignIn(context)) }
    )
}

@Composable
private fun SignInScreen(
    state: SignInState,
    snackbarHostState: SnackbarHostState,
    onSignInClick: () -> Unit
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

            SignInButton(
                onClick = onSignInClick,
                isLoading = state.isLoading
            )
        }
    }
}
