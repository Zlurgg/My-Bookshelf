package uk.co.zlurgg.mybookshelf.core.logging

import timber.log.Timber

/**
 * Debug build override that plants Timber's DebugTree for logcat output.
 * Release builds shadow this with a Crashlytics-forwarding tree.
 */
object LoggingInitializer {
    fun initialize() {
        Timber.plant(Timber.DebugTree())
    }
}
