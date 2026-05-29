package uk.co.zlurgg.mybookshelf.book.presentation.util

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

/**
 * Returns a higher-quality URL for the detail-screen hero.
 * Provider-aware: applies the correct URL transformation for each API.
 *
 * Google Books: requests an 800-pixel-wide rendering via `&fife=w800`, the CDN
 * scaling parameter. Produces a meaningfully sharper image when Google has a
 * real cover scan. For volumes Google never indexed a cover scan for, the URL
 * still returns Google's own "Image not available" graphic — that's accepted
 * (matches the C6 decision in `next-session-handover.md`). A Coil-level
 * fallback to another URL doesn't help because Google serves the placeholder
 * with HTTP 200 and Coil treats it as a successful load.
 */
fun Book.withMediumImage(): String = when (provider) {
    BookProvider.OPEN_LIBRARY ->
        imageUrl.replace(Regex("-[SML]\\.jpg$"), ApiConfig.OpenLibrary.CoverUrls.CoverSize.MEDIUM.suffix)
    BookProvider.GOOGLE_BOOKS -> {
        // Skip local-drawable markers (e.g. "local:tutorial_book_cover") and
        // blank URLs — they aren't Google Books CDN URLs and appending fife
        // breaks the LOCAL_DRAWABLES lookup in resolveImageModel.
        if (imageUrl.isBlank() || imageUrl.startsWith("local:")) {
            imageUrl
        } else {
            imageUrl
                .replace(Regex("&zoom=\\d+"), "")
                .replace("&edge=curl", "") + "&fife=w800"
        }
    }
}

/**
 * Returns a URL suitable for cover renderings embedded inside a spine, in a
 * search row, or anywhere else we want a small clean face.
 *
 * Google Books: same URL as [withMediumImage] — `zoom=1` with the curl
 * stripped. Curl reads as a gimmick inside a 3D spine gradient and is just
 * noise on a flat row.
 *
 * Open Library URLs have no curl parameter and use size suffixes rather than
 * zoom — returned unchanged so spines keep using the `-S` variant for fast
 * shelf loads.
 */
fun Book.withSpineImage(): String = when (provider) {
    BookProvider.OPEN_LIBRARY -> imageUrl
    BookProvider.GOOGLE_BOOKS -> imageUrl.replace("&edge=curl", "")
}
