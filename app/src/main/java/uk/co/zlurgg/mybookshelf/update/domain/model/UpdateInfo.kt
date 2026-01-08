package uk.co.zlurgg.mybookshelf.update.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model representing available update information.
 * Pure Kotlin - no framework dependencies.
 */
@Immutable
data class UpdateInfo(
    val versionName: String,
    val releaseUrl: String,
    val apkDownloadUrl: String?,
    val apkSize: Long?,
    val changelog: String?,
)
