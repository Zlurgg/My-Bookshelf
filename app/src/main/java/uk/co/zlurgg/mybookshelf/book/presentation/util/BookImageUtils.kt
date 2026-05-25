package uk.co.zlurgg.mybookshelf.book.presentation.util

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

/**
 * Returns a URL for a medium version of the cover image.
 * Provider-aware: applies the correct URL transformation for each API.
 *
 * Google Books `zoom` parameter is undocumented in stable form. Google's
 * `volumeInfo.imageLinks.thumbnail` ships with `zoom=1` (~128px thumbnail);
 * `zoom=2` empirically often returns similar dimensions to `zoom=1`. Using
 * `zoom=3` to ensure a meaningfully larger image (medium per Google's loose
 * convention) for the book detail screen.
 */
fun Book.withMediumImage(): String = when (provider) {
    BookProvider.OPEN_LIBRARY ->
        imageUrl.replace(Regex("-[SML]\\.jpg$"), ApiConfig.OpenLibrary.CoverUrls.CoverSize.MEDIUM.suffix)
    BookProvider.GOOGLE_BOOKS ->
        imageUrl.replace("zoom=1", "zoom=3").replace("&edge=curl", "")
}
