package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import android.content.Context
import android.content.Intent
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ShareData
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import java.net.URLEncoder

/**
 * Android infrastructure service for handling platform-specific sharing functionality.
 * Responsible for creating Android sharing intents and managing platform integration.
 */
class AndroidShareService(
    private val context: Context
) {

    /**
     * Creates and launches an Android share intent with the provided share data.
     * @param shareData The share data containing token and shelf name
     * @return Result indicating success or failure of the share operation
     */
    fun shareBookshelf(shareData: ShareData): Result<Unit, DataError.Local> {
        return try {
            val encodedName = URLEncoder.encode(shareData.shelfName, "UTF-8")
            val shareUrl = "$SHARE_BASE_URL/?name=$encodedName#${shareData.token}"
            val message = "Check out my ${shareData.shelfName}!\n$shareUrl"

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, "My Bookshelf: ${shareData.shelfName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Bookshelf")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooserIntent)
            Result.Success(Unit)
        } catch (_: Exception) {
            Result.Error(DataError.Local.SHARE_FAILED)
        }
    }

    companion object {
        private const val SHARE_BASE_URL = "https://zlurgg.github.io/My-Bookshelf/share"
    }
}