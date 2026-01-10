package uk.co.zlurgg.mybookshelf.auth.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Release build stub - renders nothing.
 * In debug builds, this is replaced by the debug source set version
 * which shows test user sign-in buttons.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DevSignInButton(
    onClick: (userNumber: Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // No-op in release builds - renders nothing
}
