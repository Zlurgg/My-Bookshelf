package uk.co.zlurgg.mybookshelf.book.presentation.util

import androidx.compose.ui.platform.UriHandler
import timber.log.Timber

/**
 * Allowlist gate for launching URLs derived from Google Books data
 * (`previewLink`, `infoLink`). Both fields are user-untrusted: they enter the app
 * via the network response and are persisted to Room + round-tripped through
 * Firestore for club shelves, so a hostile club admin could try to inject a
 * hostile scheme (`intent://`, `javascript:`, `tel:`, custom schemes) into shared
 * state.
 *
 * Scope is intentionally Google Books only — hence the file name. Adding a
 * different upstream (e.g. an affiliate program for purchase links) should be a
 * separate utility with its own allowlist and trust model rather than a new
 * entry here.
 *
 * Allowed prefixes:
 * - `https://books.google.com/` — preview pages and most info pages.
 * - `https://play.google.com/store/books/` — info pages for purchasable titles,
 *   returned by `infoLink` (e.g. `…/store/books/details?id=…&source=gbs_api`).
 *   The `/store/books/` path segment is part of the allowlist on purpose: it
 *   prevents Play Store URLs for apps, movies, etc. from being launched if they
 *   ever appeared in stored data.
 *
 * Defence-in-depth: `GoogleBookMappers` also coerces non-HTTPS values to `null`
 * before they land in `Book`. This helper is the second line.
 *
 * On rejection: skip the launch, log a warning (URL truncated to keep log lines
 * sane), and leave a TODO to forward as a Crashlytics non-fatal once Crashlytics
 * is wired up (`closed-testing-release-prep.md` item 2.1) — a rejected URL means
 * either the mapper coercion is broken or a poisoned link reached storage; both
 * are worth knowing about.
 */
private const val BOOKS_GOOGLE_PREFIX = "https://books.google.com/"
private const val PLAY_BOOKS_PREFIX = "https://play.google.com/store/books/"

internal val ALLOWED_GOOGLE_BOOKS_URL_PREFIXES = listOf(BOOKS_GOOGLE_PREFIX, PLAY_BOOKS_PREFIX)
internal const val MAX_LOGGED_URL_LENGTH = 200
private const val TAG = "GoogleBooksUrl"

/**
 * `true` if [url] points at the Play Books store. UI uses this to label a
 * Google Books `infoLink` honestly: for purchasable titles the link redirects
 * to Play Books, not `books.google.com`, and the button copy should reflect
 * where the user is actually going.
 */
fun isPlayBooksUrl(url: String): Boolean = url.startsWith(PLAY_BOOKS_PREFIX)

fun openGoogleBooksUrl(uriHandler: UriHandler, url: String) {
    if (isAllowedGoogleBooksUrl(url)) {
        uriHandler.openUri(url)
    } else {
        val truncated = url.take(MAX_LOGGED_URL_LENGTH)
        Timber.tag(TAG).w("Refused to open non-allowlisted URL: %s", truncated)
        // TODO(closed-testing-release-prep 2.1): forward as non-fatal once Crashlytics is wired
    }
}

internal fun isAllowedGoogleBooksUrl(url: String): Boolean =
    ALLOWED_GOOGLE_BOOKS_URL_PREFIXES.any { url.startsWith(it) }
