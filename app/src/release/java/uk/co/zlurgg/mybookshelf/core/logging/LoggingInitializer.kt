package uk.co.zlurgg.mybookshelf.core.logging

import timber.log.Timber

/**
 * Release build version that forwards Log.ERROR-priority Timber events as
 * Firebase Crashlytics non-fatals (with the originating tag preserved as a
 * custom key for triage). The blank-key path in
 * `GoogleBooksRemoteBookDataSource.searchBooks` and similar graceful-degradation
 * logs would otherwise be silent in the field — tester devices are not
 * accessible for logcat. See closed-testing-release-prep.md item 2.1.
 */
object LoggingInitializer {
    fun initialize() {
        Timber.plant(CrashlyticsTree())
    }
}
