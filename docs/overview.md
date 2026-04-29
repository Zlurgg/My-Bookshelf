# Project Overview

MyBookshelf is a personal bookshelf organizer Android application built with Kotlin and Jetpack Compose.

## Quick Start

```bash
# Build
./gradlew assembleDebug

# Run tests
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Integration tests

# Code quality
./gradlew detekt                 # Static analysis

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

## Package Structure

```
uk.co.zlurgg.mybookshelf/
├── app/                    # Application setup and navigation
├── di/                     # Root DI aggregator
├── core/                   # Shared infrastructure
│   ├── data/              # Network, database, services
│   └── domain/            # Error types, Result pattern
├── auth/                   # Authentication feature
├── sync/                   # Cloud sync feature
└── bookshelf/             # Main bookshelf feature
    ├── data/              # Room entities, Ktor API, repositories
    ├── domain/            # Book/Shelf models, UseCases
    └── presentation/      # Screens, ViewModels, components
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Clean Architecture |
| DI | Koin |
| Database | Room |
| Network | Ktor |
| Image Loading | Coil 3 |
| Auth | Firebase Auth, Credential Manager API |
| Cloud Sync | Firestore |
| Background Work | WorkManager |
| Logging | Timber |
| Testing | JUnit 4, Robolectric |
| Static Analysis | Detekt |

## Build Configuration

- Target SDK: 36
- Min SDK: 28
- Java: 11
- R8/ProGuard enabled for release
- Room schema version: 1

## Source Sets

```
app/src/
├── main/     # Shared code
├── debug/    # Firebase emulator auth
└── release/  # No-op stubs
```

## Koin Modules

- `CoreModule` - Infrastructure (database, network, preferences)
- `AuthModule` - Authentication
- `SyncModule` - Cloud sync
- `BookshelfModule` - Main feature
- `BookClubModule` - Book club feature

## Navigation

```
Bookcase (root)
    └── Bookshelf/{id}
            └── BookDetail/{id}/{shelfId}
```

## Documentation

| Document | Purpose |
|----------|---------|
| `docs/specs/constitution.md` | Architectural principles |
| `docs/specs/style/code-style.md` | Naming conventions, testing |
| `docs/specs/patterns/` | Implementation patterns |
| `CLAUDE.local.md` | AI assistant guidance |
