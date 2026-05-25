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
        ?.firstOrNull { it.type == ISBN_13_TYPE }?.identifier
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
        description = this.toDescription(),
        languages = listOfNotNull(volumeInfo?.language),
        // Google returns ISO dates ("2010", "2010-05", "2010-05-15") but also
        // historical/uncertain forms ("c. 2010", "19??", "2010s"). Extract the
        // first run of four digits so garbage like "c. 2" never reaches storage.
        firstPublishYear = volumeInfo?.publishedDate?.let { YEAR_REGEX.find(it)?.value },
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
        // textSnippet contains HTML similar to descriptions — strip before storing
        searchSnippet = stripHtml(this.searchInfo?.textSnippet),
    )
}

/**
 * Single owner of "Google → domain description" mapping.
 *
 * Used by both [toBook] (search-result mapping path) and the
 * `getBookDescription` flow on `GoogleBooksRemoteBookDataSource` (detail-fetch
 * path). Keeps HTML stripping in one place so behavioural changes (e.g. entity
 * handling) land everywhere at once.
 */
internal fun GoogleBookItemDto.toDescription(): String? =
    stripHtml(this.volumeInfo?.description)

private const val ISBN_13_TYPE = "ISBN_13"
private const val HTTPS_PREFIX = "https://"
private val YEAR_REGEX = Regex("\\d{4}")

/**
 * Strips HTML tags and decodes entities from Google Books descriptions.
 * Google returns descriptions with HTML formatting (<b>, <i>, <br>, &amp;, etc.).
 * We strip to plain text at the data layer to keep the domain model clean.
 *
 * Internal visibility: used by both GoogleBookMappers (search-snippet) and the
 * shared [toDescription] mapper.
 */
internal fun stripHtml(html: String?): String? {
    if (html == null) return null
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        .trim()
}
