package uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    @SerialName("numFound") val numFound: Int,
    @SerialName("docs") val results: List<SearchedBookDto>,
)
