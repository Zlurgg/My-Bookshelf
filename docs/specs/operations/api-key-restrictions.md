# API Key Restrictions — Record

Cloud Console state is not source-controlled. This file is the durable record of which API keys exist, what they restrict to, and who applied the restriction. Update it whenever a key, a SHA-1, or an API allowlist entry changes.

For how the Android-app restriction mechanism actually works (build-time injection, runtime headers, Google's edge check, threat model), see [`google-books-api-key-security.md`](google-books-api-key-security.md).

## Project

- **Firebase / GCP project:** `my-bookshelf-c1e91`
- **Project number:** `1080842261600`
- **Console:** https://console.cloud.google.com/apis/credentials?project=my-bookshelf-c1e91

## Keys in this project

### Google Books API key

Baked into the APK at build time via `local.properties → GOOGLE_BOOKS_API_KEY → BuildConfig.GOOGLE_BOOKS_API_KEY`, sent on every Books API request as the `X-Goog-Api-Key` header (see `GoogleBooksApiService`).

| Field | Value |
| --- | --- |
| Cloud Console key UUID | `95d5837c-ab12-4753-b72e-ab9550512b6e` |
| Used by | `GoogleBooksApiService` (every Books search + volume lookup) |
| Application restriction | **Android apps** |
| API restriction | **Books API** only |
| Applied by | Zlurgg |
| Applied on | 2026-05-26 |

**Allowed Android-app entries** (all on `uk.co.zlurgg.mybookshelf`):

| Purpose | SHA-1 | Source |
| --- | --- | --- |
| Debug builds (`./gradlew assembleDebug`, Android Studio Run) | `87:8D:01:48:21:6E:86:3D:EA:E1:27:9A:C1:BD:AB:B6:E0:16:20:DB` | `~/.android/debug.keystore` (shared with anyone holding the repo) |
| Local release builds (`./gradlew assembleRelease`, sideloaded testing) | `9F:2F:9F:7A:F4:EA:E8:33:B9:CD:3F:B2:9A:2B:59:3D:69:EB:BC:C0` | `~/StudioProjects/android-keystores/zlurgg-release-key.keystore` |
| Play Store distribution (Play App Signing) | `12:DC:FB:87:D6:FF:52:5A:C6:B7:59:96:35:04:2B:52:16:F7:10:D4` | Play Console (Google-managed) — captured 2026-05-31 after the first v1.0.0 internal-track upload |

**Risk accepted:** anyone with the debug keystore (in practice, anyone with the repo plus the standard Android debug keystore) can use this key against the project quota. At current scale (closed testing, small cohort, modest quota), that's fine.

**Revisit if any of these change:**
- The team grows beyond one developer.
- The debug keystore moves off the local machine (e.g. CI starts producing debug builds with a different keystore).
- Quota or billing on the key becomes non-trivial.

Then switch to a separate `GOOGLE_BOOKS_API_KEY_DEBUG` (wired via the `debug` block of `app/build.gradle.kts`), restrict each key by its respective SHA-1. See `closed-testing-release-prep.md` Phase 3.1 for the upgrade path.

### Firebase auto-generated Android key (`AIzaSyB8HO9fVtXulCsXIhu6Jl0JcMhLq4xwjyI`)

Auto-managed by the `google-services` Gradle plugin from `google-services.json` (the `current_key` field). Used by Firebase Auth (`signInWithCredential` → Identity Toolkit), Firestore, Crashlytics SDK reporting, and the rest of the Firebase Android SDK bundle (24 APIs at time of writing). Listed in Cloud Console as **"New Android key (auto created by Firebase)"**.

Application restriction is **Android apps**, with its OWN allowlist independent from the Books API key. Cloud Console enforces it on every SDK call — if the calling app's `{package, signing-cert SHA-1}` isn't on the list, Firebase backend rejects with:

> `Requests from this Android client application <package> are blocked.`

| Field | Value |
| --- | --- |
| API key value | `AIzaSyB8HO9fVtXulCsXIhu6Jl0JcMhLq4xwjyI` |
| Used by | Firebase Android SDK (Auth, Firestore, Crashlytics reporting, etc.) |
| Application restriction | **Android apps** |
| API restriction | (None — required for Firebase SDK breadth) |

**Allowed Android-app entries** (all on `uk.co.zlurgg.mybookshelf`) — must include every signing cert that will ever ship the app:

| Purpose | SHA-1 | Source |
| --- | --- | --- |
| Debug builds | `87:8D:01:48:21:6E:86:3D:EA:E1:27:9A:C1:BD:AB:B6:E0:16:20:DB` | `~/.android/debug.keystore` |
| Local release builds (sideloaded testing) | `9F:2F:9F:7A:F4:EA:E8:33:B9:CD:3F:B2:9A:2B:59:3D:69:EB:BC:C0` | `~/StudioProjects/android-keystores/zlurgg-release-key.keystore` |
| Play Store distribution (Play App Signing) | `12:DC:FB:87:D6:FF:52:5A:C6:B7:59:96:35:04:2B:52:16:F7:10:D4` | Play Console (Google-managed) — added 2026-05-31 after Firebase Auth started failing on the v1.0.0 Play install |

Do **not** restrict to specific APIs — Firebase needs the full bundle. Do **not** delete this key — `google-services` will not regenerate it; you'd have to recreate the Firebase Android app to fix.

### Browser key (auto created by Firebase) — `AIzaSy…` (different value)

Auto-created alongside the Android key. Unused by the Android SDK at runtime; intended for web-SDK usage in projects that have a web app. **No restrictions, no allowlist** — and that's fine because the Android app doesn't touch it. Don't add an Android-app restriction here; it'd break web access for any future web client without helping the app.

## ⚠ Adding a new signing cert? Update BOTH Android-app allowlists

Every new signing cert (debug, release, Play App Signing, alternative tracks, etc.) needs adding to **two separate Cloud Console allowlists**:

1. **Books API key** (`95d5837c-…`) — or Books search falls back to OL
2. **Firebase auto-generated Android key** (`AIzaSyB8H…`) — or Firebase Auth/Firestore is blocked

Plus, for Google Sign-In OAuth provisioning:

3. **Firebase Console → Project settings → Android app → SHA certificate fingerprints** — Firebase creates an Android OAuth client + updates `google-services.json`

Three independent gates, all keyed on `{package, SHA-1}`, all must be in sync per signing cert. There is no UI that updates them together — easy to forget one and get a partial failure (we hit this with v1.0.0: Books worked on Play install but Firebase Auth was blocked because step 2 was missing).

## How to get the Play App Signing SHA-1

In the current Play Console UI: **Protected with Play → Play Store distribution → App signing**.

Direct URL (this project): https://play.google.com/console/u/0/developers/7719378280506600473/app/4972722619296640599/keymanagement

Copy the **SHA-1 certificate fingerprint** (20 hex pairs, colon-separated, uppercase). Paste into Cloud Console → Credentials → [Books API key] → Application restrictions → Android apps → ADD AN ITEM with package `uk.co.zlurgg.mybookshelf`.

Note: Google's "App signing" location moves between Console UI revisions. If the path above 404s in the future, search "App signing" in the Play Console top search bar.

## How to get the release-keystore SHA-1

```
keytool -list -v \
  -keystore ~/StudioProjects/android-keystores/zlurgg-release-key.keystore \
  -alias zlurgg
```

Enter the `storePassword` from `keystore.properties` when prompted. Copy the `SHA1:` line (20 hex pairs, 59 chars, colon-separated, uppercase). Paste into Cloud Console > Credentials > [Books API key] > Application restrictions > Android apps > ADD AN ITEM.

## How to get the debug-keystore SHA-1

```
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android -keypass android
```

(Debug keystore password is the fixed Android default `android`; safe to put inline.)

## Verification after applying restrictions

Rebuild both a debug and a release APK; in each, run a Books search.

- **If Google attribution shows on results** → key + restriction working.
- **If every search falls back to OpenLibrary** → restriction is wrong for that variant. Check the SHA-1 on the allowlist matches what the build is actually signed with. Once Crashlytics is live (`closed-testing-release-prep.md` Phase 2.1), the blank-key/403 path will surface as a non-fatal in the Firebase console.
