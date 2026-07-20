# Dependency & Release Updates

How to bump dependencies and cut a Play Store release. Documents the **process and constraints**, not the current version state — for versions, read `gradle/libs.versions.toml`.

## Checking for latest versions

Latest stable versions live in each artifact's `maven-metadata.xml`:

- **Google Maven** (`androidx.*`, `com.google.firebase`, `com.google.gms`, AGP, `com.google.android.*`):
  `https://dl.google.com/dl/android/maven2/<group/as/path>/<artifact>/maven-metadata.xml`
- **Maven Central** (Kotlin, Koin, Ktor, Coil, kotlinx, KSP, detekt, Robolectric, Timber, qrose):
  `https://repo1.maven.org/maven2/<group/as/path>/<artifact>/maven-metadata.xml`

Filter out pre-releases (`alpha`, `beta`, `-rc`, `-dev`, `-eap`, `-M#`, `SNAPSHOT`) and take the highest remaining `<version>` (`sort -V`).

## Compatibility constraints (the gotchas)

- **KSP is pinned to Kotlin's major.minor.** KSP's version tracks the Kotlin line (e.g. Kotlin 2.3.x ↔ KSP 2.3.x). Room's compiler runs through KSP, so **Kotlin cannot move ahead of the latest published KSP.** Before bumping Kotlin, confirm a matching KSP exists — otherwise hold Kotlin back. "Latest stable" means latest *mutually compatible*, not latest-in-isolation.
- **compileSdk floor.** AndroidX libraries declare a minimum `compileSdk` via AAR metadata; a bump can fail `checkReleaseAarMetadata` demanding a higher `compileSdk`. Raising `compileSdk` is compile-time only and independent of `targetSdk` (which gates runtime behavior and has Play policy implications) — bump `compileSdk` to satisfy the libs, leave `targetSdk` unless intentionally opting into new runtime behavior.
- **Compose compiler** ships with the Kotlin Compose plugin (versioned by `kotlin`), separate from the Compose **BOM** (UI artifacts) — they move independently.
- **Gradle ↔ AGP.** Keep the Gradle wrapper (`gradle/wrapper/gradle-wrapper.properties`) within AGP's supported range.

## Verify (the CI gates)

Run the same checks CI runs, in order:

```bash
./gradlew test        # unit tests
./gradlew detekt      # static analysis
./gradlew bundleRelease   # signed AAB — catches R8/ProGuard breakage from lib bumps
```

`bundleRelease` needs `keystore.properties` and `local.properties` (with `GOOGLE_BOOKS_API_KEY`) present locally.

## Versioning

- Play requires each upload's `versionCode` to exceed **any** previously uploaded code across **all** tracks. Bump `versionCode` in `app/build.gradle.kts` every release.
- `versionName` is the public string. Also update the two references in `docs/index.html` (badge + app-info) to match.

## Cutting a release

The GitHub Action `.github/workflows/release.yml` builds the AAB (running `test` + `detekt` first) and uploads via `r0adkll/upload-google-play` as **`status: draft`** — never auto-published, always reviewed/rolled out manually in Play Console.

- **Tag push** (`git push origin v*`) → uploads to the **`internal`** track.
- **Manual dispatch** (`gh workflow run release.yml -f track=<internal|alpha|beta|production>`) → uploads to the chosen track. Closed testing = `alpha`, open testing = `beta`.

Promotion from one track to production is done in the Play Console, not by a new upload.
