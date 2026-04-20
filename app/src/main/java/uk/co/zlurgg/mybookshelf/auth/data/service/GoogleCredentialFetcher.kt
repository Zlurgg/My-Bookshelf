package uk.co.zlurgg.mybookshelf.auth.data.service

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.data.config.AuthConfig
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GoogleCredentialFetcher(
    private val authConfig: AuthConfig
) {
    companion object {
        private const val TAG = "GoogleCredential"
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun fetch(activity: Activity): Result<String, DataError.Local> {
        Timber.tag(TAG).d("Requesting credential from CredentialManager...")

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(authConfig.webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(activity)
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken

            Timber.tag(TAG).d("Got ID token successfully")
            Result.Success(idToken)
        } catch (_: GetCredentialCancellationException) {
            Timber.tag(TAG).d("Sign-in cancelled by user")
            Result.Error(DataError.Local.AUTH_CANCELLED)
        } catch (_: NoCredentialException) {
            Timber.tag(TAG).w("No credentials available")
            Result.Error(DataError.Local.AUTH_NO_CREDENTIAL)
        } catch (e: GetCredentialException) {
            Timber.tag(TAG).e(e, "Credential exception: %s", e.message)
            Result.Error(DataError.Local.AUTH_FAILED)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Unexpected credential fetch error")
            Result.Error(DataError.Local.AUTH_FAILED)
        }
    }
}
