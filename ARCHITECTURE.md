# Architecture

My Bookshelf is a personal bookshelf organizer built with Kotlin and Jetpack Compose.

## Quick Start

```bash
# Build
./gradlew build

# Run tests
./gradlew testDebugUnitTest      # 542 unit tests (~60s)
./gradlew connectedAndroidTest   # 55 integration tests (~25s)

# Code quality
./gradlew detekt                 # Static analysis
./gradlew lintDebug              # Android lint

# Release
./gradlew assembleRelease
```

## Firebase Emulator (Debug Builds)

Debug builds auto-connect to local Firebase emulators.

```bash
# Start emulators
firebase emulators:start

# With data persistence
firebase emulators:start --export-on-exit=./emulator-data --import=./emulator-data
```

- **Emulator UI**: http://localhost:4000
- Debug builds connect to `10.0.2.2` (Android emulator's localhost alias)
- Release builds connect to production Firebase

## Architecture

### Clean Architecture Layers

```
Presentation (ViewModels, Compose UI)
        ↓
    Domain (UseCases, Entities, Repository Interfaces)
        ↓
    Data (Room, Ktor, Repository Implementations)
```

**Key principle**: ViewModels depend only on UseCases, never repositories.

### Package Structure

```
uk.co.zlurgg.mybookshelf/
├── app/                    # Application setup and navigation
├── di/                     # Root DI aggregator
├── core/                   # Shared infrastructure
│   ├── data/              # Network, database, services
│   └── domain/            # Error types, Result pattern
├── auth/                   # Authentication feature
├── sync/                   # Cloud sync feature
├── update/                 # In-app update feature
└── bookshelf/             # Main bookshelf feature
    ├── data/              # Room entities, Ktor API, repositories
    ├── domain/            # Book/Shelf models, UseCases
    └── presentation/      # Screens, ViewModels, components
```

### Key Patterns

- **Repository Pattern**: Interfaces in domain, implementations in data
- **UseCase Pattern**: 50 UseCases encapsulating business logic
- **Result Pattern**: `Result<T, DataError>` for error handling
- **State Management**: ViewModel + StateFlow, unidirectional data flow
- **Mapper Pattern**: DTOs ↔ Entities ↔ Domain models

### Dependency Injection (Koin)

Feature-scoped modules:
- `CoreModule` - Infrastructure (database, network, preferences)
- `AuthModule` - Authentication
- `SyncModule` - Cloud sync
- `UpdateModule` - In-app updates
- `BookshelfModule` - Main feature
- `BookClubModule` - Book club feature

## Tech Stack

| Component | Technology |
|-----------|------------|
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Clean Architecture |
| DI | Koin 4.1.1 |
| Database | Room 2.8.0 |
| Network | Ktor 3.3.0 |
| Image Loading | Coil 3 |
| Auth | Firebase Auth, Google Sign-In |
| Cloud Sync | Firestore |
| Logging | Timber |
| Testing | JUnit 4, Robolectric |
| Static Analysis | Detekt, ktlint |

## Build Configuration

- Android SDK: Target 36, Min 28
- Kotlin: 2.2.20
- ProGuard enabled for release
- Room schema version: 8

## Navigation

```
Bookcase (root)
    └── Bookshelf/{id}
            └── BookDetail/{id}/{shelfId}
```

## Metrics

- 286 Kotlin source files
- 597 tests (542 unit + 55 integration)
- 50 UseCases across 10 domains
- 6 ViewModels, 9 Repositories
