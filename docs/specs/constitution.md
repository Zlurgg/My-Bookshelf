# Constitution

Non-negotiable architectural principles for MyBookshelf. These rules MUST be followed in all code changes.

## Core Principles

- **Offline-first**: Room database is the primary data store; works without internet
- **Privacy-focused**: No tracking, no analytics; personal data stays on device
- **Optional auth**: Google Sign-In for book clubs, not required for core bookshelf functionality
- **Guest mode**: Full bookshelf experience without sign-in

## Clean Architecture

```
┌─────────────────────┐
│  Presentation Layer │ ──────┐
└─────────────────────┘       │
                              ▼
┌─────────────────────┐     ┌─────────────────────┐
│     Data Layer      │ ───>│    Domain Layer     │
└─────────────────────┘     └─────────────────────┘
```

**Dependency Rule**: Presentation and Data depend on Domain. Domain depends on nothing.

- **Domain Layer** (`*/domain/`): Business logic, repository interfaces, UseCases, models
- **Data Layer** (`*/data/`): Repository implementations, Room entities, Ktor API, data sources
- **Presentation Layer** (`*/presentation/`): ViewModels, Compose UI, navigation

## Clean Code Principles

### DRY (Don't Repeat Yourself)
- Extract repeated logic into UseCases or utility functions
- Share domain models across features rather than duplicating
- Use composition for common ViewModel behavior
- If you copy-paste code, refactor into a shared function

### SRP (Single Responsibility Principle)
- Each class/function does ONE thing well
- UseCases: One business operation per UseCase
- ViewModels: Manage UI state for ONE screen
- Composables: Render ONE logical UI component
- Repositories: Handle ONE data source type

### Separation of Concerns

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  - UI rendering (Composables)                               │
│  - UI state management (ViewModels)                         │
│  - User input handling                                      │
│  - Navigation                                               │
│  X NO business logic, NO data access                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ depends on
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  - Business rules (UseCases)                                │
│  - Domain models (entities, value objects)                  │
│  - Repository interfaces (contracts only)                   │
│  X NO Android imports, NO UI, NO frameworks                 │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ depends on
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  - Repository implementations                               │
│  - Data sources (Room, Ktor, preferences)                   │
│  - Data mappers (Entity/DTO <-> Domain)                     │
│  X NO UI, NO business logic beyond data transformation      │
└─────────────────────────────────────────────────────────────┘
```

## Key Rules

- Domain layer has ZERO dependencies on other layers
- Presentation NEVER imports from Data layer directly
- Data layer implements Domain interfaces, never defines them
- Business logic lives in UseCases, NOT in ViewModels or Repositories
- ViewModels orchestrate UseCases, they don't contain business rules

## Layered Dependencies (Enforced)

```
UI (Composables) -> ViewModels -> UseCases -> Repositories -> DataSources
                                    ↓
                              Domain Models
```

**Critical Rules:**
- ViewModels depend on UseCases, never repositories directly
- UseCases encapsulate all business logic
- Repositories are implementation details hidden from ViewModels

## Error Handling

All fallible operations return `Result<T, DataError>`, never throw exceptions.

```kotlin
sealed interface DataError {
    sealed interface Local : DataError {
        data object NOT_FOUND : Local
        data object DATABASE_ERROR : Local
        data object UNKNOWN : Local
        // ...
    }
    sealed interface Remote : DataError {
        data object TIMEOUT : Remote
        data object NO_INTERNET : Remote
        data object SERVER_ERROR : Remote
        // ...
    }
    sealed interface Sync : DataError {
        data object NOT_AUTHENTICATED : Sync
        data object ALREADY_MEMBER : Sync
        // ...
    }
}
```

## Anti-patterns to Avoid

- X ViewModel calling repository directly (bypasses UseCase)
- X Business logic in Composables (move to ViewModel/UseCase)
- X Domain model with Android imports (keep platform-agnostic)
- X Repository doing business validation (that's UseCase's job)
- X "God class" with multiple responsibilities (split it up)
- X Using `!!` operator (use safe calls or require())
- X Manual dependency instantiation (use Koin injection)
- X Context in ViewModels (inject what you need instead)
- X LiveData in new code (use StateFlow/Flow)
- X String interpolation in logs (use varargs formatting)
- X Replicating a known-wrong pattern for "consistency" -- fix the original instead

## Dealing with Existing Violations

When you encounter an existing pattern that violates these principles:

1. **Flag it** -- don't silently copy it. Consistency with a wrong pattern is still wrong.
2. **Fix both** -- correct the original and the new code in the same change.
3. **If fixing the original is out of scope**, document it explicitly as tech debt with a clear explanation of what's wrong and what the correct approach is. Never describe a violation as "acceptable" or "consistent with existing patterns" -- that normalises the mistake for the next person who reads the code.
