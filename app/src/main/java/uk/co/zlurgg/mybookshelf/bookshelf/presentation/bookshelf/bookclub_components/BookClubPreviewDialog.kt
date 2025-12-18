package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookclub_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

/**
 * Dialog showing a preview of a book club before joining.
 * Displays club name, style, book count, and member count.
 */
@Composable
fun BookClubPreviewDialog(
    bookClub: BookClub,
    onDismiss: () -> Unit,
    onJoin: () -> Unit,
    isJoining: Boolean = false,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {
            if (!isJoining) onDismiss()
        },
        title = {
            Text(
                text = stringResource(R.string.book_club_preview_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shelf style preview
                ShelfStylePreview(
                    style = bookClub.style,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Club name
                Text(
                    text = bookClub.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Created by
                Text(
                    text = "Created by ${bookClub.createdByName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Book count
                    StatItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = stringResource(R.string.book_club_preview_books, bookClub.bookCount)
                    )

                    // Member count
                    StatItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = stringResource(R.string.book_club_preview_members, bookClub.memberCount)
                    )
                }
            }
        },
        confirmButton = {
            if (isJoining) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.book_club_preview_joining))
                }
            } else {
                Button(onClick = onJoin) {
                    Text(stringResource(R.string.book_club_preview_join))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isJoining
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    )
}

@Composable
private fun StatItem(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ShelfStylePreview(
    style: ShelfStyle,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (style) {
        ShelfStyle.DarkWood -> Color(0xFF5D4E37)
        ShelfStyle.SilverMetal -> Color(0xFFC0C0C0)
        ShelfStyle.WhiteMetal -> Color(0xFFE8E8E8)
        ShelfStyle.GreyMetal -> Color(0xFF808080)
        ShelfStyle.DarkGreyMetal -> Color(0xFF404040)
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = style.name.replace("([A-Z])".toRegex(), " $1").trim(),
            style = MaterialTheme.typography.labelMedium,
            color = if (style == ShelfStyle.DarkWood || style == ShelfStyle.DarkGreyMetal) {
                Color.White.copy(alpha = 0.8f)
            } else {
                Color.Black.copy(alpha = 0.6f)
            }
        )
    }
}
