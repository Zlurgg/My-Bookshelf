package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import android.content.Context

/**
 * Service for managing onboarding state persistence.
 *
 * Handles first launch detection using SharedPreferences.
 * This is a pure persistence layer - contains NO business logic.
 */
class OnboardingService(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Checks if this is the first time the app has been launched.
     *
     * @return true if first launch, false if app has been launched before
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Marks the first launch as complete.
     * Subsequent calls to [isFirstLaunch] will return false.
     */
    fun markFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "onboarding"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
    }
}