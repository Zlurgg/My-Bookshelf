package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import android.content.Context
import android.content.Intent
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ShareData
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig
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
            val encodedToken = URLEncoder.encode(shareData.token, "UTF-8")
            val shareUrl = "${ApiConfig.shareBaseUrl}#$encodedToken"

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareUrl)
                putExtra(Intent.EXTRA_SUBJECT, "Bookshelf: ${shareData.shelfName}")
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

}