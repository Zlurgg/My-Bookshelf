package uk.co.zlurgg.mybookshelf.core.presentation.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

/**
 * About dialog showing app information, version, and credits.
 */
@Composable
fun AboutDialog(
    versionName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaseInfoDialog(
        title = stringResource(R.string.about_title),
        buttonText = stringResource(R.string.action_ok),
        onDismiss = onDismiss,
        scrollable = true,
        modifier = modifier
    ) {
        DialogContentSection(
            title = stringResource(R.string.about_version_title),
            content = versionName
        )

        Spacer(modifier = Modifier.height(16.dp))

        DialogContentSection(
            title = stringResource(R.string.about_description_title),
            content = stringResource(R.string.about_description_content)
        )

        Spacer(modifier = Modifier.height(16.dp))

        DialogContentSection(
            title = stringResource(R.string.about_privacy_title),
            content = stringResource(R.string.about_privacy_content)
        )

        Spacer(modifier = Modifier.height(16.dp))

        DialogContentSection(
            title = stringResource(R.string.about_open_source_title),
            content = stringResource(R.string.about_open_source_content)
        )

        Spacer(modifier = Modifier.height(16.dp))

        DialogContentSection(
            title = stringResource(R.string.about_credits_title),
            content = stringResource(R.string.about_credits_content)
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AboutDialogPreview() {
    MyBookshelfTheme {
        AboutDialog(
            versionName = "1.0.5",
            onDismiss = {}
        )
    }
}
