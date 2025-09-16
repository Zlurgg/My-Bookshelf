package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import android.content.Context
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.SystemLanguageProvider
import java.util.Locale

class AndroidSystemLanguageProvider(
    private val context: Context
) : SystemLanguageProvider {

    override fun getCurrentLanguageCode(): String {
        val locale = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
        } catch (e: Exception) {
            Locale.getDefault()
        } ?: Locale.getDefault()

        return mapToOpenLibraryLanguageCode(locale.language)
    }

    override fun getAvailableLanguages(): List<String> {
        return listOf(
            "eng", // English
            "spa", // Spanish
            "fre", // French
            "ger", // German
            "ita", // Italian
            "por", // Portuguese
            "dut", // Dutch
            "rus", // Russian
            "jpn", // Japanese
            "chi", // Chinese
            "kor", // Korean
            "ara", // Arabic
            "hin", // Hindi
        )
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