# Compose Screen Pattern

Patterns for implementing screens with Jetpack Compose.

## Root/Presenter Pattern

Every screen has two composables:

1. **ScreenRoot** - Handles ViewModel, navigation, side effects
2. **Screen** - Pure UI, receives state and callbacks

```kotlin
// BookcaseScreenRoot.kt - Handles ViewModel and navigation
@Composable
fun BookcaseScreenRoot(
    viewModel: BookcaseViewModel = koinViewModel(),
    onBookshelfClick: (Bookshelf) -> Unit,
    onBookDetailClick: (String, String) -> Unit,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BookcaseScreen(
        state = state,
        onAction = viewModel::onAction,
        onBookshelfClick = onBookshelfClick,
        onBookDetailClick = onBookDetailClick
    )
}

// BookcaseScreen.kt - Pure UI
@Composable
fun BookcaseScreen(
    state: BookcaseState,
    onAction: (BookcaseAction) -> Unit,
    onBookshelfClick: (Bookshelf) -> Unit,
    onBookDetailClick: (String, String) -> Unit
) {
    Scaffold(
        topBar = { /* ... */ },
        floatingActionButton = { /* ... */ }
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator()
        } else {
            BookcaseContent(
                bookshelves = state.bookshelves,
                onBookshelfClick = onBookshelfClick,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
```

### Why This Pattern?

- **Testability**: Pure Screen can be tested in isolation with preview
- **Separation**: ViewModel logic stays out of UI composition
- **Reusability**: Screen can be reused with different data sources
- **Navigation**: Root handles navigation callbacks cleanly

## State Hoisting

Pass state down, events up:

```kotlin
@Composable
fun BookshelfRow(
    bookshelf: Bookshelf,
    bookCount: Int,
    onShelfClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

## Side Effects

Use appropriate effect handlers:

```kotlin
@Composable
fun BookcaseScreenRoot(...) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // React to navigation events
    LaunchedEffect(state.navigateToSignIn) {
        if (state.navigateToSignIn) {
            onSignIn()
            viewModel.onAction(BookcaseAction.ResetNavigateToSignIn)
        }
    }

    // React to operation success
    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            // Show snackbar, etc.
            viewModel.onAction(BookcaseAction.ResetOperationState)
        }
    }
}
```

## Preview Support

Design screens to be previewable:

```kotlin
@Preview(showBackground = true)
@Composable
private fun BookcaseScreenPreview() {
    MyBookshelfTheme {
        BookcaseScreen(
            state = BookcaseState(
                bookshelves = bookshelves(), // from preview package
                isLoading = false
            ),
            onAction = {},
            onBookshelfClick = {},
            onBookDetailClick = { _, _ -> }
        )
    }
}
```

## Component Extraction

Extract reusable components to `components/` package:

```
bookcase/presentation/
├── BookcaseScreenRoot.kt
├── BookcaseScreen.kt
├── BookcaseViewModel.kt
├── BookcaseState.kt
├── BookcaseAction.kt
├── handlers/
│   ├── ShelfOperationsHandler.kt
│   └── ShelfManagementHandler.kt
└── components/
    ├── BookshelfRow.kt
    ├── SettingsMenu.kt
    ├── AddShelfDialog.kt
    └── RenameShelfDialog.kt
```

## Navigation Setup

```kotlin
NavHost(navController, startDestination = Route.Bookcase) {
    composable<Route.Bookcase> {
        BookcaseScreenRoot(
            onBookshelfClick = { shelf ->
                navController.navigate(Route.Bookshelf(shelf.id))
            },
            onBookDetailClick = { shelfId, bookId ->
                navController.navigate(Route.BookDetail(bookId, shelfId))
            }
        )
    }
}
```

## Accessibility

Follow Material 3 accessibility guidelines:

- Minimum touch target: 48dp
- Content descriptions on all interactive elements
- Semantic grouping for screen readers
