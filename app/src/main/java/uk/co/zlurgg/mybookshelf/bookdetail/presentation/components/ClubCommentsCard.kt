package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.BookComment

/**
 * Card showing all club comments in a messaging-style conversation thread.
 * Features:
 * - Scrollable list with max height (shows ~3-4 comments)
 * - Oldest comments first, auto-scrolls to show latest
 * - Own comments aligned right with primary background
 * - Other comments aligned left with surface variant background
 * - Long-press on own comment shows dropdown menu with Edit/Delete
 * - Inline editing mode
 */
@Composable
fun ClubCommentsCard(
    comments: List<BookComment>,
    currentUserId: String?,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onCommentSubmit: () -> Unit,
    editingCommentId: String?,
    editingCommentText: String,
    onCommentEditStart: (commentId: String, currentText: String) -> Unit,
    onCommentEditTextChange: (String) -> Unit,
    onCommentEditSave: () -> Unit,
    onCommentEditCancel: () -> Unit,
    onCommentDelete: (commentId: String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom (latest comment) when comments change
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = if (comments.isNotEmpty()) {
                    stringResource(R.string.club_comments_count, comments.size)
                } else {
                    stringResource(R.string.club_discussion_title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Comments list with max height
            if (comments.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        val isOwnComment = comment.userId == currentUserId
                        val isEditing = editingCommentId == comment.id

                        CommentBubble(
                            comment = comment,
                            isOwnComment = isOwnComment,
                            isEditing = isEditing,
                            editingText = if (isEditing) editingCommentText else "",
                            onEditTextChange = onCommentEditTextChange,
                            onEditStart = { onCommentEditStart(comment.id, comment.text) },
                            onEditSave = onCommentEditSave,
                            onEditCancel = onCommentEditCancel,
                            onDelete = { onCommentDelete(comment.id) }
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.club_comment_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = onCommentTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.club_comment_placeholder)) },
                    maxLines = 3,
                    enabled = !isLoading && editingCommentId == null
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onCommentSubmit,
                    enabled = !isLoading && commentText.isNotBlank() && editingCommentId == null
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.club_comment_send),
                        tint = if (commentText.isNotBlank() && editingCommentId == null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentBubble(
    comment: BookComment,
    isOwnComment: Boolean,
    isEditing: Boolean,
    editingText: String,
    onEditTextChange: (String) -> Unit,
    onEditStart: () -> Unit,
    onEditSave: () -> Unit,
    onEditCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Alignment and colors based on ownership
    val horizontalArrangement = if (isOwnComment) Arrangement.End else Arrangement.Start
    val bubbleShape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = if (isOwnComment) 12.dp else 4.dp,
        bottomEnd = if (isOwnComment) 4.dp else 12.dp
    )
    val backgroundColor = if (isOwnComment) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isOwnComment) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        Box {
            Surface(
                shape = bubbleShape,
                color = backgroundColor,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            if (isOwnComment && !isEditing) {
                                showMenu = true
                            }
                        }
                    )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Header: Display name and timestamp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comment.displayName.ifBlank { stringResource(R.string.anonymous) },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatRelativeTime(comment.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = 0.7f)
                            )

                            // Show (edited) indicator
                            if (comment.updatedAt > comment.createdAt) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.club_comment_edited),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Content: Text or edit field
                    if (isEditing) {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = onEditTextChange,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = onEditCancel) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_cancel),
                                    tint = contentColor
                                )
                            }
                            IconButton(
                                onClick = onEditSave,
                                enabled = editingText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.club_comment_save),
                                    tint = if (editingText.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        contentColor.copy(alpha = 0.5f)
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                }
            }

            // Dropdown menu for own comments
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.club_comment_edit)) },
                    onClick = {
                        showMenu = false
                        onEditStart()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.club_comment_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}

/**
 * Formats a timestamp to a relative time string (e.g., "2 hours ago", "Yesterday").
 */
private fun formatRelativeTime(timestampMillis: Long): String {
    if (timestampMillis <= 0) return ""

    return DateUtils.getRelativeTimeSpanString(
        timestampMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
