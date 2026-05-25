package uk.co.zlurgg.mybookshelf.book.domain.model

enum class MaturityRating {
    NOT_MATURE,
    MATURE,
    UNKNOWN;

    companion object {
        fun fromApiValue(value: String?): MaturityRating = when (value?.uppercase()) {
            "NOT_MATURE" -> NOT_MATURE
            "MATURE" -> MATURE
            else -> UNKNOWN
        }
    }
}
