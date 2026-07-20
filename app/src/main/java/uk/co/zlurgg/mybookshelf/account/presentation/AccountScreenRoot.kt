package uk.co.zlurgg.mybookshelf.account.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.auth.presentation.service.CredentialFetcher
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

private const val FEEDBACK_EMAIL = "zlurgg.marq@gmail.com"

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noEmailClientMessage = stringResource(R.string.feedback_no_email_client)

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
        onSendFeedback = {
            try {
                context.startActivity(buildFeedbackIntent(context))
            } catch (_: ActivityNotFoundException) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        noEmailClientMessage,
                    )
                }
            }
        },
        onBack = onBack,
    )
}

private fun buildFeedbackIntent(context: Context): Intent {
    val subject = context.getString(
        R.string.feedback_email_subject,
        BuildConfig.VERSION_NAME,
    )
    val body = context.getString(
        R.string.feedback_email_body,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT,
        Build.MANUFACTURER,
        Build.MODEL,
    )
    return Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$FEEDBACK_EMAIL")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
}
