package uk.co.zlurgg.mybookshelf.core.presentation.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.BuildConfig

fun launchInAppReview(activity: Activity) {
    val reviewManager = ReviewManagerFactory.create(activity)
    reviewManager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            reviewManager.launchReviewFlow(activity, task.result)
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to launch review flow")
                    openPlayStoreListing(activity)
                }
        } else {
            Timber.e(task.exception, "Failed to request review flow")
            openPlayStoreListing(activity)
        }
    }
}

private fun openPlayStoreListing(activity: Activity) {
    val uri = Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        activity.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        Timber.e(e, "No Play Store app found")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
        activity.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}
