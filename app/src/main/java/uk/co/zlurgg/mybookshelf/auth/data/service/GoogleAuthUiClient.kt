package uk.co.zlurgg.mybookshelf.auth.data.service

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.auth.domain.model.SignInResult
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData

class GoogleAuthUiClient(
    private val context: Context
) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activityContext: Context): SignInResult {
        Timber.tag(TAG).d("=== GOOGLE SIGN-IN START ===")

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.web_client_id))
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Timber.tag(TAG).d("Requesting credential from CredentialManager...")

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken

            Timber.tag(TAG).d("Got ID token, authenticating with Firebase...")

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            Timber.tag(TAG).d("=== SIGN-IN SUCCESS ===")
            Timber.tag(TAG).d("User ID: %s", user?.uid)
            Timber.tag(TAG).d("Display name: %s", user?.displayName)

            SignInResult(
                data = user?.let {
                    UserData(
                        userId = it.uid,
                        username = it.displayName,
                        profilePictureUrl = it.photoUrl?.toString()
                    )
                },
                errorMessage = null
            )
        } catch (e: GetCredentialCancellationException) {
            Timber.tag(TAG).d("Sign-in cancelled by user")
            SignInResult(data = null, errorMessage = "Sign-in cancelled")
        } catch (e: NoCredentialException) {
            Timber.tag(TAG).w("No credentials available")
            SignInResult(
                data = null,
                errorMessage = "No Google account found. Please add one in Settings → Accounts."
            )
        } catch (e: GetCredentialException) {
            Timber.tag(TAG).e(e, "Credential exception")
            SignInResult(
                data = null,
                errorMessage = mapCredentialError(e)
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Unexpected sign-in error")
            SignInResult(
                data = null,
                errorMessage = "Sign-in failed. Please try again."
            )
        }
    }

    suspend fun signOut() {
        Timber.tag(TAG).d("=== SIGN OUT START ===")
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            auth.signOut()
            Timber.tag(TAG).d("=== SIGN OUT COMPLETE ===")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Sign-out error")
        }
    }

    fun getSignedInUser(): UserData? {
        val user = auth.currentUser
        return user?.let {
            UserData(
                userId = it.uid,
                username = it.displayName,
                profilePictureUrl = it.photoUrl?.toString()
            )
        }
    }

    private fun mapCredentialError(e: GetCredentialException): String {
        val message = e.message ?: ""
        return when {
            message.contains("16", ignoreCase = true) ->
                "Configuration error. Please contact support."
            message.contains("network", ignoreCase = true) ->
                "Network error. Please check your connection."
            else -> "Sign-in failed. Please try again."
        }
    }

    companion object {
        private const val TAG = "GoogleAuth"
    }
}
