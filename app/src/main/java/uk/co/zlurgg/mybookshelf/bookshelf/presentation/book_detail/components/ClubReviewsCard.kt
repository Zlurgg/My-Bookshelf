package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview

/**
 * Card showing all club reviews and input for user's review.
 */
@Composable
fun ClubReviewsCard(
    reviews: List<BookClubReview>,
    currentUserId: String?,
    userReviewText: String,
    onReviewTextChange: (String) -> Unit,
    onReviewSubmit: () -> Unit,
    onReviewDelete: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // Filter to only reviews with text
    val reviewsWithText = reviews.filter { it.reviewText.isNotBlank() }
    val userHasExistingReview = reviews.any { it.userId == currentUserId && it.reviewText.isNotBlank() }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (reviewsWithText.isNotEmpty()) {
                    stringResource(R.string.club_reviews_count, reviewsWithText.size)
                } else {
                    stringResource(R.string.club_reviews_title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display other members' reviews
            val otherReviews = reviewsWithText.filter { it.userId != currentUserId }
            if (otherReviews.isNotEmpty()) {
                otherReviews.forEach { review ->
                    ReviewItem(review = review)
                    if (review != otherReviews.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if (reviewsWithText.isEmpty()) {
                Text(
                    text = stringResource(R.string.club_review_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // User's review input section
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.club_review_your_review),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = userReviewText,
                onValueChange = onReviewTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.club_review_placeholder)) },
                minLines = 2,
                maxLines = 5,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                if (userHasExistingReview) {
                    IconButton(
                        onClick = onReviewDelete,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.club_review_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                IconButton(
                    onClick = onReviewSubmit,
                    enabled = !isLoading && userReviewText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.club_review_post),
                        tint = if (userReviewText.isNotBlank()) {
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

@Composable
private fun ReviewItem(
    review: BookClubReview,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = review.displayName.ifBlank { "Anonymous" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (review.rating > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.rating_display, review.rating.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = review.reviewText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
