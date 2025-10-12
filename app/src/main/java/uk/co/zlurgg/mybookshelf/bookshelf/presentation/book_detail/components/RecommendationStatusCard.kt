package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus

/**
 * Card for managing personal recommendation status.
 * Shows: Reading Status dropdown + Personal Rating (1-5 stars).
 * This data is NOT exported for privacy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationStatusCard(
    readingStatus: ReadingStatus,
    personalRating: Float?,
    onReadingStatusChange: (ReadingStatus) -> Unit,
    onPersonalRatingChange: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "My Reading Status",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reading Status Dropdown
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = readingStatus.toDisplayString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reading Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ReadingStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.toDisplayString()) },
                            onClick = {
                                onReadingStatusChange(status)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Personal Rating
            Text(
                text = "My Rating",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    IconButton(
                        onClick = {
                            val newRating = if (personalRating == i.toFloat()) null else i.toFloat()
                            onPersonalRatingChange(newRating)
                        }
                    ) {
                        Icon(
                            imageVector = if (personalRating != null && i <= personalRating) {
                                Icons.Filled.Star
                            } else {
                                Icons.Filled.StarBorder
                            },
                            contentDescription = "Rate $i stars",
                            tint = if (personalRating != null && i <= personalRating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                if (personalRating != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${personalRating.toInt()}/5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Converts ReadingStatus enum to user-friendly display string.
 */
private fun ReadingStatus.toDisplayString(): String = when (this) {
    ReadingStatus.WANT_TO_READ -> "Want to Read"
    ReadingStatus.CURRENTLY_READING -> "Currently Reading"
    ReadingStatus.READ -> "Read"
}
