# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

My Bookshelf is a personal bookshelf organizer built with Kotlin and Jetpack Compose that lets users create, customize, and organize their book collections. Features include drag-and-drop shelf building, book search via Open Library/Google Books APIs, local data storage, and simple bookshelf Export/Import for sharing between devices.

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
- Current database version: **5** (affiliateLink removed in v4→v5 migration)

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
- **Koin 4.1.1**: Used for dependency injection
- Main configuration in `di/AppModule.kt`
- Scoped ViewModels with parameters (e.g., `shelfId`, `bookId`)
- Pattern: `viewModel { (shelfId: String) -> BookshelfViewModel(shelfId, get(), get()) }`
- Service layer abstractions: `TimeProvider`, `BookshelfIdGenerator` for testability

#### Database
- **Room 2.8.0**: Local persistence with SQLite
- Entities: `BookEntity`, `BookshelfEntity`, `BookshelfBookCrossRef`
- Database factory pattern for initialization
- Type converters for complex data types
- Current schema version: **5** (affiliateLink removed, position added)
- KSP annotation processing with incremental compilation
- Migration path: v2→v3 (removed onShelf), v3→v4 (added position), v4→v5 (removed affiliateLink)

#### Networking
- **Ktor 3.3.0**: HTTP client for API calls
- Android engine for network requests
- JSON serialization with kotlinx.serialization
- Remote data source abstraction pattern
- Timeout configuration: 20s socket/request timeouts
- Custom `Result<T, DataError.Remote>` for error handling
- **Coil3 Integration**: Image loading with Ktor3 network fetcher

#### UI Architecture
- **Jetpack Compose** (BOM 2025.09.00): Modern UI toolkit
- **Material 3**: Design system implementation
- **State Management**: ViewModel + StateFlow pattern
- **Navigation Compose**: Type-safe navigation with route definitions
- **Screen-ViewModel pattern**: Each screen has dedicated ViewModel
- **Shared ViewModels**: Cross-screen data sharing via `SharedMyBookshelfViewModel`
- **3D Visual Effects**: Realistic book spine rendering with shadows and gradients

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
- **Current Status**: 11 test files, 53 tests total, **ALL PASSING** ✅ (0 failures)
- **Test Utilities**: `TestIdGenerator`, `TestTimeProvider` for deterministic testing
- **Coverage Analysis**: 14% file coverage (11 test files / 79 source files) - **needs expansion**
- **Test Quality**: High-quality tests with proper fakes, mocks, and integration testing

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
- ProGuard **ENABLED** for release builds (`isMinifyEnabled = true`, `isShrinkResources = true`)
- KSP arguments configured for Room incremental processing
- Version catalog system in `gradle/libs.versions.toml`
- Namespace: `uk.co.zlurgg.mybookshelf`
- Build tools version: AGP 8.13.0, Kotlin 2.2.20

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

### Coil3 Migration & Visual Improvements (Completed)
- **Coil3 Integration**: Upgraded from Coil2 to Coil3 with Ktor3 network fetcher
- **Image Loading**: Enhanced image loading with proper timeout configuration
- **3D Book Effects**: Realistic book spine rendering with shadows, gradients, and matte colors
- **Service Layer**: Added proper abstraction with `SystemTimeProvider`, `UuidBookshelfIdGenerator`
- **Test Restructuring**: Complete test infrastructure overhaul with utilities

### Database Architecture Cleanup (Completed)
- **Schema v5**: Removed `affiliateLink` field from BookEntity (clean architecture decision)
- **Proper Migrations**: Clean migration path from v2 through v5
- **Type Converters**: Proper handling of complex data types

### Search UI/UX Improvements (Completed)
- **Consolidated Search Interface**: Moved sort dropdown into advanced filters section for cleaner UI
- **Right-Aligned Controls**: Toggle buttons now right-aligned for neater, more professional appearance
- **Compact Components**: Reduced component sizes (padding, spacing, typography) for less cramped dialog
- **Progressive Disclosure**: Single collapsible "Advanced" section containing sort + author/title filters
- **State Management Cleanup**: Removed unused showSort state and simplified callback structure
- **Enhanced User Flow**: Search → Advanced Filters (Sort + Author/Title) → Results
- **String Resources**: Fixed hint text formatting ("e.g. Tolkien" instead of "e.g., Tolkien")
- **Code Architecture**: Clean separation with BookSearchState + BookSearchCallbacks pattern

## Planned Features - Updated Priority Order

### UI/Visual Enhancements ✅ COMPLETED
- **Add Button Redesign**: Bookend/bookstop design implemented
- **3D Visual Effects**: Realistic book spines with shadows, gradients, and matte colors
- **Code Cleanup**: Architecture improvements and violation fixes

### Search Improvements ✅ COMPLETED
- **Enhanced UI/UX**: Consolidated search interface with professional right-aligned controls
- **Advanced Filtering**: Author/title filtering with progressive disclosure
- **Sorting Options**: 5 sort options (Best Match, Newest, Oldest, Highest Rated, Most Popular)
- **Improved Architecture**: Clean state management with BookSearchState + callbacks pattern
- **Performance**: Debounced search (450ms) with efficient re-search triggering

### 📁 Bookshelf Export/Import (NEXT)
- **Local-First Design**: Export/Import functionality for sharing bookshelves between devices
- **Android Share Integration**: Standard share sheet for sending bookshelf data
- **JSON Format**: Simple, readable format for bookshelf data exchange
- **Validation**: Import validation with user-friendly error handling
- **Timeline**: 2-3 hours implementation

### Testing Priorities
- Increase test coverage from 15% to 80%+
- Fix existing failing tests (5 search/debounce tests)
- Update BookcaseViewModelTest for reorder functionality  
- Add integration tests for drag and drop
- Test database migrations thoroughly
- Add UI/component testing with Compose testing framework

### Future Enhancements
- **Affiliate Link Service**: Optional on-demand affiliate link generation for book purchases
- **Enhanced Search**: Additional search filters and recommendation features
- **Cloud Sync**: Future user accounts for cross-device synchronization
- **Advanced Organization**: Tags, categories, and custom shelf organization features

## Navigation Structure
```
MyBookshelfGraph/
├── Bookcase (root) → BookcaseScreen → BookcaseViewModel
├── Bookshelf/{id} → BookshelfScreen → BookshelfViewModel(shelfId)
└── BookDetail/{id}/{shelfId} → BookDetailScreen → BookDetailViewModel(bookId, shelfId)
```

## Architectural Concerns to Address

### ✅ RESOLVED ISSUES

#### 1. Repository Pattern Violation - Responsibility Overlap
**Status**: ✅ RESOLVED - Clean separation of concerns maintained
**Solution**: BookRepository, BookshelfRepository, BookcaseRepository have distinct responsibilities

#### 2. Package Naming Typo
**Status**: ✅ RESOLVED - All packages correctly use "presentation"
**Solution**: Verified all packages use correct spelling throughout codebase

#### 3. ProGuard Disabled in Release
**Status**: ✅ RESOLVED - ProGuard properly enabled
**Solution**: `isMinifyEnabled = true`, `isShrinkResources = true` in release builds

#### 4. Compilation Issues
**Status**: ✅ RESOLVED - Fixed ImageLoaderFactory context injection
**Solution**: Updated AppModule.kt to properly inject Android Context for ImageLoader creation

### 🔄 CURRENT MEDIUM PRIORITY ISSUES

#### 5. Test Status
**Status**: ✅ RESOLVED - All tests now passing!
**Current Status**: 53 tests passing, 0 failures 🎉
**Recent Fixes**:
- Fixed `BookDetailViewModelTest.onPurchaseClick_marks_book_as_purchased` - implemented missing purchase functionality
- Fixed `BookcaseViewModelTest.showAddDialog_toggles_dialog_visibility` - fixed dialog state management
**Note**: Search tests are all passing ✅

### Medium Priority Issues

#### 6. Domain Entity Architecture
**Status**: ✅ RESOLVED
**Decision**: `spineColor` remains in domain - it's persistent book data (Int), becomes UI only when Color() applied
**Decision**: `affiliateLink` REMOVED (database v4→v5 migration) - will be generated on-demand via future AffiliateService

#### 7. ViewModel Exception Handling Inconsistency
**Problem**: Mixed error handling patterns (try-catch vs Result type)
**Solution**: Standardize on `Result<T, Error>` pattern throughout

#### 8. Test Coverage Improvement Needed
**Current State**: 9 test files, 53 tests total, 79 source files
**Coverage**: ~12% (needs significant improvement)
**Target**: 80%+ coverage, especially business logic and error paths
**Status**: All tests passing, ready for coverage expansion

#### 9. API Compliance Complete
**Status**: ✅ RESOLVED
**Implemented**:
- User-Agent header: "MyBookshelf/1.0 (Android App; github.com/zlurgg/mybookshelf)"
- Conditional logging: debug builds only (BuildConfig.DEBUG)
- BuildConfig generation enabled in gradle configuration
- Release builds successfully disable logging for production privacy

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

---

## 📋 Comprehensive Code Review Results

### 🏆 **Overall Assessment: EXCELLENT**
The codebase demonstrates **excellent engineering practices** with a solid architecture foundation ready for scaling and feature expansion. Quality is **production-ready** with only minor technical debt.

### ✅ **Architecture & Design Patterns - OUTSTANDING**
- **Clean Architecture**: Proper layer separation (Domain → Data → Presentation)
- **MVVM Pattern**: ViewModels properly handle business logic and state
- **Repository Pattern**: Clean abstraction between data sources
- **Result Pattern**: Robust error handling with `Result<T, E>` type
- **Dependency Injection**: Well-structured Koin DI with proper scoping
- **Reactive Programming**: Excellent use of StateFlow and coroutines

### ✅ **Code Quality - HIGH STANDARD**
- **Type Safety**: Excellent use of Kotlin's type system
- **Null Safety**: Proper handling of nullable types throughout
- **Immutability**: Consistent use of data classes and val
- **Naming**: Clear, descriptive variable and function names
- **No Code TODOs**: Clean codebase with no pending work items

### ✅ **Security & Performance - WELL OPTIMIZED**
- **Security**: No hardcoded secrets, proper HTTPS, User-Agent headers
- **ProGuard**: Comprehensive obfuscation rules for release builds
- **Performance**: Debounced search, lazy loading, efficient database queries
- **Memory Management**: Proper coroutine scope usage and lifecycle handling

## 🎯 Development Roadmap & Priorities

### ✅ COMPLETED MILESTONES

#### Core Architecture & Infrastructure ✅
- Clean Architecture implementation with proper layer separation
- MVVM pattern with reactive state management
- Dependency injection with Koin
- Database architecture with Room (schema v5)
- Network layer with Ktor3 and proper error handling

#### UI/Visual Features ✅
- Drag & drop bookshelf reordering
- 3D book spine effects with realistic rendering
- Coil3 image loading integration
- Material 3 design system implementation

#### Quality & Reliability ✅
- All tests passing (53 tests, 0 failures)
- API compliance (User-Agent, conditional logging)
- Security hardening (ProGuard, HTTPS, no secrets)
- Build system optimization

### 🚨 IMMEDIATE PRIORITIES (Next Development Session)

#### 1. Bookshelf Export/Import Implementation (HIGH PRIORITY) ⭐
**Timeline**: 2-3 hours implementation
**Status**: Ready to implement - no dependencies
**User Flow**: Share shelf → Android share sheet → Recipient imports → Books appear in collection

**Action Items**:
// Core components to implement:
- BookshelfExportService.kt - JSON Export/Import functionality
- BookshelfExportData.kt - Data class for export format
- Import UI components - User-friendly import flow with validation
- Android share sheet integration - Standard platform sharing


**Technical Details**:
- JSON serialization of shelf data (books, name, material, metadata)
- Android Intent.createChooser() for sharing
- Import validation with user-friendly error messages
- No server required - pure local file sharing

#### 2. Test Coverage Expansion (MEDIUM PRIORITY)
**Current**: 14% file coverage (11 test files / 79 source files)
**Target**: 50%+ initial coverage improvement
**Focus Areas**:
- Export/Import service testing
- Network layer (`KtorRemoteBookDataSource`)
- Database layer (DAOs, migrations)
- Error handling and validation

#### 3. Configuration Management (LOW PRIORITY)
**Issues**: Hard-coded BASE_URL and API parameters
**Solution**: Move to BuildConfig for better maintainability

### 🔮 FUTURE MILESTONES

#### Enhanced Features
- Advanced search filters and recommendation engine
- Cloud synchronization with user accounts (optional)
- Offline-first improvements and data caching
- Affiliate link service integration (optional monetization)

#### Performance & Scalability
- Database query optimization for large collections
- Image loading performance improvements with advanced caching
- Background processing optimization
- Analytics and crash reporting for stability

#### Store Deployment
- Basic privacy policy (simple data collection disclosure)
- App store listing optimization for "Books & Reference" category
- Beta testing with friends and family
- Standard Play Store submission process

## Next Session Action Items

### 🎯 **Immediate Tasks (Ready to Start)**
1. **Implement `BookshelfExportService.kt`** - Core Export/Import functionality with JSON serialization
2. **Create `BookshelfExportData.kt`** - Data class defining export format and validation
3. **Add export UI** - "Share Shelf" button in BookshelfScreen with Android share integration
4. **Add import UI** - Import flow in BookcaseScreen with validation and error handling
5. **Create Export/Import tests** - Comprehensive testing for data integrity and edge cases

### 📋 **Session Planning**
- **Estimated Time**: 2-3 hours for complete Export/Import implementation
- **Complexity**: Low-Medium (leverages existing architecture patterns)
- **Dependencies**: None (all prerequisites completed)
- **Outcome**: Working bookshelf sharing between devices with zero legal overhead

### 📈 **Success Metrics**
- Export: Generate shareable JSON from any bookshelf
- Share: Android share sheet integration working smoothly
- Import: Validate and import shared bookshelves successfully
- User Experience: Intuitive flow from share to import with clear feedback
- Testing: Comprehensive coverage for Export/Import edge cases