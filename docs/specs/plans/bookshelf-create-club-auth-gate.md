# Plan: Gate "Create Book Club" on Sign-In Status at BookshelfScreen

## Context

When a guest user clicks "Create Book Club" on the BookshelfScreen, they confirm a dialog, get navigated back to BookcaseScreen, and only *then* see a "sign in required" dialog. This is jarring — the user gets bounced to a different screen for no reason.

**Goal:** Show the sign-in required dialog immediately on the BookshelfScreen, before any navigation occurs. If signed in, the existing flow continues unchanged.

## Approach

Add `isSignedIn` to `BookshelfState`, checked by `BookshelfViewModel` on init via `CheckSignInStatusUseCase`. The screen gates the FAB click: signed-in users see the existing confirmation dialog, guests see `SignInRequiredDialog` with an `onSignIn` callback that navigates to sign-in.

This matches the established pattern: `BookcaseViewModel` checks auth on init via `AuthUseCases` and exposes `isSignedIn` in `BookcaseState`. The cross-feature dependency (`bookshelf` → `auth/domain/usecase/CheckSignInStatusUseCase`) is the same kind as the existing `bookshelf` → `bookcase/domain/usecase/GetShelfByIdUseCase`.

### Decisions

- **Pattern consistency over minimal footprint**: Using the ViewModel approach (like BookcaseScreen) avoids the staleness, first-frame race, and pattern divergence that a composable-parameter approach would introduce.
- **`onSignIn` callback stays in the nav layer**: BookshelfScreen receives `onSignIn: () -> Unit` — it doesn't know what sign-in means, just fires the callback. The nav graph wires it to `navigateToSignIn()`.
- **Bookcase downstream check kept as defense-in-depth**: The `BookcaseScreen` `LaunchedEffect` (lines 103-115) that checks `isSignedIn` when `createClubForShelfId` arrives becomes unreachable for the bookshelf→bookcase flow (guests never trigger `onCreateBookClub`). Kept intentionally as a safety net against future paths that might set `createClubForShelfId` without the early gate.
- **DRY**: Extract `navigateToSignIn(navController)` helper in `MyBookShelfApp.kt` to eliminate the duplicated nav lambda.

## Implementation

### 1. BookshelfState.kt — Add isSignedIn field

```kotlin
data class BookshelfState(
    ...
    val isSignedIn: Boolean = false   // fail-closed default
)
```

### 2. BookshelfViewModel.kt — Check auth on init

Add `CheckSignInStatusUseCase` constructor parameter. Call on init alongside existing setup:

```kotlin
class BookshelfViewModel(
    private val bookshelfUseCases: BookshelfUseCases,
    private val getShelfById: GetShelfByIdUseCase,
    private val bookClubOperations: ClubOperations,
    private val checkSignInStatus: CheckSignInStatusUseCase,
    private val shelfId: String
) : ViewModel() {

    init {
        observeDebouncedQuery()
        loadBooks()
        loadShelfDetails()
        checkSignInStatus()
    }

    private fun checkSignInStatus() {
        viewModelScope.launch {
            val isSignedIn = checkSignInStatus.invoke()
            _state.update { it.copy(isSignedIn = isSignedIn) }
        }
    }
}
```

### 3. BookshelfModule.kt — Add UseCase to ViewModel DI

```kotlin
viewModel { (shelfId: String) ->
    BookshelfViewModel(
        bookshelfUseCases = get(),
        getShelfById = get(),
        bookClubOperations = get(),
        checkSignInStatus = get(),
        shelfId = shelfId
    )
}
```

### 4. BookshelfScreen.kt — Gate FAB click and show dialog

**`BookshelfScreenRoot`** — add `onSignIn` parameter (no default, compiler-enforced):

```kotlin
fun BookshelfScreenRoot(
    viewModel: BookshelfViewModel = koinViewModel(),
    onAddBookClick: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onBackClick: () -> Unit,
    onCreateBookClub: () -> Unit = {},
    onSignIn: () -> Unit,
    shelfName: String? = null,
    shelfMaterial: ShelfMaterial? = null,
)
```

Pass through to `BookshelfScreen`:
```kotlin
BookshelfScreen(
    state = uiState,
    onAction = { ... },
    onSignIn = onSignIn,
)
```

**`BookshelfScreen`** — add `onSignIn` parameter, gate FAB, add dialog:

```kotlin
fun BookshelfScreen(
    state: BookshelfState,
    onAction: (BookshelfAction) -> Unit,
    onSignIn: () -> Unit = {},
)
```

```kotlin
var showSignInRequiredDialog by remember { mutableStateOf(false) }

// FAB onClick:
onClick = {
    if (state.isSignedIn) {
        showCreateBookClubDialog = true
    } else {
        showSignInRequiredDialog = true
    }
}
```

Add `SignInRequiredDialog` after the existing confirmation dialog (reuses same strings as BookcaseScreen):
```kotlin
if (showSignInRequiredDialog) {
    SignInRequiredDialog(
        title = stringResource(R.string.sign_in_required_book_clubs_title),
        message = stringResource(R.string.sign_in_required_book_clubs_message),
        onSignIn = {
            showSignInRequiredDialog = false
            onSignIn()
        },
        onDismiss = { showSignInRequiredDialog = false }
    )
}
```

### 5. MyBookShelfApp.kt — Extract helper, wire onSignIn

Extract duplicated sign-in navigation:
```kotlin
private fun navigateToSignIn(navController: NavHostController) {
    navController.navigate(NavigationRoute.SignIn.createRoute()) {
        popUpTo(NavigationRoute.MyBookshelfGraph.ROUTE) { inclusive = true }
    }
}
```

Update both Bookcase and Bookshelf composable blocks to use the helper. Pass `onSignIn` to BookshelfScreenRoot:

```kotlin
BookshelfScreenRoot(
    ...
    onSignIn = { navigateToSignIn(navController) },
    ...
)
```

## Files changed

| File | Change |
|------|--------|
| `bookshelf/presentation/BookshelfState.kt` | Add `isSignedIn: Boolean = false` |
| `bookshelf/presentation/BookshelfViewModel.kt` | Add `CheckSignInStatusUseCase` dep, check on init |
| `bookshelf/presentation/BookshelfScreen.kt` | Add `onSignIn` param, gate FAB click, add `SignInRequiredDialog` |
| `bookshelf/di/BookshelfModule.kt` | Pass `checkSignInStatus = get()` to ViewModel |
| `app/presentation/MyBookShelfApp.kt` | Extract `navigateToSignIn` helper, pass `onSignIn` to BookshelfScreenRoot |

## Files NOT changed

- `BookshelfAction.kt` — no new actions
- `BookcaseScreen.kt` — downstream `LaunchedEffect` kept as defense-in-depth (see Decisions)

## Architecture compliance

- **Cross-feature dependency**: `bookshelf` → `auth/domain/usecase/CheckSignInStatusUseCase` (domain interface). Same pattern as existing `bookshelf` → `bookcase/domain/usecase/GetShelfByIdUseCase`.
- **Screen imports `SignInRequiredDialog`**: Same as `BookcaseScreen` already does.
- **Pattern consistency**: Matches `BookcaseViewModel.checkSignInStatus()` → `BookcaseState.isSignedIn` exactly.
- **Fail-closed**: `isSignedIn` defaults to `false` in state — if the check fails, guests see the sign-in dialog.

## Verification

1. `./gradlew :app:compileDebugKotlin` — builds
2. `./gradlew :app:detekt` — no lint violations
3. `./gradlew :app:testDebugUnitTest` — existing tests pass
4. Manual: open shelf as guest → click Groups FAB → sign-in dialog appears immediately
5. Manual: open shelf signed in → click Groups FAB → confirmation dialog → creates club (existing flow)
