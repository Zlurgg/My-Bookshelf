package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R

// Constants for AddBookSpine dimensions
private const val BOOKEND_HEIGHT = 150
private const val BOOKEND_WIDTH = 60
private const val BOOKEND_CAP_HEIGHT = 20
private const val BOOKEND_CAP_WIDTH = 56
private const val BOOKEND_BODY_WIDTH = 52
private const val BOOKEND_FOOT_HEIGHT = 12
private const val BOOKEND_FOOT_WIDTH = 58
private const val HIGHLIGHT_WIDTH = 2
private const val HIGHLIGHT_HEIGHT = 140
private const val ICON_SIZE = 24

@Composable
fun AddBookSpine(
    onClick: () -> Unit,
) {
    val cd = stringResource(id = R.string.cd_add_book_to_shelf)
    
    // Bookend/bookstop design with decorative elements
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .height(BOOKEND_HEIGHT.dp)
            .width(BOOKEND_WIDTH.dp)
            .semantics { contentDescription = cd }
    ) {
        // Base bookend structure with gradient-like appearance
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                )
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Decorative top section (bookend cap)
            Box(
                modifier = Modifier
                    .height(BOOKEND_CAP_HEIGHT.dp)
                    .width(BOOKEND_CAP_WIDTH.dp)
                    .background(
                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                    )
            )
            
            // Main body with add icon
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(BOOKEND_BODY_WIDTH.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(2.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(ICON_SIZE.dp)
                )
            }
            
            // Decorative base (bookend foot)
            Box(
                modifier = Modifier
                    .height(BOOKEND_FOOT_HEIGHT.dp)
                    .width(BOOKEND_FOOT_WIDTH.dp)
                    .background(
                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                    )
            )
        }
        
        // Subtle highlight line to simulate 3D depth
        Box(
            modifier = Modifier
                .width(HIGHLIGHT_WIDTH.dp)
                .height(HIGHLIGHT_HEIGHT.dp)
                .background(
                    MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(1.dp)
                )
                .align(Alignment.CenterStart)
        )
    }
}

@Preview
@Composable
private fun AddBookSpinePreview() {
    MaterialTheme {
        AddBookSpine(onClick = {})
    }
}
