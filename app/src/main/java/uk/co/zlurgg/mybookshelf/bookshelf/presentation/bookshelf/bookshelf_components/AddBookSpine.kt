package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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

@Composable
fun AddBookSpine(
    onClick: () -> Unit,
) {
    val cd = stringResource(id = R.string.cd_add_book_to_shelf)
    
    // Bookend/bookstop design with decorative elements
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .height(150.dp)
            .width(60.dp)
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
                    .height(20.dp)
                    .width(56.dp)
                    .background(
                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                    )
            )
            
            // Main body with add icon
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(52.dp)
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
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Decorative base (bookend foot)
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .width(58.dp)
                    .background(
                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                    )
            )
        }
        
        // Subtle highlight line to simulate 3D depth
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(140.dp)
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
