package uk.co.zlurgg.mybookshelf.book.presentation.util

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

/**
 * Returns a URL for a small version of the cover image.
 * Provider-aware: applies the correct URL transformation for each API.
 */
fun Book.withSmallImage(): String = when (provider) {
    BookProvider.OPEN_LIBRARY ->
        imageUrl.replace(Regex("-[SML]\\.jpg$"), ApiConfig.OpenLibrary.CoverUrls.CoverSize.SMALL.suffix)
    BookProvider.GOOGLE_BOOKS ->
        imageUrl.replace("zoom=1", "zoom=1").replace("&edge=curl", "")
}

/**
 * Returns a URL for a medium version of the cover image.
 * Provider-aware: applies the correct URL transformation for each API.
 */
fun Book.withMediumImage(): String = when (provider) {
    BookProvider.OPEN_LIBRARY ->
        imageUrl.replace(Regex("-[SML]\\.jpg$"), ApiConfig.OpenLibrary.CoverUrls.CoverSize.MEDIUM.suffix)
    BookProvider.GOOGLE_BOOKS ->
        imageUrl.replace("zoom=1", "zoom=2").replace("&edge=curl", "")
}

/**
 * Returns a URL for a large version of the cover image.
 * Provider-aware: applies the correct URL transformation for each API.
 */
fun Book.withLargeImage(): String = when (provider) {
    BookProvider.OPEN_LIBRARY ->
        imageUrl.replace(Regex("-[SML]\\.jpg$"), ApiConfig.OpenLibrary.CoverUrls.CoverSize.LARGE.suffix)
    BookProvider.GOOGLE_BOOKS ->
        imageUrl.replace("zoom=1", "zoom=3").replace("&edge=curl", "")
}
