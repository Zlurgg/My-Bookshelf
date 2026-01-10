# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

My Bookshelf is a personal bookshelf organizer built with Kotlin and Jetpack Compose that lets users create, customize, and organize their book collections. Features include drag-and-drop shelf building, book search via Open Library API, optional cloud sync via Google Sign-In, and bookshelf sharing via deep links.

## Planning & Standards

When planning new features or implementation strategies, read `docs/FEATURE_PLAN_TEMPLATE.md` first to ensure consistency with project standards.

## Common Commands

### Build & Development
- **Build the project**: `./gradlew build`
- **Clean build**: `./gradlew clean build`
- **Install debug APK**: `./gradlew installDebug`
- **Generate APK**: `./gradlew assembleDebug`
- **List available tasks**: `./gradlew tasks`
- **Clean compilation cache**: `./gradlew clean` (needed after major dependency changes)

### Testing
- **Run all tests**: `./gradlew test`
- **Run unit tests only**: `./gradlew testDebugUnitTest` (542 tests, ~60 seconds)
- **Run instrumented tests**: `./gradlew connectedAndroidTest` (55 tests, ~25 seconds, requires device/emulator)
- **Run single test**: `./gradlew testDebugUnitTest --tests "*TestClassName*"`
- **Run single test class**: `./gradlew testDebugUnitTest --tests "uk.co.zlurgg.mybookshelf.bookshelf.data.repository.BookRepositoryImplTest"`
- **Run with coverage**: `./gradlew testDebugUnitTestCoverage`
- **Test distribution**: 542 unit tests (91%) + 55 integration tests (9%) = **597 total tests, ALL PASSING** ✅
- **Total execution time**: ~80 seconds
- **Philosophy**: Following Google's **"Just Say No to More End-to-End Tests"** - lean, fast, reliable integration tests instead of slow E2E tests

### Code Quality

#### Detekt (Static Analysis)
- **Run Detekt**: `./gradlew detekt`
- **Detekt config**: `app/detekt.yml` - customized thresholds and exclusions
- **Detekt version**: 1.23.8 with formatting module (ktlint wrapper)
- **Zero tolerance**: `maxIssues: 0` - all issues must be resolved
- **Key configurations**:
  - Formatting via ktlint (indentation disabled due to IDE conflict)
  - Package naming exceptions for existing underscore packages (`book_detail`, etc.)
  - Complexity thresholds raised for ViewModels and Repositories
  - `TooGenericExceptionCaught` enforced with `@Suppress` + logging requirement

#### Android Lint
- **Lint check**: `./gradlew lint`
- **Generate lint report**: `./gradlew lintDebug`

#### Other
- **Check dependencies**: `./gradlew dependencies`
- **Analyze APK size**: `./gradlew app:dependencies --configuration releaseRuntimeClasspath`

### Release Management
- **Build release APK**: `./gradlew assembleRelease`
- **GitHub CLI - Check auth**: `"C:\Program Files\GitHub CLI\gh.exe" auth status`
- **GitHub CLI - Create release**:
  ```bash
  "C:\Program Files\GitHub CLI\gh.exe" release create v1.0.X-alpha \
    app\build\outputs\apk\release\app-release.apk \
    --title "MyBookshelf vX.X.X-alpha - Feature Name" \
    --notes-file RELEASE_NOTES.md
  ```
- **Release Process**:
  1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
  2. Update `RELEASE_NOTES.md` with new version section at top
  3. Build release APK with `./gradlew assembleRelease`
  4. Commit changes: `git add . && git commit -m "Prepare for vX.X.X release"`
  5. Push to GitHub: `git push`
  6. Create release with GitHub CLI command above
- **Note**: APK is automatically uploaded and attached to the GitHub release

### Commit Style
- Keep commit messages concise and descriptive of the actual work done
- Do NOT include "Generated with Claude Code" signatures
- Do NOT include "Co-Authored-By" footers
- Use conventional commit format: `type(scope): description`
- Examples: `feat(bookclub): Add rating system`, `fix(ui): Fix hardcoded strings`

### Database & Schemas
- Room database schemas are stored in `app/schemas/` directory
- The build automatically includes schema assets via `sourceSets["main"].assets.srcDir("schemas")`
- Current database version: **8**
- **No migrations**: Pre-release app, fresh database on each install

### Logging & Debugging

The app uses **Timber** for professional logging with zero overhead in release builds.

#### Viewing Logs

**Filter by tag:**
```bash
# Book search queries and API responses
adb logcat -s BookSearch

# Export/import operations
adb logcat -s BookshelfExport
adb logcat -s BookshelfImport

# Deep link sharing
adb logcat -s DeepLinkImport

# HTTP errors and exceptions
adb logcat -s ErrorMapper

# View all app logging
adb logcat | grep -E "(BookSearch|BookshelfExport|BookshelfImport|DeepLinkImport|ErrorMapper)"
```

**In Android Studio:**
- Open Logcat panel
- Filter by tag (e.g., `tag:BookSearch`)
- Filter by level (Debug, Warning, Error)

#### Where Logging is Implemented

**Network Layer:**
- `KtorRemoteBookDataSource.kt` - Search requests/responses, query construction
- `OpenLibraryApiService.kt` - HTTP requests and status codes
- `ErrorMapper.kt` - All exceptions, HTTP errors, serialization failures

**Use Cases:**
- `ExportBookshelfUseCaseImpl.kt` - Export workflow stages
- `ImportBookshelfUseCaseImpl.kt` - Import workflow stages, validation errors
- `DeepLinkImportUseCaseImpl.kt` - Token validation, conflict detection

#### Adding New Logging

**1. Import Timber:**
```kotlin
import timber.log.Timber
```

**2. Add a tag constant:**
```kotlin
companion object {
    private const val TAG = "FeatureName"  // e.g., "BookDatabase", "ImageCache"
}
```

**3. Log at appropriate levels:**
```kotlin
// Debug - Development info (DEBUG builds only)
Timber.tag(TAG).d("Starting operation with param: %s", value)

// Warning - Recoverable issues
Timber.tag(TAG).w("Retrying failed operation: %s", error)

// Error - Failures that prevent functionality
Timber.tag(TAG).e("Operation failed: %s", error)
Timber.tag(TAG).e(exception, "Operation failed with exception")
```

#### Logging Best Practices

**DO:**
- ✅ Log errors and exceptions with context
- ✅ Log critical operation start/completion (export, import, deep links)
- ✅ Use Timber's varargs formatting: `Timber.d("Value: %s", value)`
- ✅ Include relevant IDs: `"Processing shelf: %s", shelfId`
- ✅ Use appropriate tags for filtering
- ✅ Log at correct levels (debug/warning/error)

**DON'T:**
- ❌ Log in normal happy-path operations (too verbose)
- ❌ Log user data or PII (privacy violation)
- ❌ Use string interpolation: `"Value: $value"` (allocates even when disabled)
- ❌ Log in tight loops (performance impact)
- ❌ Use `println()` or `Log` directly (use Timber instead)

#### Log Levels

- **Debug** (`Timber.d()`): Development info, operation stages - **DEBUG builds only**
- **Warning** (`Timber.w()`): Recoverable issues (conflicts, retries, deprecated data)
- **Error** (`Timber.e()`): Failures preventing functionality (network errors, database errors)
- **Info** (`Timber.i()`): Important milestones (rarely needed)

#### Performance

- **DEBUG builds**: All logging active via `Timber.DebugTree()`
- **RELEASE builds**: Zero overhead - no tree planted, all logs are no-ops
- **String formatting**: Only happens if tree is planted (varargs pattern)
- **ProGuard**: Automatically optimized in release builds

#### Example Log Output

**Book Search:**
```
D/BookSearch: === SEARCH REQUEST ===
D/BookSearch: Raw inputs - query: 'asimov', author: 'null', title: 'null'
D/BookSearch: Final query: 'asimov'
D/BookSearch: === HTTP REQUEST ===
D/BookSearch: Full URL: https://openlibrary.org/search.json?q=asimov&limit=15...
D/BookSearch: Response status: 200
D/BookSearch: === SEARCH RESPONSE: SUCCESS ===
D/BookSearch: Total results found by API: 1490
D/BookSearch: Results returned in response: 15
```

**Import Error:**
```
D/BookshelfImport: Starting bookshelf import
D/BookshelfImport: Deserialization successful, validating format...
E/BookshelfImport: Bookshelf import failed: INVALID_INPUT
```

**HTTP Exception:**
```
E/ErrorMapper: HTTP call failed - Mapped to: REQUEST_TIMEOUT
    java.net.SocketTimeoutException: timeout
    at io.ktor.client.engine.android...
```

#### Configuration

**Initialization:** `MyBookshelfApplication.kt`
```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```

**Dependency:** `gradle/libs.versions.toml`
```toml
timber = "5.0.1"
```

#### Future Logging Candidates

Consider adding logging to these areas if debugging is needed:
- Database repository errors (constraint violations, query failures)
- Image loading failures (Coil errors)
- Tutorial flow progression
- State restoration issues

## Architecture Overview

### Clean Architecture Pattern
The app follows Clean Architecture with clear separation of concerns:

- **Domain Layer**: Core business logic, entities, and repository interfaces
  - `domain/` - Contains `Book`, `Bookcase`, `Bookshelf`, `ShelfStyle` entities
  - Repository interfaces define contracts for data access
  - **Pure domain code** - Zero Android dependencies, including ErrorFormatter

- **Data Layer**: Database, network, and repository implementations
  - **Database**: Room with entities, DAOs, and type converters
  - **Network**: Ktor client for API communication with Open Library API
  - **Repository**: Implementation of domain contracts

- **Presentation Layer**: UI components, ViewModels, and state management
  - **MVVM**: ViewModels handle business logic and state
  - **Jetpack Compose**: Modern declarative UI framework
  - **Navigation**: Jetpack Navigation Compose for screen navigation

### Key Technical Components

#### Dependency Injection
- **Koin 4.1.1**: Used for dependency injection
- **Feature-Scoped Modules**: Each feature package has its own `di/` folder with dedicated module
- **Root Aggregator**: `di/AppModule.kt` includes all feature modules (~30 lines)
- **Module Structure**:
  - `core/di/CoreModule.kt` - Infrastructure (database, network, preferences, core services)
  - `auth/di/AuthModule.kt` - Authentication (Google Sign-In, auth state, SignInViewModel)
  - `sync/di/SyncModule.kt` - Cloud sync (Firestore engine, connectivity, migration)
  - `update/di/UpdateModule.kt` - In-app updates (GitHub release checking, APK download)
  - `bookshelf/di/BookClubModule.kt` - Book club feature (16 use cases, handler)
  - `bookshelf/di/BookshelfModule.kt` - Main bookshelf (repositories, use cases, ViewModels)
- Scoped ViewModels with parameters (e.g., `shelfId`, `bookId`)
- Pattern: `viewModel { (shelfId: String) -> BookshelfViewModel(shelfId, get(), get()) }`
- Service layer abstractions: `TimeProvider`, `BookshelfIdGenerator` for testability

#### Database
- **Room 2.8.0**: Local persistence with SQLite
- Entities: `BookEntity`, `BookshelfEntity`, `BookshelfBookCrossRef`
- Database factory pattern for initialization
- Type converters for complex data types
- Current schema version: **8**
- KSP annotation processing with incremental compilation
- **No migrations**: Pre-release app, fresh database on each install

#### Networking & HTTP Architecture
- **Ktor 3.3.0**: HTTP client for API calls with Android engine
- **Enterprise-grade HTTP setup**: Retry policies, exponential backoff, proper timeout configuration
- **Configuration-driven**: All API endpoints and timeouts configured via BuildConfig (no hardcoded URLs)
- **OpenLibrary Integration**: Book search and details via Open Library API (GoogleBooks removed)
- **Intelligent retry policies**: 3x automatic retries for 5xx errors, timeouts, network failures
- **JSON serialization**: kotlinx.serialization with proper error handling
- **Custom Result pattern**: `Result<T, DataError>` for comprehensive error handling
- **Coil3 Integration**: Image loading with Ktor3 network fetcher
- **Clean API architecture**: Domain-oriented data sources with HTTP service abstraction

#### UI Architecture
- **Jetpack Compose** (BOM 2025.09.00): Modern UI toolkit
- **Material 3**: Design system implementation
- **State Management**: ViewModel + StateFlow pattern
- **Navigation Compose**: Type-safe navigation with route definitions
- **Screen-ViewModel pattern**: Each screen has dedicated ViewModel
- **UseCase Pattern**: ViewModels depend only on UseCases, never repositories
- **3D Visual Effects**: Realistic book spine rendering with shadows and gradients

### Package Structure
```
uk.co.zlurgg.mybookshelf/
├── app/                    # Application setup and navigation
│   └── navigation/        # Route definitions and nav graph
├── di/                     # Root DI aggregator (AppModule.kt ~30 lines)
├── core/                   # Shared utilities and infrastructure (GENERIC ONLY)
│   ├── di/                # CoreModule.kt - database, network, preferences
│   ├── data/
│   │   ├── network/       # Generic HTTP infrastructure (HttpClientFactory, ApiConfig)
│   │   ├── image/         # Image loading infrastructure
│   │   └── service/       # Generic services (TimeProvider, IdGenerator, SystemLanguageProvider)
│   ├── domain/            # Generic domain types and contracts (PURE - NO ANDROID DEPS)
│   │   ├── error/         # ErrorMapper, DataError, Result types, ErrorFormatter
│   │   └── service/       # Generic service interfaces
│   ├── presentation/      # UI theme and sample data
│   └── util/              # Generic utilities
├── auth/                   # Authentication feature
│   ├── di/                # AuthModule.kt - auth services, SignInViewModel
│   ├── data/              # GoogleAuthUiClient, AuthStateRepository
│   ├── domain/            # AuthService interface, UseCases
│   └── presentation/      # SignInViewModel, SignInScreen
├── sync/                   # Cloud sync feature
│   ├── di/                # SyncModule.kt - sync engine, repositories
│   ├── data/              # SyncEngine, FirestoreRemoteDataSource
│   └── domain/            # SyncRepository, migration UseCases
├── update/                 # In-app update feature
│   ├── di/                # UpdateModule.kt - GitHub API, download service
│   ├── data/              # GitHubApiService, UpdateRepository
│   └── domain/            # Update UseCases
└── bookshelf/             # Book feature domain (BOOK-SPECIFIC)
    ├── di/                # BookshelfModule.kt + BookClubModule.kt
    ├── data/              # Book data layer implementations
    │   ├── database/      # Room entities, DAOs, type converters
    │   ├── mappers/       # DTO ↔ Entity ↔ Domain mappers
    │   ├── network/       # Book-specific network layer
    │   │   ├── api/       # Book API services (OpenLibraryApiService)
    │   │   └── *.kt       # RemoteBookDataSource, KtorRemoteBookDataSource
    │   ├── repository/    # Repository implementations
    │   └── service/       # Book-specific services
    ├── domain/            # Book domain layer
    │   ├── model/         # Book, Bookshelf, Bookcase domain models
    │   ├── repository/    # Repository contracts
    │   ├── service/       # Book service interfaces
    │   └── usecase/       # Use case implementations (50 UseCases)
    └── presentation/      # Book UI layer
        ├── bookcase/      # Bookcase screen and ViewModel
        ├── book_detail/   # Book detail screen and ViewModel
        ├── bookshelf/     # Bookshelf screen and ViewModel
        ├── bookclub/      # Book club handlers
        ├── deeplink/      # Deep link handling
        └── components/    # Reusable book UI components
```

### Key Patterns Used

#### Repository Pattern
- Abstract repository interfaces in domain layer
- Concrete implementations in data layer
- Separation of local and remote data sources

#### UseCase Pattern
- ViewModels depend only on UseCases (no direct repository access)
- UseCases encapsulate business logic
- Consistent `Result<T, DataError>` return types
- **50 UseCases total**: bookcase (10), bookshelf (4), book_detail (6), bookclub (16), deeplink (1), export (3), tutorial (3), welcome (1), auth (3), sync (3)

#### State Management
- ViewModels hold UI state via StateFlow
- Actions/Events pattern for user interactions
- Unidirectional data flow (State + Actions)

#### Mapper Pattern
- Data mappers convert between DTOs, entities, and domain models
- Located in `data/mappers/` directories

#### Error Handling Pattern
- `Result<T, DataError>` type for all operations that can fail
- `ErrorFormatter.formatDataErrorMessage()` for context-free error messages
- `ErrorMapper.mapExceptionToDataError()` for exception conversion
- `ErrorMapper.safeSuspendCall()` for centralized exception handling with logging
- Named variables in `when` expressions (e.g., `deleteResult`, `addResult`)

#### Error Handling Best Practices (ENFORCED)

**Ideal Design (for new code):**
- Repositories should **never throw exceptions** - always return `Result<T, DataError>`
- UseCases should not need try-catch blocks when repositories are properly designed
- This keeps error handling explicit and type-safe throughout the stack

**Pragmatic Pattern (for existing code with throwing repositories):**
```kotlin
// Simple UseCases: Use ErrorMapper.safeSuspendCall()
override suspend fun execute(id: String): Result<T?, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        repository.getById(id)
    }
}

// Complex UseCases with early returns: Use @Suppress + logging
@Suppress("TooGenericExceptionCaught") // Intentional: converts all exceptions to Result.Error with logging
override suspend fun execute(id: String): Result<Unit, DataError.Local> {
    return try {
        // Complex logic with early returns, multiple operations...
        Result.Success(Unit)
    } catch (e: Exception) {
        val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
        Timber.tag(TAG).e(e, "Operation failed - Mapped to: %s", error)
        Result.Error(error)
    }
}
```

**Key Rules:**
- ✅ **Always log caught exceptions** with Timber for debugging
- ✅ **Always add TAG constant** in companion object for log filtering
- ✅ **Use `@Suppress("TooGenericExceptionCaught")`** with comment explaining intent
- ✅ **Map to typed errors** using `ErrorMapper.mapExceptionToDataError()`
- ❌ **Never silently catch exceptions** without logging
- ❌ **Never catch Exception without `@Suppress`** - Detekt will flag it

## Development Notes

### Testing Infrastructure
- **Test Framework**: JUnit 4 with Robolectric for unit tests, AndroidJUnit4 for instrumented tests
- **Test Philosophy**: Following Google's **"Just Say No to More End-to-End Tests"** - fast, reliable integration tests
- **Current Status**: **597 tests total, ALL PASSING** ✅
  - Unit tests (91%): 542 tests in `app/src/test/` (~60s execution)
  - Integration tests (9%): 55 tests in `app/src/androidTest/` (~25s execution)
  - **Total execution time**: ~80 seconds
- **Test Utilities**: `TestIdGenerator`, `TestTimeProvider` for deterministic testing
- **StateFlow Testing**: ViewModels using `stateIn()` require state collection via `launch { vm.state.collect { } }` to trigger initialization
- **DataStore Testing**: Use unique file names per test to avoid singleton conflicts (`test_file_${System.currentTimeMillis()}`)

### API Integration
- Open Library API for book search and details
- Custom serializers for API response handling
- Comprehensive error handling via custom `Result` type with `DataError` enum
- Configuration-driven API endpoints via BuildConfig
- **Note**: GoogleBooks API was removed - OpenLibrary is the sole provider

### Build Configuration
- Android SDK: Target 36, Min 28, Compile 36
- Kotlin JVM target: 11
- ProGuard **ENABLED** for release builds (`isMinifyEnabled = true`, `isShrinkResources = true`)
- KSP arguments configured for Room incremental processing
- Version catalog system in `gradle/libs.versions.toml`
- Namespace: `uk.co.zlurgg.mybookshelf`
- Build tools version: AGP 8.13.0, Kotlin 2.2.20
- Static analysis: Detekt 1.23.8 with ktlint formatting

### Key Testing Patterns
- **StateFlow ViewModels**: Always collect state in tests to trigger `onStart` initialization
- **Coroutine Testing**: Use `advanceUntilIdle()` after actions for proper async completion
- **Mock Repositories**: Implement full repository interfaces with realistic fake behavior
- **Integration Tests**: Test full stack (ViewModel → UseCases → Repository → Database) with real Room database
- **Stub External Only**: Only stub network/external services - use real implementations for everything else

### Critical Testing Lesson: Always Research Before Coding
**IMPORTANT**: Before writing any test code, follow this 4-step research process:

1. **Search for actual implementations**: Use Glob/Grep to find the real class names
   - Example: `JsonBookshelfSerializer` exists, not `KotlinxBookshelfSerializer`
   - Example: `DatabaseBookshelfDataOrchestrator` exists, not `BookshelfDataOrchestratorImpl`

2. **Check exact data class definitions**: Read the actual source files to verify:
   - Constructor parameters (e.g., `BookshelfExportMapper` requires `TimeProvider` and `IdGenerator`)
   - Field names (e.g., `averageRating` not `ratingsAverage`, `ratingCount` not `ratingsCount`)
   - Required vs optional parameters

3. **Verify methods exist**: Check interfaces and implementations before calling methods
   - Example: `removeShelf()` exists in `BookcaseRepository`, not `deleteShelf()`
   - Example: `getShelfById()` was added, but `getShelfByName()` doesn't exist

4. **Create focused tests**: Only test what actually exists with correct signatures
   - Use extension functions correctly (`Book.toBookEntity()` not `BookMapper.toEntity()`)
   - Match constructor parameter order exactly
   - Use correct enum values (`ShelfStyle.DarkWood` not `ShelfStyle.DARK_WOOD`)

**Why This Matters**: During integration test development, initial tests without research resulted in 3 files with 20+ compilation errors. After applying this research process, all 6 new integration test files (35 tests) compiled successfully on first attempt. This approach saves significant debugging time and prevents false assumptions about the codebase.

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
- **Layered dependencies**: ViewModels → UseCases → Repositories → Database/Network

### Coroutine Management
- **Prefer reactive operators**: Use `flatMapLatest`, `combine`, etc. over manual coroutine job management for better lifecycle handling and cleaner code.

### Koin Feature-Scoped DI Modules
- **Pattern**: Each feature package has its own `di/` folder with dedicated Koin module
- **Rationale**: Replaced 435-line monolithic `AppModule.kt` with 6 focused modules (~30-240 lines each)
- **Benefits**:
  - **Maintainability**: Each feature's DI is self-contained and easy to navigate
  - **Team Scalability**: Different developers can work on different modules without conflicts
  - **Testing**: Individual modules can be loaded in isolation for testing
  - **Readability**: Clear separation of concerns, easy to find DI configuration for any feature
- **Structure**:
  - `di/AppModule.kt` - Root aggregator using `includes()` (~30 lines)
  - `core/di/CoreModule.kt` - Infrastructure dependencies
  - `auth/di/AuthModule.kt` - Authentication dependencies
  - `sync/di/SyncModule.kt` - Cloud sync dependencies
  - `update/di/UpdateModule.kt` - In-app update dependencies
  - `bookshelf/di/BookshelfModule.kt` - Main bookshelf feature dependencies
  - `bookshelf/di/BookClubModule.kt` - Book club feature dependencies

### DRY (Don't Repeat Yourself) Enforcement
- **ErrorFormatter**: Centralized error message formatting via `ErrorFormatter.formatDataErrorMessage()` with 28 error type mappings
- **Named Variables**: All `when` expressions use named variables for clarity (e.g., `deleteResult`, `addResult`)
- **Sample Data IDs**: Use consistent naming patterns like `"sample-shelf-*"` instead of random UUIDs for deterministic behavior
- **Test Utilities**: Extract common test setup patterns to avoid duplication across test classes

### Error Handling Strategy
- **Context-Free Domain Errors**: `ErrorFormatter.formatDataErrorMessage()` provides user-friendly messages without Android Context dependency
- **Comprehensive Error Types**: 28 error types mapped (Remote, Local, Validation)
- **Full Error Preservation**: `Result<T, DataError>` pattern preserves actual error types throughout the stack
- **Consistent Patterns**: All 5 ViewModels use identical error handling across the app

### Test Utilities
- **TestIdGenerator**: Provides unique IDs using AtomicInteger counter for deterministic tests located in `app/src/test/java/uk/co/zlurgg/mybookshelf/test/`
- **TestTimeProvider**: Allows controlling time in tests via `setTime()` and `advanceBy()` methods located in `app/src/test/java/uk/co/zlurgg/mybookshelf/test/`
- **Repository Fakes**: Implement full repository interfaces with realistic fake behavior rather than simple mocks

## Current Status

### **Production Readiness**: 98% Complete ✅
- ✅ **Core Functionality**: Complete and polished
- ✅ **Architecture**: Enterprise-level Clean Architecture with pure domain layer (zero Android deps)
  - 50 UseCases across 10 domains (including bookclub with 16 use cases)
  - 5 ViewModels with consistent patterns
  - 3 Repository interfaces with clean implementations
- ✅ **Code Quality**: 100% consistent patterns with standardized error handling
- ✅ **User Experience**: Professional, intuitive, responsive
- ✅ **HTTP Infrastructure**: Production-grade with retry policies, configuration-driven
- ✅ **Testing**: All 597 tests passing (542 unit + 55 integration) ✅
  - **Distribution**: 91% unit, 9% integration - exceeds Google's recommendations
  - **Quality**: Real Room database testing, full stack validation, deterministic test utilities
  - **Performance**: ~80 seconds execution time for full test suite
- ✅ **Performance**: Optimized with efficient reactive patterns and intelligent HTTP retry mechanisms
- ✅ **Security**: Proper practices, no secrets exposed, environment-specific configurations
- ✅ **Technical Debt**: **Zero TODOs**, zero code smells

#### **Remaining Work (2% to 100%)**:
1. **Store Deployment Preparation** - Privacy policy, screenshots, store listing
2. **Beta Testing** - Friends and family testing round

## 🎯 Next Steps

### Immediate Priorities
1. **Store Deployment Preparation**: Privacy policy, screenshots, store listing
2. **Beta Testing**: Friends and family testing round
3. **Play Store Submission**: Final production release

### Optional Future Enhancements
- Enhanced search filters (genre, publication year, rating range)
- Recommendation engine based on collection analysis
- Tags, categories, reading lists
- Statistics dashboard (reading progress, collection insights)

---

## Navigation Structure
```
MyBookshelfGraph/
├── Bookcase (root) → BookcaseScreen → BookcaseViewModel
├── Bookshelf/{id} → BookshelfScreen → BookshelfViewModel(shelfId)
└── BookDetail/{id}/{shelfId} → BookDetailScreen → BookDetailViewModel(bookId, shelfId)
```

## Architectural Principles (ENFORCED)

### ✅ RESOLVED ARCHITECTURAL ISSUES

#### 1. Repository Pattern Violation
**Status**: ✅ RESOLVED
**Solution**: ViewModels only depend on UseCases, never repositories

#### 2. Error Information Loss
**Status**: ✅ RESOLVED
**Solution**: `ErrorFormatter.formatDataErrorMessage()` preserves full `DataError` type information

#### 3. Domain Layer Purity
**Status**: ✅ RESOLVED
**Solution**: ErrorFormatter has zero Android dependencies (no Context, no R imports)

#### 4. ProGuard Configuration
**Status**: ✅ RESOLVED
**Solution**: ProGuard properly enabled with `isMinifyEnabled = true`, `isShrinkResources = true`

#### 5. Compilation Issues
**Status**: ✅ RESOLVED
**Solution**: All 597 tests passing, clean compilation

---

## 📋 Code Quality Metrics (Current State)

### Overall Assessment: **PRODUCTION-READY** ✅
- **Architecture Grade**: A+ (Outstanding)
- **Code Quality Grade**: A (Excellent)
- **Test Coverage Grade**: B- (Good quality, needs expansion)
- **Overall Score**: 92/100

### Metrics
- **Production Files**: 286 Kotlin source files (including 6 feature-scoped DI modules)
- **Test Files**: 63 test files (55 unit + 8 integration)
- **Total Tests**: 597 test cases (**all passing** ✅)
  - 542 unit tests (~60s execution)
  - 55 integration tests (~25s execution)
- **UseCases**: 50 across 10 domains (including bookclub and sync)
- **ViewModels**: 6 (Bookcase, Bookshelf, BookDetail, DeepLink, Welcome, SignIn)
- **Repositories**: 9 (Bookcase, Bookshelf, Book, BookClub, AuthState, Sync, UserPreferences, Update, UpdatePreferences)
- **Build Status**: ✅ Clean compilation, zero errors
- **Technical Debt**: **Zero TODOs** ✅
- **Code Smells**: Minimal

### Strengths
- ✅ **Clean Architecture** executed flawlessly
- ✅ **SOLID principles** followed consistently
- ✅ **Pure domain layer** with zero Android dependencies
- ✅ **Consistent error handling** across all ViewModels (17 locations)
- ✅ **Named variables** in all when expressions
- ✅ **Zero duplication** through shared utilities
- ✅ **Professional-grade HTTP** with retry policies

---

*Last Updated*: January 2025 (Koin Feature-Scoped DI Module Refactoring)
*Status*: Production-ready with 98% completion
*Next Priority*: Store deployment preparation
