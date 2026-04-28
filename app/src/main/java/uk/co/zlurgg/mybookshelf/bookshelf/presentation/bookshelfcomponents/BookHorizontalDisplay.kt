package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelfcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.util.getBookThickness

@Composable
fun BookHorizontal(
    book: Book,
    onClick: () -> Unit
) {
    val thickness = getBookThickness(book.numPages)
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .width(150.dp)
            .height(thickness.dp) // Use page-based thickness
            .background(Color(book.spineColor), shape = RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = book.title,
                color = Color.White,
                maxLines = 1,
                fontSize = 10.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            LoadImage(
                imageUrl = book.imageUrl,
                title = book.title,
                modifier = Modifier
                    .size((thickness * 0.9f).dp) // Scale with thickness
                    .rotate(90f)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}
