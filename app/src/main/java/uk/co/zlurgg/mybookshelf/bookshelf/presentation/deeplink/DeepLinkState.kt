package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink

data class DeepLinkState(
    val isLoading: Boolean = false,
    val importSuccessful: Boolean = false,
    val error: String? = null,
    val nameConflict: NameConflictData? = null
)

data class NameConflictData(
    val existingName: String,
    val jsonData: String
)