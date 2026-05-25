package uk.co.zlurgg.mybookshelf.book.data.mappers

import androidx.core.text.HtmlCompat
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleBookItemDto
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.MaturityRating
import uk.co.zlurgg.mybookshelf.book.domain.model.PrintType

fun GoogleBookItemDto.toBook(): Book {
    val volumeInfo = this.volumeInfo
    val isbn = volumeInfo?.industryIdentifiers
        ?.firstOrNull { it.type == "ISBN_13" }?.identifier
        ?: volumeInfo?.industryIdentifiers?.firstOrNull()?.identifier

    // Google serves HTTP URLs — force HTTPS
    val imageUrl = volumeInfo?.imageLinks?.thumbnail
        ?.replace("http://", "https://") ?: ""

    return Book(
        id = this.id,
        title = volumeInfo?.title ?: "",
        subtitle = volumeInfo?.subtitle,
        authors = volumeInfo?.authors ?: emptyList(),
        imageUrl = imageUrl,
        description = stripHtml(volumeInfo?.description),
        languages = listOfNotNull(volumeInfo?.language),
        firstPublishYear = volumeInfo?.publishedDate?.take(YEAR_LENGTH),
        numPages = volumeInfo?.pageCount,
        purchased = false,
        spineColor = 0,
        provider = BookProvider.GOOGLE_BOOKS,
        isbn = isbn,
        publisher = volumeInfo?.publisher,
        publishDate = volumeInfo?.publishedDate,
        subjects = volumeInfo?.categories ?: emptyList(),
        previewLink = volumeInfo?.previewLink?.takeIf { it.startsWith(HTTPS_PREFIX) },
        infoLink = volumeInfo?.infoLink?.takeIf { it.startsWith(HTTPS_PREFIX) },
        maturityRating = MaturityRating.fromApiValue(volumeInfo?.maturityRating),
        printType = PrintType.fromApiValue(volumeInfo?.printType),
    )
}

private const val YEAR_LENGTH = 4
private const val HTTPS_PREFIX = "https://"

/**
 * Strips HTML tags and decodes entities from Google Books descriptions.
 * Google returns descriptions with HTML formatting (<b>, <i>, <br>, &amp;, etc.).
 * We strip to plain text at the data layer to keep the domain model clean.
 *
 * Internal visibility: used by both GoogleBookMappers and GoogleBooksRemoteBookDataSource.
 */
internal fun stripHtml(html: String?): String? {
    if (html == null) return null
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        .trim()
}
