package uk.co.zlurgg.mybookshelf.core.domain.service

interface SystemLanguageProvider {
    fun getCurrentLanguageCode(): String
    fun getRawLanguageCode(): String
}
