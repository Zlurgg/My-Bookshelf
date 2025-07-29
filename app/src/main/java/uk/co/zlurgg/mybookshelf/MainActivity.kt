package uk.co.zlurgg.mybookshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.BookshelfScreen
import uk.co.zlurgg.mybookshelf.ui.theme.MyBookshelfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyBookshelfTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BookshelfScreen(
                        modifier = Modifier.padding(innerPadding),
                        books = sampleBooks,
                        onBookClick = {}
                    )
                }
            }
        }
    }
}

private val sampleBooks = List(50) {
    Book(
        id = it.toString(),
        title = "Test Book $it",
        author = "Author",
        spineImageUrl = "https://upload.wikimedia.org/wikipedia/en/8/8e/Harry_Potter_and_the_Philosopher%27s_Stone_Book_Cover.jpg",
        fullImageUrl = "",
        blurb = "",
        purchased = false,
        affiliateLink = ""
    )
}