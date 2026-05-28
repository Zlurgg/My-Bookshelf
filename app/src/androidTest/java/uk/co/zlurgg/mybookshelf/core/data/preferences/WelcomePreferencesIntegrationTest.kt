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
 * Integration test for [WelcomePreferencesImpl] with real DataStore.
 *
 * Welcome state is per-device (single boolean), so the only persisted facts are:
 * initial value, set, idempotence, and survival across instances. Per-user tests
 * existed previously when the API took a userId; they were removed when the
 * design moved to per-device (see [WelcomePreferences] docstring).
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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Per-test unique file name so the DataStore singleton doesn't bleed between tests.
        testFileName = "test_welcome_preferences_${System.currentTimeMillis()}"

        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = {
                context.preferencesDataStoreFile(testFileName)
            }
        )

        welcomePreferences = WelcomePreferencesImpl(testDataStore)
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(testFileName).delete()
    }

    @Test
    fun hasShownWelcomeReturnsFalseInitially() = runTest {
        val hasShown = welcomePreferences.hasShownWelcome().first()

        assertFalse("Welcome should not be shown on a fresh install", hasShown)
    }

    @Test
    fun setWelcomeShownPersistsToDataStore() = runTest {
        assertFalse(welcomePreferences.hasShownWelcome().first())

        welcomePreferences.setWelcomeShown()

        assertTrue(
            "Welcome shown state should persist after set",
            welcomePreferences.hasShownWelcome().first()
        )
    }

    @Test
    fun setWelcomeShownPersistsAcrossInstances() = runTest {
        welcomePreferences.setWelcomeShown()

        // Same backing DataStore, fresh impl — simulates a recreated repository.
        val newInstance = WelcomePreferencesImpl(testDataStore)

        assertTrue(
            "Welcome state should survive a new repository instance",
            newInstance.hasShownWelcome().first()
        )
    }

    @Test
    fun multipleCallsToSetWelcomeShownAreIdempotent() = runTest {
        welcomePreferences.setWelcomeShown()
        welcomePreferences.setWelcomeShown()
        welcomePreferences.setWelcomeShown()

        assertTrue(
            "Repeated set calls should not flip the flag back",
            welcomePreferences.hasShownWelcome().first()
        )
    }
}
