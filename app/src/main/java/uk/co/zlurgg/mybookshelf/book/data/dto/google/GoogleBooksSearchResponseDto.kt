package uk.co.zlurgg.mybookshelf.book.data.dto.google

import kotlinx.serialization.Serializable

@Serializable
data class GoogleBooksSearchResponseDto(
    val totalItems: Int = 0,
    val items: List<GoogleBookItemDto>? = null
)

@Serializable
data class GoogleBookItemDto(
    val id: String,
    val volumeInfo: GoogleVolumeInfoDto? = null,
    val searchInfo: GoogleSearchInfoDto? = null,
)

@Serializable
data class GoogleVolumeInfoDto(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null,
    val imageLinks: GoogleImageLinksDto? = null,
    val language: String? = null,
    val previewLink: String? = null,
    val infoLink: String? = null,
    val maturityRating: String? = null,
    val printType: String? = null,
    val industryIdentifiers: List<GoogleIndustryIdentifierDto>? = null,
)

@Serializable
data class GoogleImageLinksDto(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null,
)

@Serializable
data class GoogleIndustryIdentifierDto(
    val type: String,
    val identifier: String,
)

@Serializable
data class GoogleSearchInfoDto(
    val textSnippet: String? = null,
)
