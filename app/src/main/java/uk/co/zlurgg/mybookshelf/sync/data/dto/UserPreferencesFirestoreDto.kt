package uk.co.zlurgg.mybookshelf.sync.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for User Preferences document.
 *
 * Document path: /users/{userId}/settings/preferences
 *
 * Stores user-level preferences that should sync across devices.
 * Currently tracks onboarding state (welcomeShown) so users only see
 * the welcome screen once per account, not once per device.
 *
 * Note: Firestore requires a no-arg constructor for deserialization,
 * hence the default values for all properties.
 */
data class UserPreferencesFirestoreDto(
    @DocumentId
    val id: String = "",

    @get:PropertyName("welcome_shown")
    @set:PropertyName("welcome_shown")
    var welcomeShown: Boolean = false,

    // Sync metadata
    @get:PropertyName("last_modified_at")
    @set:PropertyName("last_modified_at")
    var lastModifiedAt: Long = 0L
)
