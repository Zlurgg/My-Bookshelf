# Google Books API Key — Security Model

How the Google Books API key is provisioned, shipped, and enforced.

For the current key UUID, allowlisted SHA-1s, and who applied them, see [`api-key-restrictions.md`](api-key-restrictions.md). This doc is the architecture explainer; that doc is the operational record.

## Summary

The key is shipped in the APK in plaintext (via `BuildConfig`). The Android-app restriction in Cloud Console requires every Books request to carry `X-Android-Package` and `X-Android-Cert` headers — Google's edge checks them against an allowlist of (package name, cert SHA-1) pairs and 403s on mismatch.

**Important honesty caveat:** these headers are *not* cryptographically bound to anything. Google's edge takes them at face value. Anyone who possesses the full API key, the package name, and the cert SHA-1 can spoof a request from `curl` and Google will serve it. Google's own docs describe API key restrictions as best-effort, not a strong security boundary.

So the *actual* protection model has two layers:

1. **The key itself stays out of attacker hands as long as possible.** It's in `local.properties` (gitignored), never in the repo. It enters the APK at build time. As long as no APK has been distributed, the key only exists on the developer's machine.
2. **Cloud Console quota + API restrictions are the real cost backstop** once any APK with the key is in the wild (testers, Play Store). The SHA-1 check stops unsophisticated abuse (e.g. someone pasting the key into a public tutorial) but won't stop a determined attacker who decompiles a shipped APK.

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

  Pre-distribution (no APK shipped yet — current state):

    Attacker clones the public repo ──► no key (local.properties gitignored)
                                        → can't call the Books API at all

  Once any APK with the key has shipped (testers, Play Store):

    Attacker decompiles the APK ──► extracts:
                                    • full AIzaSy… key (BuildConfig const)
                                    • package name (manifest)
                                    • cert SHA-1 (keytool -printcert -jarfile)

    Attacker calls Books API from curl ──► 200 OK
    with all three headers spoofed       (Google's edge does NOT verify the
                                          cert SHA-1 against TLS evidence —
                                          the header is taken at face value)

    Attacker burns quota / runs up cost ──► bounded ONLY by Cloud Console
                                            quota caps + "Books API only"
                                            restriction. This is the real
                                            cost backstop, not the SHA-1.

  What the SHA-1 restriction actually buys you:

    Casual leak (key pasted in a public tutorial, ──► attacker without the
    Stack Overflow answer, screenshot)               SHA-1 still 403s. So
                                                     accidental sharing is
                                                     bounded.

    Determined attacker with the APK ──► SHA-1 doesn't stop them.
```

## Key insight

The API key in the APK is **not** strongly protected by anything at the protocol layer. The protection is operational: the key is gitignored, so it's only inside APKs the developer has personally built and distributed. Quota and API restrictions in Cloud Console cap the blast radius once that APK is out in the world.

The signing private key in the gitignored keystore protects the *Play Store identity* (no one can publish updates pretending to be you) and gates *some* abuse scenarios (someone re-signing a modified APK gets a different cert SHA-1 — useful if they're trying to use the key from a fork). It does **not** prevent header-spoofed `curl` requests from a determined attacker.

## Operational notes

- **Switching key restriction Cloud Console-side:** If the key is flipped from `Application restrictions = None` to `Android apps` without these headers in place, every request 403s silently — `FallbackRemoteBookDataSource` will mask the failure by falling back to OpenLibrary. Verify both ends together.
- **Missing key in a build:** `local.properties` without `GOOGLE_BOOKS_API_KEY` produces an empty `BuildConfig` field. `GoogleBooksRemoteBookDataSource` short-circuits to `PROVIDER_UNAVAILABLE` and the fallback kicks in — the app still works, just degraded.
- **Cost backstop:** Quota caps on the key in Cloud Console are the real protection against abuse. The Android restriction only stops accidental/unsophisticated misuse.
- **If quota burn becomes a concern:** the strong fix is a server-side proxy (the app calls your backend; your backend holds the real key and rate-limits per-user). Not justified at current scale.
