package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.util.getBookThickness

@Composable
fun BookVertical(
    book: Book,
    onClick: () -> Unit,
    height: Int = 150
) {
    val thickness = getBookThickness(book.numPages)
    val baseColor = Color(book.spineColor)
    val lighterColor = baseColor.copy(
        red = (baseColor.red * 1.2f).coerceAtMost(1f),
        green = (baseColor.green * 1.2f).coerceAtMost(1f), 
        blue = (baseColor.blue * 1.2f).coerceAtMost(1f)
    )
    val darkerColor = baseColor.copy(
        red = baseColor.red * 0.7f,
        green = baseColor.green * 0.7f,
        blue = baseColor.blue * 0.7f
    )
    
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .height(height.dp)
            .width(thickness.dp)
            .padding(horizontal = 1.dp) // Contain shadow within bounds
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(4.dp),
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
    ) {
        // 3D spine with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(lighterColor, baseColor, darkerColor),
                        startX = 0f,
                        endX = thickness.toFloat() * 2
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Book image integrated into spine (no separate shadow/background)
                LoadImage(
                    imageUrl = book.imageUrl,
                    title = book.title,
                    modifier = Modifier
                        .size((thickness * 0.8f).dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                
                // Text integrated into spine background
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = book.title,
                        color = Color.White,
                        maxLines = 4,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 9.sp
                    )
                }
            }
        }
        
        // Subtle highlight strip for 3D depth
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(height.dp)
                .offset(x = 3.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(0.5.dp)
                )
        )
    }
}

@Composable
fun BookLeaning(
    book: Book,
    onClick: () -> Unit,
    leanAngle: Float = -5f,
    height: Int = 140
) {
    val thickness = getBookThickness(book.numPages)
    val baseColor = Color(book.spineColor)
    val lighterColor = baseColor.copy(
        red = (baseColor.red * 1.2f).coerceAtMost(1f),
        green = (baseColor.green * 1.2f).coerceAtMost(1f), 
        blue = (baseColor.blue * 1.2f).coerceAtMost(1f)
    )
    val darkerColor = baseColor.copy(
        red = baseColor.red * 0.7f,
        green = baseColor.green * 0.7f,
        blue = baseColor.blue * 0.7f
    )
    
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .rotate(leanAngle)
            .height(height.dp)
            .width(thickness.dp)
            .padding(start = 2.dp, end = 2.dp, bottom = 3.dp) // Bottom padding for lean offset
            .shadow(
                elevation = 3.dp, // Enhanced shadow for leaning effect
                shape = RoundedCornerShape(4.dp),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
    ) {
        // 3D spine with enhanced gradient for leaning effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(lighterColor, baseColor, darkerColor),
                        startX = 0f,
                        endX = thickness.toFloat() * 2
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Book image integrated into spine (no separate effects)
                LoadImage(
                    imageUrl = book.imageUrl,
                    title = book.title,
                    modifier = Modifier
                        .size((thickness * 0.75f).dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                
                // Text integrated into spine background
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = book.title,
                        color = Color.White,
                        maxLines = 3,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 9.sp
                    )
                }
            }
        }
        
        // More prominent highlight for leaning books
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(height.dp)
                .offset(x = 2.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(0.5.dp)
                )
        )
    }
}

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

@Composable
fun BookHorizontalStack(
    books: List<Book>,
    onClick: (Book) -> Unit,
    maxStack: Int = 3
) {
    val stackedBooks = books.take(maxStack)
    
    Column(
        modifier = Modifier
            .width(150.dp)
            .padding(4.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        stackedBooks.forEachIndexed { index, book ->
            Box(
                modifier = Modifier
                    .clickable { onClick(book) }
                    .width(150.dp - (index * 5).dp)
                    .height(25.dp)
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
                            .size(20.dp)
                            .rotate(90f)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}


