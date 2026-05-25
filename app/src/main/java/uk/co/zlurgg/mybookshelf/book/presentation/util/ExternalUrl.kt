package uk.co.zlurgg.mybookshelf.book.presentation.util

import androidx.compose.ui.platform.UriHandler
import timber.log.Timber

/**
 * Allowlist gate for opening external URLs derived from Google Books data
 * (`previewLink`, `infoLink`). Both fields are user-untrusted: they enter the app
 * via the network response and are persisted to Room + round-tripped through
 * Firestore for club shelves, so a hostile club admin could try to inject a
 * hostile scheme (`intent://`, `javascript:`, `tel:`, custom schemes) into shared
 * state. The Books API itself only ever returns links under
 * `https://books.google.com/`, so anything outside that is rejected.
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
internal const val ALLOWED_URL_PREFIX = "https://books.google.com/"
internal const val MAX_LOGGED_URL_LENGTH = 200
private const val TAG = "ExternalUrl"

fun openExternalUrl(uriHandler: UriHandler, url: String) {
    if (isAllowedExternalUrl(url)) {
        uriHandler.openUri(url)
    } else {
        val truncated = url.take(MAX_LOGGED_URL_LENGTH)
        Timber.tag(TAG).w("Refused to open non-allowlisted URL: %s", truncated)
        // TODO(closed-testing-release-prep 2.1): forward as non-fatal once Crashlytics is wired
    }
}

internal fun isAllowedExternalUrl(url: String): Boolean =
    url.startsWith(ALLOWED_URL_PREFIX)
