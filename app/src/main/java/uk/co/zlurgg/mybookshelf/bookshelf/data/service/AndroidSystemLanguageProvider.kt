package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import android.content.Context
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.SystemLanguageProvider
import java.util.Locale

class AndroidSystemLanguageProvider(
    private val context: Context
) : SystemLanguageProvider {

    override fun getCurrentLanguageCode(): String {
        val locale = try {
            context.resources.configuration.locales[0]
        } catch (e: Exception) {
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