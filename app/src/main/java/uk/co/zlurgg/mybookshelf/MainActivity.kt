package uk.co.zlurgg.mybookshelf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.MyBookShelfApp
import uk.co.zlurgg.mybookshelf.core.domain.Result

class MainActivity : ComponentActivity() {

    private val shareTokenService: ShareTokenService by inject()
    private val bookshelfExportService: BookshelfExportService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link if present
        handleDeepLink(intent)

        setContent {
            MyBookShelfApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data?.scheme == "mybookshelf" && data.host == "share") {
            val token = data.path?.removePrefix("/") // Remove leading slash
            if (!token.isNullOrBlank()) {
                importBookshelfByToken(token)
            }
        }
    }

    private fun importBookshelfByToken(token: String) {
        lifecycleScope.launch {
            when (val result = shareTokenService.getShelfDataByToken(token)) {
                is Result.Success -> {
                    // Import the bookshelf data
                    when (val importResult = bookshelfExportService.importBookshelf(result.data)) {
                        is Result.Success -> {
                            // TODO: Show success message or navigate to imported bookshelf
                        }
                        is Result.Error -> {
                            // TODO: Show error message
                        }
                    }
                }
                is Result.Error -> {
                    // TODO: Handle token not found or expired
                }
            }
        }
    }
}