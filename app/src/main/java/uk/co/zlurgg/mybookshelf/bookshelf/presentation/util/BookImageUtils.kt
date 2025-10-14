package uk.co.zlurgg.mybookshelf.bookshelf.presentation.util

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

/**
 * Returns a URL for a small version of the cover image.
 * Optimized for list views and search results (bandwidth efficient).
 */
fun Book.withSmallImage(): String {
    return imageUrl.replace(Regex("-[SML]\\.jpg$"), "-S.jpg")
}

/**
 * Returns a URL for a large version of the cover image.
 * Optimized for detail views where image quality matters.
 */
fun Book.withLargeImage(): String {
    return imageUrl.replace(Regex("-[SML]\\.jpg$"), "-L.jpg")
}
