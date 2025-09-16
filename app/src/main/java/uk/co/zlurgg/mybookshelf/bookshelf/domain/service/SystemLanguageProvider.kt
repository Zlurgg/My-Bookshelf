package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

interface SystemLanguageProvider {
    fun getCurrentLanguageCode(): String
    fun getAvailableLanguages(): List<String>
}