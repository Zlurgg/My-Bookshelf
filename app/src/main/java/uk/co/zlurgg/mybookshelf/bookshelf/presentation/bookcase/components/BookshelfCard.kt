package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.presentation.bookshelf
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun BookshelfCard(
    shelf: Bookshelf,
    bookCount: Int,
    isReorderMode: Boolean,
    onBookshelfClick: (Bookshelf) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onBookshelfClick(shelf) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 8.dp,
            color = getShelfStyleColor(shelf.shelfStyle)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = shelf.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = pluralStringResource(
                        id = R.plurals.bookcount_books,
                        count = bookCount,
                        bookCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isReorderMode) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getShelfStyleColor(shelfStyle: ShelfStyle): Color {
    return when (shelfStyle) {
        ShelfStyle.DarkWood -> Color(0xFF8B4513)
        ShelfStyle.SilverMetal -> Color(0xFFC0C0C0)
        ShelfStyle.WhiteMetal -> Color(0xFFF5F5F5)
        ShelfStyle.GreyMetal -> Color(0xFF808080)
        ShelfStyle.DarkGreyMetal -> Color(0xFF555555)
    }
}

@Preview(showBackground = true)
@Composable
fun BookshelfCardPreview() {
    MyBookshelfTheme {
        Column {
            BookshelfCard(
                shelf = bookshelf,
                bookCount = 5,
                isReorderMode = false,
                onBookshelfClick = {}
            )
            BookshelfCard(
                shelf = bookshelf.copy(name = "My Reading List"),
                bookCount = 12,
                isReorderMode = true,
                onBookshelfClick = {}
            )
        }
    }
}