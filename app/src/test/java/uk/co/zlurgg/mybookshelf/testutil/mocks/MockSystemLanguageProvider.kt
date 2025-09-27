package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider

class MockSystemLanguageProvider : SystemLanguageProvider {

    private var _currentLanguageCode = "en"
    var getLanguageCallCount = 0

    fun reset() {
        _currentLanguageCode = "en"
        getLanguageCallCount = 0
    }

    fun setCurrentLanguageCode(languageCode: String) {
        _currentLanguageCode = languageCode
    }

    override fun getCurrentLanguageCode(): String {
        getLanguageCallCount++
        return _currentLanguageCode
    }
}