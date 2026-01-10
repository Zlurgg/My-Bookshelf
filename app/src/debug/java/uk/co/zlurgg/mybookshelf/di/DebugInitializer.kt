package uk.co.zlurgg.mybookshelf.di

import uk.co.zlurgg.mybookshelf.core.data.firebase.FirebaseEmulatorConfig

/**
 * Debug build override that initializes debug-only components.
 * This file shadows the release version and configures Firebase emulators.
 */
object DebugInitializer {
    fun initialize() {
        // Configure Firebase to use local emulators
        // Start emulators with: firebase emulators:start
        FirebaseEmulatorConfig.configureEmulators()
    }
}
