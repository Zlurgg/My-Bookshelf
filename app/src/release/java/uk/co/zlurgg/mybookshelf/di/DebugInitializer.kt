package uk.co.zlurgg.mybookshelf.di

/**
 * Release build version - does nothing.
 * In debug builds, this is replaced by the debug source set version
 * which configures Firebase emulators.
 */
object DebugInitializer {
    fun initialize() {
        // No-op in release builds
    }
}
