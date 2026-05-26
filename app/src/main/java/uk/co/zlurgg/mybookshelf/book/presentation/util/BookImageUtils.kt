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

/**
 * Returns a URL suitable for tiny cover renderings embedded inside a spine.
 *
 * For Google Books, strips the `&edge=curl` page-curl effect and bumps the
 * `zoom` parameter so the spine shows the same physical cover scan as the
 * detail screen. Google sometimes maps low and high `zoom` values to different
 * cover scans (thumbnail tier vs preview tier), which made the same book look
 * different between shelf and detail — unifying the zoom level fixes that.
 * The curl strip is independent: it looks fine on cover-face surfaces (the
 * search dialog) but reads as a gimmick inside a 3D spine gradient.
 *
 * OL URLs have no curl parameter and use size suffixes rather than zoom —
 * returned unchanged here so spines keep using the `-S` variant for fast
 * shelf loads; bumping size for spines on OL is a separate decision.
 */
fun Book.withSpineImage(): String = when (provider) {
    BookProvider.OPEN_LIBRARY -> imageUrl
    BookProvider.GOOGLE_BOOKS -> imageUrl.replace("zoom=1", "zoom=3").replace("&edge=curl", "")
}
