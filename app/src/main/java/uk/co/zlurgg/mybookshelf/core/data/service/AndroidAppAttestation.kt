package uk.co.zlurgg.mybookshelf.core.data.service

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

/**
 * Snapshot of the app's package name and SHA-1 fingerprint of its signing
 * certificate, formatted for Google's `X-Android-Package` / `X-Android-Cert`
 * request headers.
 *
 * Google API keys with **Android-app** Application restrictions reject every
 * request that does not carry these headers (the restriction has no other way
 * to verify the request's origin). Without them, switching a Books API key
 * from `Application restrictions = None` to `Android apps` makes the key
 * effectively useless for our app — every request 403s and
 * `FallbackRemoteBookDataSource` silently falls back to OpenLibrary.
 *
 * SHA-1 is uppercase hex without separator colons, per Google's contract:
 * https://cloud.google.com/docs/authentication/api-keys#api_key_restrictions
 */
data class AndroidAppAttestation(
    val packageName: String,
    val signingCertSha1Hex: String,
) {
    companion object {
        fun from(context: Context): AndroidAppAttestation {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            val signers = packageInfo.signingInfo?.apkContentsSigners
                ?: error("No signing certificates for ${context.packageName}")
            val digest = MessageDigest.getInstance("SHA-1").digest(signers.first().toByteArray())
            return AndroidAppAttestation(
                packageName = context.packageName,
                signingCertSha1Hex = digest.joinToString("") { "%02X".format(it) },
            )
        }
    }
}
