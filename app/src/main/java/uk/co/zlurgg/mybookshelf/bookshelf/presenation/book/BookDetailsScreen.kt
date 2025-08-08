package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book.components.RatingBar
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.util.sampleBook

@Composable
fun BookDetailsScreen(
    book: Book,
    onRate: (Int) -> Unit,
    onPurchaseClick: () -> Unit,
    onAddToBookShelfClick:() -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = onPurchaseClick,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (!book.onShelf) Icon(Icons.Default.Add, contentDescription = "Add") else Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
                FloatingActionButton(
                    onClick = onAddToBookShelfClick,
                    containerColor = if (book.purchased) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Purchase")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = book.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "by ${book.authors.joinToString()}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = book.imageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            RatingBar(
                rating = book.averageRating?.toInt() ?: 0,
                onRatingChanged = onRate
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = book.description ?: "No description available.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookDetailScreenPreview() {
    BookDetailsScreen(
        book = sampleBook,
        onRate = {},
        onPurchaseClick = {},
        onAddToBookShelfClick = {}
    )
}