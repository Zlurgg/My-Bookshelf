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
| Play Store distribution (Play App Signing) | `<TODO after first Play upload — see Play Console → Setup → App integrity → App signing key certificate → SHA-1>` | Play Console (Google-managed) |

**Risk accepted:** anyone with the debug keystore (in practice, anyone with the repo plus the standard Android debug keystore) can use this key against the project quota. At current scale (closed testing, small cohort, modest quota), that's fine.

**Revisit if any of these change:**
- The team grows beyond one developer.
- The debug keystore moves off the local machine (e.g. CI starts producing debug builds with a different keystore).
- Quota or billing on the key becomes non-trivial.

Then switch to a separate `GOOGLE_BOOKS_API_KEY_DEBUG` (wired via the `debug` block of `app/build.gradle.kts`), restrict each key by its respective SHA-1. See `closed-testing-release-prep.md` Phase 3.1 for the upgrade path.

### Firebase auto-generated key (`AIzaSyB8H…`)

Auto-managed by the `google-services` Gradle plugin from `google-services.json`. Used by Firebase Auth, Firestore, and other Firebase SDK calls. Do **not** restrict it to "Books API only" — that would break sign-in.

Typically hidden from the default Credentials list view (scroll or remove filters to see it). No manual action required unless Firebase docs change the recommendation.

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
