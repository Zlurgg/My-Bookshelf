# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

My Bookshelf is a social reading app built with Kotlin and Jetpack Compose that lets users create, customize, and share their bookshelves, reading setups, and cozy corners. Features include drag-and-drop shelf building, book search via Open Library/Google Books APIs, affiliate link integration for monetization, public shelf sharing, and reading nook galleries.

## Common Commands

### Build & Development
- **Build the project**: `./gradlew build`
- **Clean build**: `./gradlew clean build`
- **Install debug APK**: `./gradlew installDebug`
- **Generate APK**: `./gradlew assembleDebug`
- **List available tasks**: `./gradlew tasks`

### Testing
- **Run all tests**: `./gradlew test`
- **Run unit tests only**: `./gradlew testDebugUnitTest`
- **Run single test**: `./gradlew testDebugUnitTest --tests "*TestClassName*"`
- **Run single test class**: `./gradlew testDebugUnitTest --tests "uk.co.zlurgg.mybookshelf.bookshelf.data.repository.BookRepositoryImplTest"`
- **Run with coverage**: `./gradlew testDebugUnitTestCoverage`
- **Run connected Android tests**: `./gradlew connectedAndroidTest`

### Code Quality
- **Lint check**: `./gradlew lint`
- **Generate lint report**: `./gradlew lintDebug`
- **Check dependencies**: `./gradlew dependencies`
- **Analyze APK size**: `./gradlew app:dependencies --configuration releaseRuntimeClasspath`

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
- **Koin 4.1.0**: Used for dependency injection
- Main configuration in `di/AppModule.kt`
- Scoped ViewModels with parameters (e.g., `shelfId`, `bookId`)
- Pattern: `viewModel { (shelfId: String) -> BookshelfViewModel(shelfId, get(), get()) }`

#### Database
- **Room 2.7.2**: Local persistence with SQLite
- Entities: `BookEntity`, `BookshelfEntity`, `BookshelfBookCrossRef`
- Database factory pattern for initialization
- Type converters for complex data types
- Current schema version: 3
- KSP annotation processing with incremental compilation

#### Networking
- **Ktor 3.2.3**: HTTP client for API calls
- Android engine for network requests
- JSON serialization with kotlinx.serialization
- Remote data source abstraction pattern
- Timeout configuration: 20s socket/request timeouts
- Custom `Result<T, DataError.Remote>` for error handling

#### UI Architecture
- **Jetpack Compose** (BOM 2025.08.01): Modern UI toolkit
- **Material 3**: Design system implementation
- **State Management**: ViewModel + StateFlow pattern
- **Navigation Compose**: Type-safe navigation with route definitions
- **Screen-ViewModel pattern**: Each screen has dedicated ViewModel
- **Shared ViewModels**: Cross-screen data sharing via `SharedMyBookshelfViewModel`

### Package Structure
```
uk.co.zlurgg.mybookshelf/
├── app/                    # Application setup and navigation
│   └── navigation/        # Route definitions and nav graph
├── core/                   # Shared utilities and base classes
│   ├── data/              # HTTP client setup
│   ├── domain/            # Common error types and results
│   └── presentation/      # UI theme and sample data
├── bookshelf/             # Main feature module
│   ├── data/              # Repository implementations, database, network
│   │   ├── database/      # Room entities, DAOs, type converters
│   │   ├── mappers/       # DTO ↔ Entity ↔ Domain mappers
│   │   ├── network/       # Ktor client, DTOs, remote data sources
│   │   └── repository/    # Repository implementations
│   ├── domain/            # Entities and repository interfaces
│   │   ├── entity/        # Book, Bookshelf, Bookcase, ShelfStyle
│   │   └── repository/    # Repository contracts
│   └── presenation/       # UI screens, ViewModels, components [TYPO: should be presentation]
│       ├── bookcase/      # Bookcase screen and ViewModel
│       ├── bookdetail/    # Book detail screen and ViewModel
│       ├── bookshelf/     # Bookshelf screen and ViewModel
│       └── components/    # Reusable UI components
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
- **Current Coverage**: 12 test files for 75+ source files (needs improvement)

### API Integration
- Open Library API for book search and details
- Google Books API as fallback
- Custom serializers for API response handling
- Comprehensive error handling via custom `Result` type with `DataError.Remote` enum
- Base URLs hard-coded (should move to BuildConfig)

### Database Migrations
- Room handles schema migrations
- Schema files versioned in `app/schemas/`
- KSP used for Room annotation processing

### Build Configuration
- Android SDK: Target 36, Min 28, Compile 36
- Kotlin JVM target: 11
- ProGuard **DISABLED** for release builds (security concern - see issues below)
- KSP arguments configured for Room incremental processing
- Version catalog system in `gradle/libs.versions.toml`
- Namespace: `uk.co.zlurgg.mybookshelf`
- Build tools version: AGP 8.8.2, Kotlin 2.1.20

### Key Testing Patterns
- **StateFlow ViewModels**: Always collect state in tests to trigger `onStart` initialization
- **Coroutine Testing**: Use `advanceUntilIdle()` after actions for proper async completion
- **Mock Repositories**: Implement full repository interfaces with realistic fake behavior
- **Integration Tests**: Test complete user workflows rather than just isolated units

## Working Style Preferences

- **Be willing to disagree**: Don't just be accommodating to avoid conflict. Engage in genuine technical debate about architectural trade-offs.
- **Tell me when I'm wrong**: If you disagree with a technical decision or approach, defend your position with reasoning rather than just agreeing.
- **Technical honesty**: Be intellectually honest about problems and solutions. Don't back down from legitimate technical concerns when challenged.
- **Principled architecture**: Defend Clean Architecture, SOLID principles, DRY, and good engineering practices when they're being violated.

## Recent Architectural Decisions

### Abstraction Strategy
- **Slight over-engineering accepted**: We've implemented abstractions like `BookshelfIdGenerator` and `TimeProvider` that may seem excessive for current needs but provide future flexibility and testability.
- **Testing benefits justify abstractions**: Even simple abstractions are valuable if they make testing more deterministic and reliable.

### Repository Pattern Evolution  
- **BookDataRepository**: Implemented to eliminate duplication between `BookRepository` and `BookshelfRepository`. Handles all common book CRUD operations.
- **Layered dependencies**: ViewModels → Domain Repositories → BookDataRepository → Database/Network

### Coroutine Management
- **Prefer reactive operators**: Use `flatMapLatest`, `combine`, etc. over manual coroutine job management for better lifecycle handling and cleaner code.

### DRY (Don't Repeat Yourself) Enforcement
- **ErrorFormatter**: Centralized error message formatting via `ErrorFormatter.formatOperationError()` to avoid repeated `"Failed to [action]: ${e.message}"` patterns
- **Sample Data IDs**: Use consistent naming patterns like `"sample-shelf-*"` instead of random UUIDs for deterministic behavior
- **Test Utilities**: Extract common test setup patterns to avoid duplication across test classes

### Error Recovery Strategy
- **Current approach**: Basic error messages without retry mechanisms or offline queues
- **Status**: Acceptable technical debt for current project stage. Comprehensive error recovery (exponential backoff, offline queues) deferred as it represents a significant architectural change.

### Test Utilities
- **TestIdGenerator**: Provides unique IDs using AtomicInteger counter for deterministic tests located in `app/src/test/java/uk/co/zlurgg/mybookshelf/test/`
- **TestTimeProvider**: Allows controlling time in tests via `setTime()` and `advanceBy()` methods located in `app/src/test/java/uk/co/zlurgg/mybookshelf/test/`
- **Repository Fakes**: Implement full repository interfaces with realistic fake behavior rather than simple mocks

## Recently Implemented Features

### Bookshelf Drag & Drop Reordering (Completed)
- **Lock/Unlock Toggle**: Edit icon (unlocked) / Lock icon (locked) in TopAppBar switches between normal and reorder modes
- **Database**: Added `position: Int` field to BookshelfEntity (migration v3→v4)
- **UI Design**: Card-based shelves with 80dp fixed height, 12dp colored borders showing shelf style
- **Drag Calculation**: 88dp total item height (80dp card + 8dp padding) for accurate positioning
- **Key Learning**: Use fresh database position for each drag, don't track cumulative movements - add `shelf.position` as pointerInput key
- **Performance**: Only update shelves whose positions actually changed
- **Component Structure**: `BookshelfCard.kt` (display), `BookcaseShelf.kt` (drag/swipe logic)

## Planned Features for Next Session

### UI/Visual Enhancements (NEW)
- **Add Button Redesign**: Replace current add button with bookend/bookstop design to match bookshelf theme
- **3D Visual Effects**: Add depth and shadows to make bookshelf appear more three-dimensional
  - Book spine shadows/gradients
  - Shelf depth illusion
  - Layered background effects
  - Book cover bevels/highlights

### Affiliate Link Service
- Create `AffiliateService` in service/business layer
- Generate affiliate links on-demand based on book ISBN/title
- Support multiple affiliate partners (Amazon, Waterstones, etc.)
- Region-based link generation
- A/B testing and analytics tracking capabilities
- No database storage - generate fresh links each time

### Book Ordering Within Shelves
- Add position field to BookshelfBookCrossRef for book ordering
- Implement drag & drop for books similar to bookshelf approach
- Consider grid vs list layout for books

### Search Improvements
- Enhanced search functionality with better filtering and sorting options
- Potentially add search history or saved searches

### Testing Priorities
- Update BookcaseViewModelTest for reorder functionality
- Test database migration from v4 to v5 (affiliateLink removal)
- Add integration tests for drag and drop
- Increase test coverage from 15% to 80%+

## Navigation Structure
```
MyBookshelfGraph/
├── Bookcase (root) → BookcaseScreen → BookcaseViewModel
├── Bookshelf/{id} → BookshelfScreen → BookshelfViewModel(shelfId)
└── BookDetail/{id}/{shelfId} → BookDetailScreen → BookDetailViewModel(bookId, shelfId)
```

## Architectural Concerns to Address

### High Priority Issues

#### 1. Repository Pattern Violation - Responsibility Overlap
**Status**: RESOLVED - No duplicate methods found, clean separation of concerns
**Original Problem**: Three repositories with duplicate methods
**Current State**: Only BookRepository, BookshelfRepository, BookcaseRepository exist with distinct responsibilities

#### 2. Package Naming Typo
**Status**: RESOLVED - All packages correctly use "presentation"
**Original Problem**: `presenation` typo
**Current State**: Verified all packages use correct spelling

#### 3. ProGuard Disabled in Release
**Status**: RESOLVED - ProGuard properly configured
**Current State**: `isMinifyEnabled = true`, `isShrinkResources = true`, comprehensive rules in proguard-rules.pro

### Medium Priority Issues

#### 4. Clean Architecture Violation in BookRepositoryImpl
**Status**: RESOLVED - BookDataRepository doesn't exist, clean dependencies
**Current State**: BookRepositoryImpl depends only on RemoteBookDataSource and BookshelfDao

#### 5. Domain Entity Decisions
**Status**: PARTIALLY RESOLVED
**Decision**: `spineColor` remains in domain - it's persistent book data (Int), becomes UI only when Color() applied
**Decision**: `affiliateLink` REMOVED (database v4→v5 migration) - will be generated on-demand via future AffiliateService

#### 6. ViewModel Exception Handling Inconsistency
**Problem**: Mixed error handling patterns (try-catch vs Result type)
**Solution**: Standardize on `Result<T, Error>` pattern throughout

#### 7. Limited Test Coverage
**Problem**: Only 12 test files for 75+ source files, missing critical areas
**Solution**: Aim for 80%+ coverage, especially business logic and error paths

### Low Priority Issues

#### 8. Enum Typo and Duplication
**Status**: PARTIALLY RESOLVED
**Fixed**: `SliverMetal` → `SilverMetal` typo corrected
**Remaining**: Duplicate ShelfStyle/ShelfMaterial enums still exist (intentional separation?)

#### 9. Unused Manga Package Structure
**Problem**: Complete manga package structure exists but unused
**Solution**: Remove unused packages or document future plans

#### 10. Security Concerns
**Problem**: Missing input validation, Ktor logging in production
**Solution**: Add input sanitization, use build-specific logging

#### 11. Performance Issues
**Problem**: N+1 query patterns, StateFlow cold start issues
**Solution**: Add batch operations, use `stateIn()` with `SharingStarted.Eagerly`

#### 12. Hard-coded API Configuration
**Problem**: Base URL and parameters hard-coded
**Solution**: Move to configuration-based approach with BuildConfig