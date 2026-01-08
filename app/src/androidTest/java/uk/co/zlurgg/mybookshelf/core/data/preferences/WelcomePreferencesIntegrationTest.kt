package uk.co.zlurgg.mybookshelf.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences

/**
 * Integration test for WelcomePreferences with real DataStore.
 * Tests DataStore persistence and retrieval of per-user welcome screen state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WelcomePreferencesIntegrationTest {
    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var welcomePreferences: WelcomePreferences
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())
    private var testFileName = ""

    // Test user IDs
    private val guestUserId: String? = null
    private val userA = "firebase-user-a"
    private val userB = "firebase-user-b"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create a unique DataStore file name for each test to avoid singleton conflicts
        testFileName = "test_welcome_preferences_${System.currentTimeMillis()}"

        testDataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = {
                    context.preferencesDataStoreFile(testFileName)
                },
            )

        welcomePreferences = WelcomePreferencesImpl(context, testDataStore)
    }

    @After
    fun tearDown() {
        // Clean up test DataStore file
        context.preferencesDataStoreFile(testFileName).delete()
    }

    @Test
    fun hasShownWelcomeReturnsFalseInitiallyForGuest() =
        runTest {
            // Given - Fresh install

            // When - Check if welcome was shown for guest
            val hasShown = welcomePreferences.hasShownWelcome(guestUserId).first()

            // Then - Should be false
            assertFalse("Welcome should not be shown initially for guest", hasShown)
        }

    @Test
    fun hasShownWelcomeReturnsFalseInitiallyForUser() =
        runTest {
            // Given - Fresh install

            // When - Check if welcome was shown for a user
            val hasShown = welcomePreferences.hasShownWelcome(userA).first()

            // Then - Should be false
            assertFalse("Welcome should not be shown initially for user", hasShown)
        }

    @Test
    fun setWelcomeShownPersistsToDataStoreForGuest() =
        runTest {
            // Given - Initial state
            assertFalse(welcomePreferences.hasShownWelcome(guestUserId).first())

            // When - Mark welcome as shown for guest
            welcomePreferences.setWelcomeShown(guestUserId)

            // Then - Should persist
            assertTrue(
                "Welcome shown state should persist for guest",
                welcomePreferences.hasShownWelcome(guestUserId).first(),
            )
        }

    @Test
    fun setWelcomeShownPersistsToDataStoreForUser() =
        runTest {
            // Given - Initial state
            assertFalse(welcomePreferences.hasShownWelcome(userA).first())

            // When - Mark welcome as shown for user
            welcomePreferences.setWelcomeShown(userA)

            // Then - Should persist
            assertTrue(
                "Welcome shown state should persist for user",
                welcomePreferences.hasShownWelcome(userA).first(),
            )
        }

    @Test
    fun setWelcomeShownPersistsAcrossInstances() =
        runTest {
            // Given - First instance marks welcome as shown
            welcomePreferences.setWelcomeShown(userA)

            // When - Create new instance with same DataStore
            val newInstance = WelcomePreferencesImpl(context, testDataStore)

            // Then - Should still be true
            assertTrue(
                "Welcome state should persist across instances",
                newInstance.hasShownWelcome(userA).first(),
            )
        }

    @Test
    fun multipleCallsToSetWelcomeShownAreIdempotent() =
        runTest {
            // Given/When - Call multiple times
            welcomePreferences.setWelcomeShown(userA)
            welcomePreferences.setWelcomeShown(userA)
            welcomePreferences.setWelcomeShown(userA)

            // Then - Should still be true
            assertTrue(
                "Multiple calls should be idempotent",
                welcomePreferences.hasShownWelcome(userA).first(),
            )
        }

    @Test
    fun differentUsersHaveIndependentWelcomeState() =
        runTest {
            // Given - User A has seen welcome, user B hasn't
            welcomePreferences.setWelcomeShown(userA)

            // When - Check state for both users
            val userAHasShown = welcomePreferences.hasShownWelcome(userA).first()
            val userBHasShown = welcomePreferences.hasShownWelcome(userB).first()

            // Then - Only user A should show as welcomed
            assertTrue("User A should have seen welcome", userAHasShown)
            assertFalse("User B should not have seen welcome", userBHasShown)
        }

    @Test
    fun guestAndUserHaveIndependentWelcomeState() =
        runTest {
            // Given - Guest has seen welcome
            welcomePreferences.setWelcomeShown(guestUserId)

            // When - Check state for guest and user
            val guestHasShown = welcomePreferences.hasShownWelcome(guestUserId).first()
            val userHasShown = welcomePreferences.hasShownWelcome(userA).first()

            // Then - Only guest should show as welcomed
            assertTrue("Guest should have seen welcome", guestHasShown)
            assertFalse("User should not have seen welcome", userHasShown)
        }

    @Test
    fun multipleUsersCanEachSeeWelcome() =
        runTest {
            // Given - User A and B both see welcome
            welcomePreferences.setWelcomeShown(userA)
            welcomePreferences.setWelcomeShown(userB)

            // When - Check state for both users
            val userAHasShown = welcomePreferences.hasShownWelcome(userA).first()
            val userBHasShown = welcomePreferences.hasShownWelcome(userB).first()

            // Then - Both should show as welcomed
            assertTrue("User A should have seen welcome", userAHasShown)
            assertTrue("User B should have seen welcome", userBHasShown)
        }
}
