package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ShareData
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import java.net.URLEncoder

/**
 * Android infrastructure service for handling platform-specific sharing functionality.
 * Responsible for creating Android sharing intents and managing platform integration.
 * Validates URL length to ensure compatibility with older browsers and messaging apps.
 */
class AndroidShareService(
    private val context: Context,
) {
    companion object {
        private const val TAG = "ShareService"

        // Conservative limit for maximum browser compatibility (IE, older browsers)
        private const val MAX_URL_LENGTH = 2000

        // Absolute maximum - definitely too large
        private const val ABSOLUTE_MAX_URL_LENGTH = 10000
    }

    /**
     * Creates and launches an Android share intent with the provided share data.
     * Validates URL length before sharing to prevent issues with large bookshelves.
     * @param shareData The share data containing token and shelf name
     * @return Result indicating success or failure of the share operation
     */
    fun shareBookshelf(shareData: ShareData): Result<Unit, DataError.Local> {
        return try {
            val encodedToken = URLEncoder.encode(shareData.token, "UTF-8")
            val shareUrl = "${ApiConfig.shareBaseUrl}#$encodedToken"

            // Validate URL length (infrastructure concern - appropriate here)
            when {
                shareUrl.length > ABSOLUTE_MAX_URL_LENGTH -> {
                    return Result.Error(DataError.Local.SHARE_LINK_TOO_LARGE)
                }
                shareUrl.length > MAX_URL_LENGTH -> {
                    // Log warning but allow (may work on modern browsers)
                    Timber.tag(TAG).w(
                        "Share URL length (%d) exceeds 2KB recommendation. May not work on older browsers.",
                        shareUrl.length,
                    )
                }
            }

            val shareIntent =
                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareUrl)
                    putExtra(Intent.EXTRA_SUBJECT, "Bookshelf: ${shareData.shelfName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

            val chooserIntent =
                Intent.createChooser(shareIntent, "Share Bookshelf")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooserIntent)
            Result.Success(Unit)
        } catch (_: Exception) {
            Result.Error(DataError.Local.SHARE_FAILED)
        }
    }
}
