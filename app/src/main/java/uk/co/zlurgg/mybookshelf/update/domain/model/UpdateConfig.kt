package uk.co.zlurgg.mybookshelf.update.domain.model

/**
 * Configuration for the update feature.
 * Allows the update package to be reused across different projects.
 *
 * @param gitHubOwner GitHub repository owner (e.g., "Zlurgg")
 * @param gitHubRepo GitHub repository name (e.g., "My-Bookshelf")
 * @param appName App name used in APK filename (e.g., "my-bookshelf")
 * @param downloadTitle Notification title shown during download
 */
data class UpdateConfig(
    val gitHubOwner: String,
    val gitHubRepo: String,
    val appName: String,
    val downloadTitle: String = "Downloading Update",
)
