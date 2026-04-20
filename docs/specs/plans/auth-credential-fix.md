# Fix: Google Sign-In fails on older devices (Activity context)

## Problem

`GoogleAuthUiClient.signIn()` passes Application context to `CredentialManager.getCredential()`. Older devices require Activity context for the credential picker UI. The call fails instantly without showing the Google account picker.

Works on: OnePlus Nord CE 3 Lite (SDK 36)
Fails on: OnePlus 6T (SDK ~33)

## Root Cause

```kotlin
// GoogleAuthUiClient.kt — current
class GoogleAuthUiClient(
    private val context: Context,  // ← Application context from Koin
) : AuthService {
    override suspend fun signIn(): Result<UserData, DataError.Local> {
        val result = credentialManager.getCredential(
            request = request,
            context = context  // ← Application context, needs Activity
        )
    }
}
```

Credential fetching is a UI operation (shows a system dialog). It belongs at the presentation boundary, not buried inside a domain service.

## Solution: Credential fetch at ViewModel boundary, idToken below

The lambda stays at the ViewModel boundary. Domain layer receives `idToken: String` — clean, typed, no Android dependencies below the ViewModel.

### Architecture

```
SignInScreen (Composable)
  ├─ Gets Activity from LocalActivity.current
  ├─ Creates lambda: { fetcher.fetch(activity) }
  └─ Calls viewModel.signIn(fetchCredential)
        ↓
SignInViewModel.signIn(fetchCredential)
  ├─ Invokes lambda → gets idToken (or error)
  └─ Calls authUseCases.signIn(idToken)
        ↓
SignInUseCase.invoke(idToken: String)
  └─ authService.signIn(idToken)
        ↓
GoogleAuthUiClient.signIn(idToken: String)
  └─ Firebase sign-in only (no more credential fetching)
```

Key principle: The credential picker is a UI concern. The domain receives the result (a String), not a lambda describing how to get it.

### Changes

| File | Change |
|------|--------|
| `auth/data/service/GoogleCredentialFetcher.kt` | **New** — Koin singleton. Constructor-injected `AuthConfig` for `webClientId`. Method: `suspend fun fetch(activity: Activity): Result<String, DataError.Local>`. Handles credential exceptions. |
| `auth/domain/service/AuthService.kt` | `signIn(idToken: String)` instead of `signIn()` |
| `auth/domain/usecase/SignInUseCase.kt` | `invoke(idToken: String)` — interface signature update |
| `auth/domain/usecase/SignInUseCaseImpl.kt` | `invoke(idToken: String)` — passes string to service |
| `auth/data/service/GoogleAuthUiClient.kt` | Remove credential-fetching code, accept idToken, Firebase sign-in only |
| `auth/presentation/SignInViewModel.kt` | `signIn(fetchCredential: suspend () -> Result<String, DataError.Local>)` — invokes lambda, passes idToken to use case |
| `auth/presentation/SignInScreen.kt` | Gets Activity, creates lambda with fetcher, passes to VM |
| `auth/di/AuthModule.kt` | Add `GoogleCredentialFetcher` singleton binding, remove `AuthConfig` from `GoogleAuthUiClient` |
| Debug `DevAuthService.kt` | `signIn(idToken: String)` — see design decision below |

1 new file, 8 modified. Domain layer stays clean — `String` not `CredentialProvider`.

### Design decisions

**`DevAuthService.signIn(idToken: String)` — conscious ISP trade-off.** The debug impl ignores `idToken` because it authenticates via email/password against the emulator. A `String` parameter that one impl ignores is a minor pragmatic trade-off vs. splitting the interface or adding a `setCredential()` pre-call. Accepted because: (a) it's one parameter not a complex type, (b) the alternative adds ceremony for no functional benefit, (c) the debug impl is not shipped.

**`GoogleCredentialFetcher` is a Koin singleton, not per-call.** `AuthConfig` is injected via constructor. `Activity` is passed per-call to `fetch()`, never stored. This avoids leaking Activity references.

### Confirmed: no changes needed

- **BookcaseScreen sign-in**: `BookcaseViewModel.onAction(OnSignInClick)` sets `navigateToSignIn = true`, which navigates to `SignInScreen`. Google sign-in is only triggered from `SignInScreen`. No changes needed for BookcaseScreen.
- **`signOut()`**: Uses Application context for `CredentialManager.clearCredentialState()`. Application context works fine for this — no change needed.
- **`getSignedInUser()`**: No context needed. Unchanged.

### Edge cases

- **Null Activity**: Show error state via `_state.update`, don't silently no-op
- **Config change during sign-in**: Lambda captures Activity, but `getCredential()` is fast (system UI). Add `lifecycle.currentState.isAtLeast(RESUMED)` guard if needed
- **Double-tap**: Gate on `isLoading` state in click handler (partially done already via button enabled state)

### Why this is better than threading CredentialProvider through domain

- Domain types are domain types (`String`, `UserData`, `DataError`), not platform workarounds
- One new file instead of five
- Credential fetch is correctly treated as a UI-layer concern
- No dead typealias/interface/factory abstraction stack
- Same testability — mock the lambda in VM tests, mock `AuthService.signIn(idToken)` in UseCase tests

### Commit

`fix(auth): Use Activity context for Credential Manager on older devices`
