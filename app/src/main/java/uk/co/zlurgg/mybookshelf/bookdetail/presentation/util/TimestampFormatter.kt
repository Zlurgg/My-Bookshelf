package uk.co.zlurgg.mybookshelf.bookdetail.presentation.util

import android.text.format.DateUtils

/**
 * Formats a timestamp to a relative time string (e.g., "2 hours ago", "Yesterday").
 */
fun formatRelativeTime(timestampMillis: Long): String {
    if (timestampMillis <= 0) return ""

    return DateUtils.getRelativeTimeSpanString(
        timestampMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
