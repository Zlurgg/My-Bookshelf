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

    override fun getCurrentLanguageCode(): String {
        return mapToOpenLibraryLanguageCode(getDeviceLocale().language)
    }

    override fun getRawLanguageCode(): String {
        return getDeviceLocale().language
    }

    private fun getDeviceLocale(): Locale {
        val locales = context.resources.configuration.locales
        return if (locales.isEmpty) {
            Timber.tag(TAG).w("Locale list is empty, using default")
            Locale.getDefault()
        } else {
            locales[0]
        }
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
