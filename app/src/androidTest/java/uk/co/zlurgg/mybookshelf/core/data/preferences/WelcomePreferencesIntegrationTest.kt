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
 * Tests DataStore persistence and retrieval of welcome screen state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WelcomePreferencesIntegrationTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var welcomePreferences: WelcomePreferences
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create a unique DataStore for each test
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = {
                context.preferencesDataStoreFile("test_welcome_preferences")
            }
        )

        welcomePreferences = WelcomePreferencesImpl(context, testDataStore)
    }

    @After
    fun tearDown() {
        // Clean up test DataStore file
        context.preferencesDataStoreFile("test_welcome_preferences").delete()
    }

    @Test
    fun hasShownWelcomeReturnsFalseInitially() = runTest {
        // Given - Fresh install

        // When - Check if welcome was shown
        val hasShown = welcomePreferences.hasShownWelcome().first()

        // Then - Should be false
        assertFalse("Welcome should not be shown initially", hasShown)
    }

    @Test
    fun setWelcomeShownPersistsToDataStore() = runTest {
        // Given - Initial state
        assertFalse(welcomePreferences.hasShownWelcome().first())

        // When - Mark welcome as shown
        welcomePreferences.setWelcomeShown()

        // Then - Should persist
        assertTrue("Welcome shown state should persist",
            welcomePreferences.hasShownWelcome().first())
    }

    @Test
    fun setWelcomeShownPersistsAcrossInstances() = runTest {
        // Given - First instance marks welcome as shown
        welcomePreferences.setWelcomeShown()

        // When - Create new instance with same DataStore
        val newInstance = WelcomePreferencesImpl(context, testDataStore)

        // Then - Should still be true
        assertTrue("Welcome state should persist across instances",
            newInstance.hasShownWelcome().first())
    }

    @Test
    fun multipleCallsToSetWelcomeShownAreIdempotent() = runTest {
        // Given/When - Call multiple times
        welcomePreferences.setWelcomeShown()
        welcomePreferences.setWelcomeShown()
        welcomePreferences.setWelcomeShown()

        // Then - Should still be true
        assertTrue("Multiple calls should be idempotent",
            welcomePreferences.hasShownWelcome().first())
    }
}
