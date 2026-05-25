package uk.co.zlurgg.mybookshelf.book.domain.model

enum class PrintType {
    BOOK,
    MAGAZINE,
    UNKNOWN;

    companion object {
        fun fromApiValue(value: String?): PrintType = when (value?.uppercase()) {
            "BOOK" -> BOOK
            "MAGAZINE" -> MAGAZINE
            else -> UNKNOWN
        }
    }
}
