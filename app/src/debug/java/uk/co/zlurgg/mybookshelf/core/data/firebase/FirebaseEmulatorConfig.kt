package uk.co.zlurgg.mybookshelf.core.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.BuildConfig
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Configures Firebase services to use local emulators in debug builds.
 *
 * Call [configureEmulators] once at app startup before any Firebase operations.
 *
 * Emulator URLs:
 * - Firestore UI: http://localhost:4000/firestore
 * - Auth UI: http://localhost:4000/auth
 *
 * Start emulators with: firebase emulators:start
 */
object FirebaseEmulatorConfig {

    private const val TAG = "FirebaseEmulator"

    // Configurable via local.properties: firebase.emulator.host=192.168.1.x
    // Defaults to 10.0.2.2 (Android emulator's localhost alias)
    private val EMULATOR_HOST = BuildConfig.FIREBASE_EMULATOR_HOST
    private const val FIRESTORE_PORT = 8080
    private const val AUTH_PORT = 9099
    private const val CONNECTION_TIMEOUT_MS = 2000

    @Volatile
    private var isConfigured = false

    /**
     * Configures Firebase to use local emulators in debug builds.
     * Safe to call multiple times - only configures once.
     *
     * In release builds, this is a no-op.
     */
    fun configureEmulators() {
        if (!BuildConfig.DEBUG) {
            return
        }

        if (isConfigured) {
            Timber.tag(TAG).d("Emulators already configured, skipping")
            return
        }

        synchronized(this) {
            if (isConfigured) return

            try {
                Timber.tag(TAG).d("=== CONFIGURING FIREBASE EMULATORS ===")

                // Configure Firestore emulator
                FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
                Timber.tag(TAG).d("Firestore emulator: %s:%d", EMULATOR_HOST, FIRESTORE_PORT)

                // Configure Auth emulator
                FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, AUTH_PORT)
                Timber.tag(TAG).d("Auth emulator: %s:%d", EMULATOR_HOST, AUTH_PORT)

                isConfigured = true
                Timber.tag(TAG).d("=== EMULATORS CONFIGURED ===")
                Timber.tag(TAG).d("View data at: http://localhost:4000")

                // Check if emulators are actually running (async, non-blocking)
                checkEmulatorConnectivity()
            } catch (e: IllegalStateException) {
                // Emulator already configured (can happen if getInstance was called elsewhere first)
                Timber.tag(TAG).w("Emulator config failed (may already be set): %s", e.message)
                isConfigured = true
            }
        }
    }

    /**
     * Checks if the Firebase emulators are reachable and logs a warning if not.
     * Runs asynchronously to avoid blocking app startup.
     */
    private fun checkEmulatorConnectivity() {
        CoroutineScope(Dispatchers.IO).launch {
            val firestoreReachable = isPortReachable(EMULATOR_HOST, FIRESTORE_PORT)
            val authReachable = isPortReachable(EMULATOR_HOST, AUTH_PORT)

            if (!firestoreReachable || !authReachable) {
                Timber.tag(TAG).e("╔════════════════════════════════════════════════════════════╗")
                Timber.tag(TAG).e("║  ⚠️  FIREBASE EMULATORS NOT RUNNING!                        ║")
                Timber.tag(TAG).e("╠════════════════════════════════════════════════════════════╣")
                Timber.tag(TAG).e("║  Firebase operations will FAIL in this debug build.        ║")
                Timber.tag(TAG).e("║                                                            ║")
                Timber.tag(TAG).e("║  Start emulators with:                                     ║")
                Timber.tag(TAG).e("║    firebase emulators:start                                ║")
                Timber.tag(TAG).e("║                                                            ║")
                Timber.tag(TAG).e("║  Then restart the app.                                     ║")
                Timber.tag(TAG).e("╚════════════════════════════════════════════════════════════╝")

                if (!firestoreReachable) {
                    Timber.tag(TAG).e("Firestore emulator NOT reachable at %s:%d", EMULATOR_HOST, FIRESTORE_PORT)
                }
                if (!authReachable) {
                    Timber.tag(TAG).e("Auth emulator NOT reachable at %s:%d", EMULATOR_HOST, AUTH_PORT)
                }
            } else {
                Timber.tag(TAG).d("✓ Emulators are running and reachable")
            }
        }
    }

    /**
     * Attempts to connect to a host:port to check if it's reachable.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun isPortReachable(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns true if running against emulators (debug build).
     */
    fun isUsingEmulators(): Boolean = BuildConfig.DEBUG && isConfigured
}
