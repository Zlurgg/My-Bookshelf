package uk.co.zlurgg.mybookshelf.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Forwards Log.ERROR-priority Timber events as Crashlytics non-fatals. The
 * originating Timber tag is attached as a custom key and the message is
 * logged as a breadcrumb (`log(...)`) so the Crashlytics console shows it
 * inline with the non-fatal.
 *
 * Lower-priority events (WARN/INFO/DEBUG/VERBOSE) are dropped — Crashlytics
 * pricing is per-event and we don't want every WARN in production.
 */
internal class CrashlyticsTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.ERROR

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (tag != null) crashlytics.setCustomKey(KEY_TAG, tag)
        crashlytics.log(message)
        crashlytics.recordException(t ?: NonFatalLogException(message))
    }

    private class NonFatalLogException(message: String) : RuntimeException(message)

    private companion object {
        const val KEY_TAG = "timber_tag"
    }
}
