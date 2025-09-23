package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase

sealed class ImportResult {
    data object Success : ImportResult()
    data class NameConflict(val existingName: String, val jsonData: String) : ImportResult()
}