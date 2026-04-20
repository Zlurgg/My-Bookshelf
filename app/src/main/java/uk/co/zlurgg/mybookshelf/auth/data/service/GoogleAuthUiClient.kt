package uk.co.zlurgg.mybookshelf.auth.data.service

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GoogleAuthUiClient(
    private val context: Context
) : AuthService {

    companion object {
        private const val TAG = "GoogleAuth"
    }

    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun signIn(idToken: String): Result<UserData, DataError.Local> {
        Timber.tag(TAG).d("=== GOOGLE SIGN-IN START ===")

        return try {
            Timber.tag(TAG).d("Authenticating with Firebase...")

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
