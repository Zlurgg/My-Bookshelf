package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink

sealed class ImportResult {
    data object Success : ImportResult()
    data class NameConflict(val existingName: String, val jsonData: String) : ImportResult()
}