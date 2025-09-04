# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

My Bookshelf is a social reading app built with Kotlin and Jetpack Compose that lets users create, customize, and share their bookshelves, reading setups, and cozy corners. Features include drag-and-drop shelf building, book search via Open Library/Google Books APIs, affiliate link integration for monetization, public shelf sharing, and reading nook galleries.

## Common Commands

### Build & Development
- **Build the project**: `./gradlew build`
- **Clean build**: `./gradlew clean build`
- **Install debug APK**: `./gradlew installDebug`

### Testing
- **Run all tests**: `./gradlew test`
- **Run unit tests only**: `./gradlew testDebugUnitTest`
- **Run single test**: `./gradlew testDebugUnitTest --tests "*TestClassName*"`
- **Run connected Android tests**: `./gradlew connectedAndroidTest`

### Code Quality
- **Lint check**: `./gradlew lint`
- **Generate lint report**: `./gradlew lintDebug`

### Database & Schemas
- Room database schemas are stored in `app/schemas/` directory
- The build automatically includes schema assets via `sourceSets["main"].assets.srcDir("schemas")`

## Architecture Overview

### Clean Architecture Pattern
The app follows Clean Architecture with clear separation of concerns:

- **Domain Layer**: Core business logic, entities, and repository interfaces
  - `domain/` - Contains `Book`, `Bookcase`, `Bookshelf`, `ShelfStyle` entities
  - Repository interfaces define contracts for data access

- **Data Layer**: Database, network, and repository implementations
  - **Database**: Room with entities, DAOs, and type converters
  - **Network**: Ktor client for API communication with Open Library/Google Books
  - **Repository**: Implementation of domain contracts

- **Presentation Layer**: UI components, ViewModels, and state management
  - **MVVM**: ViewModels handle business logic and state
  - **Jetpack Compose**: Modern declarative UI framework
  - **Navigation**: Jetpack Navigation Compose for screen navigation

### Key Technical Components

#### Dependency Injection
- **Koin**: Used for dependency injection
- Main configuration in `di/AppModule.kt`
- Scoped ViewModels with parameters (e.g., `shelfId`, `bookId`)

#### Database
- **Room**: Local persistence with SQLite
- Entities: `BookEntity`, `BookshelfEntity`, `BookshelfBookCrossRef`
- Database factory pattern for initialization
- Type converters for complex data types

#### Networking
- **Ktor**: HTTP client for API calls
- Android engine for network requests
- JSON serialization with kotlinx.serialization
- Remote data source abstraction pattern

#### UI Architecture
- **Jetpack Compose**: Modern UI toolkit
- **Material 3**: Design system implementation
- **State Management**: ViewModel + Compose state pattern
- **Navigation**: Type-safe navigation with route definitions

### Package Structure
```
uk.co.zlurgg.mybookshelf/
├── app/                    # Application setup and navigation
├── core/                   # Shared utilities and base classes
│   ├── data/              # HTTP client setup
│   ├── domain/            # Common error types and results
│   └── presentation/      # UI theme and sample data
├── bookshelf/             # Main feature module
│   ├── data/              # Repository implementations, database, network
│   ├── domain/            # Entities and repository interfaces
│   └── presentation/      # UI screens, ViewModels, components
└── di/                    # Dependency injection configuration
```

### Key Patterns Used

#### Repository Pattern
- Abstract repository interfaces in domain layer
- Concrete implementations in data layer
- Separation of local and remote data sources

#### State Management
- ViewModels hold UI state
- Actions/Events pattern for user interactions
- Shared ViewModels for cross-screen data

#### Mapper Pattern
- Data mappers convert between DTOs, entities, and domain models
- Located in `data/mappers/` directories

## Development Notes

### Testing Infrastructure
- **Test Framework**: JUnit 4 with Robolectric for Android unit tests
- **Test Dependencies**: Full test suite including androidx-test-core, kotlinx-coroutines-test, androidx-arch-core-testing
- **ViewModel Testing**: Uses `InstantTaskExecutorRule`, `@RunWith(RobolectricTestRunner::class)`, and `@OptIn(ExperimentalCoroutinesApi::class)`
- **Async Testing**: Tests use `runTest`, `advanceUntilIdle()`, and proper coroutine test patterns
- **Test Coverage**: Repository layer, ViewModel layer, data mappers, and integration tests
- **State Flow Testing**: ViewModels using `stateIn()` require state collection via `launch { vm.state.collect { } }` to trigger initialization

### API Integration
- Open Library API for book search and details
- Google Books API as fallback
- Custom serializers for API response handling
- Comprehensive error handling via custom `Result` type with `DataError.Remote` enum

### Database Migrations
- Room handles schema migrations
- Schema files versioned in `app/schemas/`
- KSP used for Room annotation processing

### Build Configuration
- Android SDK: Target 36, Min 28, Compile 36
- Kotlin JVM target: 11
- Proguard enabled for release builds
- KSP arguments configured for Room incremental processing
- Version catalog system in `gradle/libs.versions.toml`

### Key Testing Patterns
- **StateFlow ViewModels**: Always collect state in tests to trigger `onStart` initialization
- **Coroutine Testing**: Use `advanceUntilIdle()` after actions for proper async completion
- **Mock Repositories**: Implement full repository interfaces with realistic fake behavior
- **Integration Tests**: Test complete user workflows rather than just isolated units