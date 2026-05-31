package uk.co.zlurgg.mybookshelf.auth.data.service

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
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

        // Tag used for temporary release-build Log.e diagnostics in signIn().
        // Remove with the Log.e calls once root cause is identified.
        private const val DEBUG_TAG = "GoogleAuthDebug"
    }

    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun signIn(idToken: String): Result<UserData, DataError.Local> {
        Timber.tag(TAG).d("=== GOOGLE SIGN-IN START ===")
        // Temporary release-build diagnostic — Log.e survives R8 (only Log.v / Log.d are stripped).
        // Lets us capture the actual sign-in failure via logcat when the CrashlyticsTree path
        // is silently dropping the non-fatal. Remove once root cause identified.
        Log.e(DEBUG_TAG, "signIn entered, idToken length=${idToken.length}")

        return try {
            Timber.tag(TAG).d("Authenticating with Firebase...")
            Log.e(DEBUG_TAG, "About to call signInWithCredential")

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            Log.e(DEBUG_TAG, "signInWithCredential returned, user=${user?.uid ?: "<null>"}")

            if (user != null) {
                Timber.tag(TAG).d("=== SIGN-IN SUCCESS === User ID: %s", user.uid)
                Result.Success(
                    UserData(
                        userId = user.uid,
                        username = user.displayName,
                        email = user.email,
                        profilePictureUrl = user.photoUrl?.toString()
                    )
                )
            } else {
                Timber.tag(TAG).e("Firebase returned null user")
                Log.e(DEBUG_TAG, "Firebase returned null user (no exception thrown)")
                Result.Error(DataError.Local.AUTH_FAILED)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Unexpected sign-in error")
            Log.e(DEBUG_TAG, "Unexpected sign-in error: ${e.javaClass.name}: ${e.message}", e)
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
                email = user.email,
                profilePictureUrl = user.photoUrl?.toString()
            )
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun deleteAccount(): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("=== DELETE ACCOUNT START ===")
        val user = auth.currentUser
        if (user == null) {
            Timber.tag(TAG).e("No current user for account deletion")
            return Result.Error(DataError.Local.AUTH_FAILED)
        }
        return try {
            user.delete().await()
            // Best-effort — account is already deleted, credential clearing is cleanup
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Credential state clear failed after account deletion")
            }
            Timber.tag(TAG).d("=== DELETE ACCOUNT COMPLETE ===")
            Result.Success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Timber.tag(TAG).w("Account deletion requires recent login")
            Result.Error(DataError.Local.REQUIRES_RECENT_LOGIN)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Account deletion failed")
            Result.Error(DataError.Local.AUTH_FAILED)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun reauthenticate(idToken: String): Result<Unit, DataError.Local> {
        Timber.tag(TAG).d("=== REAUTHENTICATE START ===")
        val user = auth.currentUser
        if (user == null) {
            Timber.tag(TAG).e("No current user for re-authentication")
            return Result.Error(DataError.Local.AUTH_FAILED)
        }
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            user.reauthenticate(credential).await()
            Timber.tag(TAG).d("=== REAUTHENTICATE COMPLETE ===")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Re-authentication failed")
            Result.Error(DataError.Local.AUTH_FAILED)
        }
    }
}
