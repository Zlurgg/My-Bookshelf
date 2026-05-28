# Google Books API Key — Security Model

How the Google Books API key is provisioned, shipped, and enforced.

For the current key UUID, allowlisted SHA-1s, and who applied them, see [`api-key-restrictions.md`](api-key-restrictions.md). This doc is the architecture explainer; that doc is the operational record.

## Summary

The key is shipped in the APK in plaintext (via `BuildConfig`). It's safe because Google's edge enforces an Android-app restriction tied to the **package name + release signing-cert SHA-1**, sent on every request as `X-Android-Package` / `X-Android-Cert`. A leaked key can't be reused outside an APK signed with the release private key. The remaining risk (a malicious user of the published app burning quota) is bounded by quota caps and API restrictions set on the key in Cloud Console.

## Key files

| Concern | File |
|---|---|
| Build-time injection | `app/build.gradle.kts:104,118` |
| Runtime config accessor | `app/src/main/java/uk/co/zlurgg/mybookshelf/core/data/network/ApiConfig.kt:61` |
| Per-request header attachment | `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/network/api/GoogleBooksApiService.kt:46-50` |
| Package + cert SHA-1 computation | `app/src/main/java/uk/co/zlurgg/mybookshelf/core/data/service/AndroidAppAttestation.kt` |
| Missing-key fallback | `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/network/GoogleBooksRemoteBookDataSource.kt:36-40` |

## Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              BUILD TIME                                  │
└──────────────────────────────────────────────────────────────────────────┘

  local.properties          keystore.properties            Cloud Console
  (gitignored)              (gitignored)                   (Google side)
  ┌─────────────────┐       ┌──────────────────┐           ┌────────────────┐
  │ GOOGLE_BOOKS_   │       │ storeFile=…      │           │ API key:       │
  │ API_KEY=AIzaSy… │       │ storePassword=…  │           │  AIzaSy…       │
  └────────┬────────┘       │ keyAlias=…       │           │ Restrictions:  │
           │                └────────┬─────────┘           │  Android apps  │
           │                         │                     │  pkg + SHA-1   │
           │                         │ signs the APK ──────│  ↑ allowlist   │
           │                         │                     └────────────────┘
           ▼                         ▼
  ┌──────────────────────────────────────────────┐
  │             app/build.gradle.kts             │
  │  buildConfigField("GOOGLE_BOOKS_API_KEY", …) │
  └────────────────────┬─────────────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │  BuildConfig    │   ← compiled INTO the APK
              │  .GOOGLE_BOOKS_ │     (plaintext, recoverable
              │   API_KEY       │      with apktool + strings)
              └─────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                              RUNTIME (on device)                         │
└──────────────────────────────────────────────────────────────────────────┘

   ┌────────────────────────┐         ┌──────────────────────────────┐
   │   ApiConfig            │         │   AndroidAppAttestation      │
   │   .GoogleBooks.apiKey  │         │   .from(context)             │
   │     ↑                  │         │     ↑                        │
   │  BuildConfig field     │         │  PackageManager →            │
   └───────────┬────────────┘         │   • packageName              │
               │                      │   • SHA-1(signing cert)      │
               │                      └──────────────┬───────────────┘
               │                                     │
               ▼                                     ▼
   ┌─────────────────────────────────────────────────────────────────┐
   │  GoogleBooksApiService.attachCredentials()                      │
   │                                                                 │
   │    GET https://www.googleapis.com/books/v1/volumes?q=…          │
   │    X-Goog-Api-Key:    AIzaSy…                                   │
   │    X-Android-Package: uk.co.zlurgg.mybookshelf                  │
   │    X-Android-Cert:    AB12CD34…  (uppercase hex, no colons)     │
   └─────────────────────────────────┬───────────────────────────────┘
                                     │
                                     ▼
   ┌─────────────────────────────────────────────────────────────────┐
   │                    Google API edge (server-side)                │
   │                                                                 │
   │  1. Look up key AIzaSy…                                         │
   │  2. Key has "Android apps" restriction → require headers        │
   │  3. Check (X-Android-Package, X-Android-Cert) ∈ allowlist       │
   │       ✓  → forward to Books backend  → 200 OK                   │
   │       ✗  → 403 PERMISSION_DENIED                                │
   └─────────────────────────────────┬───────────────────────────────┘
                                     │
                  ┌──────────────────┴──────────────────┐
                  ▼                                     ▼
            200 OK + JSON                       403 → maps to
            → mapped to BookSearchResponse      PROVIDER_UNAVAILABLE
                                                → FallbackRemoteBookDataSource
                                                  silently falls back to
                                                  OpenLibrary

┌──────────────────────────────────────────────────────────────────────────┐
│                            THREAT MODEL                                  │
└──────────────────────────────────────────────────────────────────────────┘

  Attacker extracts key from APK ──► curl with X-Goog-Api-Key only
                                     → 403 (no pkg/cert headers)

  Attacker forges all 3 headers from curl ──► 403
                                     (edge cross-checks cert SHA-1
                                      against signed APKs seen for
                                      that package — can't be spoofed
                                      without the signing private key)

  Attacker re-signs modified APK ──► cert SHA-1 changes → 403

  User of YOUR signed APK proxies ──► works, BUT bounded by
  requests through their device       quota caps + API restrictions
                                      set on the key in Cloud Console
                                      (the real cost backstop)
```

## Key insight

The API key in the APK is intentionally **not** the secret. The secret is the release signing private key (in the gitignored keystore). Google's edge indirectly proves the caller possesses that private key by validating the cert SHA-1 against an allowlist tied to the package name.

## Operational notes

- **Switching key restriction Cloud Console-side:** If the key is flipped from `Application restrictions = None` to `Android apps` without these headers in place, every request 403s silently — `FallbackRemoteBookDataSource` will mask the failure by falling back to OpenLibrary. Verify both ends together.
- **Missing key in a build:** `local.properties` without `GOOGLE_BOOKS_API_KEY` produces an empty `BuildConfig` field. `GoogleBooksRemoteBookDataSource` short-circuits to `PROVIDER_UNAVAILABLE` and the fallback kicks in — the app still works, just degraded.
- **Cost backstop, not the access control:** Quota caps on the key in Cloud Console are what bound abuse from real installed users; the Android restriction only stops off-device abuse.
