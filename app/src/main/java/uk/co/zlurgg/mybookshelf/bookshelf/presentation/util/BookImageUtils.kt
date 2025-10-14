package uk.co.zlurgg.mybookshelf.bookshelf.presentation.util

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

/**
 * Returns a URL for a small version of the cover image.
 * Optimized for list views and search results (bandwidth efficient).
 *
 * Note: Search results from BookMappers already generate small URLs via
 * ApiConfig.OpenLibrary.CoverUrls. This utility is for converting existing
 * medium/large URLs when needed.
 */
fun Book.withSmallImage(): String {
    return imageUrl.replace(
        Regex("-[SML]\\.jpg$"),
        ApiConfig.OpenLibrary.CoverUrls.CoverSize.SMALL.suffix
    )
}

/**
 * Returns a URL for a medium version of the cover image.
 * Optimized for list views and search results (bandwidth efficient).
 *
 * Note: Search results from BookMappers already generate medium URLs via
 * ApiConfig.OpenLibrary.CoverUrls. This utility is for converting existing
 *  URLs when needed.
 */
fun Book.withMediumImage(): String {
    return imageUrl.replace(
        Regex("-[SML]\\.jpg$"),
        ApiConfig.OpenLibrary.CoverUrls.CoverSize.MEDIUM.suffix
    )
}

/**
 * Returns a URL for a large version of the cover image.
 * Optimized for detail views where image quality matters.
 *
 * Note: Used primarily in BookDetailScreen to convert small search result
 * images to large detail view images.
 */
fun Book.withLargeImage(): String {
    return imageUrl.replace(
        Regex("-[SML]\\.jpg$"),
        ApiConfig.OpenLibrary.CoverUrls.CoverSize.LARGE.suffix
    )
}
