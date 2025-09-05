package uk.co.zlurgg.mybookshelf.core.domain

object ErrorFormatter {
    fun formatOperationError(operation: String, exception: Exception): String {
        return "Failed to $operation: ${exception.message}"
    }
}