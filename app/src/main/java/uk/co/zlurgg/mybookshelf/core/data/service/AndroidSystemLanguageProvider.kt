package uk.co.zlurgg.mybookshelf.core.data.service

import android.content.Context
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider
import java.util.Locale

class AndroidSystemLanguageProvider(
    private val context: Context
) : SystemLanguageProvider {

    companion object {
        private const val TAG = "SystemLanguageProvider"
    }

    @Suppress("TooGenericExceptionCaught")
    override fun getCurrentLanguageCode(): String {
        val locale = try {
            context.resources.configuration.locales[0]
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get locale, using default")
            Locale.getDefault()
        } ?: Locale.getDefault()

        return mapToOpenLibraryLanguageCode(locale.language)
    }

    private fun mapToOpenLibraryLanguageCode(languageCode: String): String {
        return when (languageCode) {
            "en" -> "eng"
            "es" -> "spa"
            "fr" -> "fre"
            "de" -> "ger"
            "it" -> "ita"
            "pt" -> "por"
            "nl" -> "dut"
            "ru" -> "rus"
            "ja" -> "jpn"
            "zh" -> "chi"
            "ko" -> "kor"
            "ar" -> "ara"
            "hi" -> "hin"
            else -> "eng" // Default to English for unsupported languages
        }
    }
}
