package uk.co.zlurgg.mybookshelf.auth.data.service

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.core.data.firebase.FirebaseEmulatorConfig
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Development-only authentication service for use with Firebase Auth Emulator.
 *
 * Creates/signs-in test users with email/password authentication.
 * Only works when connected to the Auth emulator (debug builds).
 *
 * Supports multiple test users for testing multi-user features like book clubs.
 */
class DevAuthService {

    companion object {
        private const val TAG = "DevAuth"
        private const val TEST_PASSWORD = "devpassword123"

        /** Available test users for multi-user testing */
        val TEST_USERS = listOf(
            TestUser(1, "dev1@mybookshelf.test", "Alice (Dev 1)"),
            TestUser(2, "dev2@mybookshelf.test", "Bob (Dev 2)"),
            TestUser(3, "dev3@mybookshelf.test", "Charlie (Dev 3)")
        )
    }

    data class TestUser(
        val number: Int,
        val email: String,
        val displayName: String
    )

    private val auth = FirebaseAuth.getInstance()

    /**
     * Signs in with a specific test user account.
     * Creates the account if it doesn't exist.
     *
     * @param userNumber 1, 2, or 3 to select which test user
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun signInAsTestUser(userNumber: Int = 1): Result<UserData, DataError.Local> {
        val testUser = TEST_USERS.find { it.number == userNumber } ?: TEST_USERS.first()
        Timber.tag(TAG).d("=== DEV SIGN-IN START === User: %s", testUser.displayName)

        return try {
            // Fast-fail if emulators aren't running (avoids 30s+ Firebase timeout)
            val reachable = withContext(Dispatchers.IO) {
                FirebaseEmulatorConfig.areEmulatorsReachable()
            }
            if (!reachable) {
                Timber.tag(TAG).e("Firebase emulators not reachable — start with: firebase emulators:start")
                return Result.Error(DataError.Local.AUTH_FAILED)
            }

            // Try to sign in first
            val signInResult = trySignIn(testUser)
            if (signInResult != null) {
                return signInResult
            }

            // If sign-in failed (user doesn't exist), create the account
            Timber.tag(TAG).d("Creating test user account: %s", testUser.email)
            val createResult = auth.createUserWithEmailAndPassword(testUser.email, TEST_PASSWORD).await()
            val user = createResult.user

            if (user != null) {
                Timber.tag(TAG).d("=== DEV SIGN-IN SUCCESS (new account) === User ID: %s", user.uid)
                Result.Success(
                    UserData(
                        userId = user.uid,
                        username = testUser.displayName,
                        profilePictureUrl = null
                    )
                )
            } else {
                Timber.tag(TAG).e("Firebase returned null user after account creation")
                Result.Error(DataError.Local.AUTH_FAILED)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Dev sign-in failed")
            Result.Error(DataError.Local.AUTH_FAILED)
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun trySignIn(testUser: TestUser): Result<UserData, DataError.Local>? {
        return try {
            Timber.tag(TAG).d("Attempting to sign in as: %s", testUser.email)
            val result = auth.signInWithEmailAndPassword(testUser.email, TEST_PASSWORD).await()
            val user = result.user

            if (user != null) {
                Timber.tag(TAG).d("=== DEV SIGN-IN SUCCESS === User ID: %s", user.uid)
                Result.Success(
                    UserData(
                        userId = user.uid,
                        username = testUser.displayName,
                        profilePictureUrl = null
                    )
                )
            } else {
                null
            }
        } catch (e: Exception) {
            // Sign-in failed, likely because user doesn't exist - this is expected
            Timber.tag(TAG).d("Existing account not found (expected): %s", e.message)
            null
        }
    }
}
