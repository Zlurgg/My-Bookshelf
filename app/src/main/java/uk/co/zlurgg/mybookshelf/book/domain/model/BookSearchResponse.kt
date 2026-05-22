package uk.co.zlurgg.mybookshelf.book.domain.model

data class BookSearchResponse(
    val totalResults: Int,
    val books: List<Book>
)
