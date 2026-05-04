package uk.co.zlurgg.mybookshelf.core.presentation.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.core.domain.model.ThemeMode
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun ThemeSelectorDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_theme)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == currentTheme,
                                onClick = { onThemeSelected(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentTheme,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ThemeSelectorDialogPreview() {
    MyBookshelfTheme {
        ThemeSelectorDialog(
            currentTheme = ThemeMode.SYSTEM,
            onThemeSelected = {},
            onDismiss = {}
        )
    }
}
