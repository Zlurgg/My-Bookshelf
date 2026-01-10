package uk.co.zlurgg.mybookshelf.auth.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.auth.data.service.DevAuthService

// Dev-only UI constants - not extracted to theme since this component only exists in debug builds
private val DevOrange = Color(0xFFCC5500)
private val IconSize: Dp = 18.dp
private val SpacingSmall: Dp = 8.dp
private val SpacingMedium: Dp = 12.dp
private val BorderWidth: Dp = 1.dp
private const val CARD_BACKGROUND_ALPHA = 0.1f

/**
 * Development-only sign-in section for use with Firebase Auth Emulator.
 * Shows buttons for multiple test users to enable multi-user testing.
 * This component only exists in debug builds.
 */
@Composable
fun DevSignInButton(
    onClick: (userNumber: Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!expanded) {
            // Collapsed: Show single button to expand
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DevOrange),
                border = BorderStroke(BorderWidth, DevOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize)
                )
                Spacer(modifier = Modifier.width(SpacingSmall))
                Text(
                    text = "Dev Sign In (Emulator)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            // Expanded: Show user selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DevOrange.copy(alpha = CARD_BACKGROUND_ALPHA)
                ),
                border = BorderStroke(BorderWidth, DevOrange)
            ) {
                Column(
                    modifier = Modifier.padding(SpacingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = DevOrange,
                            modifier = Modifier.size(IconSize)
                        )
                        Spacer(modifier = Modifier.width(SpacingSmall))
                        Text(
                            text = "Select Test User",
                            style = MaterialTheme.typography.titleSmall,
                            color = DevOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(SpacingMedium))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
                    ) {
                        DevAuthService.TEST_USERS.forEach { testUser ->
                            OutlinedButton(
                                onClick = {
                                    expanded = false
                                    onClick(testUser.number)
                                },
                                enabled = enabled,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DevOrange),
                                border = BorderStroke(BorderWidth, DevOrange)
                            ) {
                                Text(
                                    text = testUser.displayName.substringBefore(" "),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
