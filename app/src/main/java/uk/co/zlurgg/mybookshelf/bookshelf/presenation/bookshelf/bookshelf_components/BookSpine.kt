package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.bookshelf_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.sampleBook

@Composable
fun BookSpine(
    book: Book,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(book.spineColor, shape = RoundedCornerShape(4.dp)) // default spine bg
            .padding(4.dp, top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .height(150.dp)
                .width(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

//          Book image at the top
            Box(
                modifier = Modifier
                    .padding(top = 4.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                LoadImage(
                    imageUrl = book.imageUrl,
                    title = book.title,
                    modifier = Modifier.size(50.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

//          Rotated title below the image
            Box(
                modifier = Modifier
                    .rotate(-90f)
                    .fillMaxSize()
                    .wrapContentSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    modifier = Modifier,
                    text = book.title,
                    color = Color.White,
                    maxLines = 4,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Left,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle.Default,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookSpinePreview() {
    BookSpine(
        book = sampleBook,
        onClick = {}
    )
}
