package uk.co.zlurgg.mybookshelf.account.presentation

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import uk.co.zlurgg.mybookshelf.auth.presentation.service.CredentialFetcher
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

@Composable
fun AccountScreenRoot(
    viewModel: AccountViewModel = koinViewModel(),
    credentialFetcher: CredentialFetcher = koinInject(),
    onNavigateToSignIn: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current

    // One-shot: navigate to sign-in
    LaunchedEffect(state.navigateToSignIn) {
        if (state.navigateToSignIn) {
            onNavigateToSignIn()
            viewModel.onAction(AccountAction.ResetNavigation)
        }
    }

    // One-shot: show error snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(AccountAction.DismissError)
        }
    }

    // One-shot: re-auth credential fetch
    LaunchedEffect(state.requestReAuth) {
        if (state.requestReAuth) {
            val currentActivity = activity ?: run {
                viewModel.onAction(AccountAction.OnReAuthFailed)
                viewModel.onAction(AccountAction.ResetReAuth)
                return@LaunchedEffect
            }
            when (val result = credentialFetcher.fetch(currentActivity)) {
                is Result.Success -> viewModel.onAction(AccountAction.OnReAuthCompleted(result.data))
                is Result.Error -> viewModel.onAction(AccountAction.OnReAuthFailed)
            }
            viewModel.onAction(AccountAction.ResetReAuth)
        }
    }

    AccountScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}
