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
import uk.co.zlurgg.mybookshelf.auth.data.config.AuthConfig
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GoogleAuthUiClient(
    private val context: Context,
    private val authConfig: AuthConfig
) : AuthService {

    companion object {
        private const val TAG = "GoogleAuth"
    }

    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun signIn(): Result<UserData, DataError.Local> {
        Timber.tag(TAG).d("=== GOOGLE SIGN-IN START ===")

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(authConfig.webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Timber.tag(TAG).d("Requesting credential from CredentialManager...")

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken

            Timber.tag(TAG).d("Got ID token, authenticating with Firebase...")

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            if (user != null) {
                Timber.tag(TAG).d("=== SIGN-IN SUCCESS === User ID: %s", user.uid)
                Result.Success(
                    UserData(
                        userId = user.uid,
                        username = user.displayName,
                        profilePictureUrl = user.photoUrl?.toString()
                    )
                )
            } else {
                Timber.tag(TAG).e("Firebase returned null user")
                Result.Error(DataError.Local.AUTH_FAILED)
            }
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
            Timber.tag(TAG).e(e, "Unexpected sign-in error")
            Result.Error(DataError.Local.AUTH_FAILED)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun signOut(): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("=== SIGN OUT START ===")
        return try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            auth.signOut()
            Timber.tag(TAG).d("=== SIGN OUT COMPLETE ===")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Sign-out error")
            Result.Error(DataError.Local.AUTH_FAILED)
        }
    }

    override fun getSignedInUser(): UserData? {
        return auth.currentUser?.let { user ->
            UserData(
                userId = user.uid,
                username = user.displayName,
                profilePictureUrl = user.photoUrl?.toString()
            )
        }
    }
}
