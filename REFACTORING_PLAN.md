# Core Package Refactoring Plan

## 🎯 Objective
Move generic, reusable components from the bookshelf package to core for better separation of concerns and improved architecture.

## 📋 Refactoring Tasks

### ✅ High Priority Moves (Generic System Services)

#### 1. TimeProvider Service
- **From**: `bookshelf/domain/service/TimeProvider.kt` → **To**: `core/domain/service/TimeProvider.kt`
- **From**: `bookshelf/data/service/SystemTimeProvider.kt` → **To**: `core/data/service/SystemTimeProvider.kt`
- **Reason**: Completely generic system time abstraction, no bookshelf-specific logic

#### 2. SystemLanguageProvider Service
- **From**: `bookshelf/domain/service/SystemLanguageProvider.kt` → **To**: `core/domain/service/SystemLanguageProvider.kt`
- **From**: `bookshelf/data/service/AndroidSystemLanguageProvider.kt` → **To**: `core/data/service/AndroidSystemLanguageProvider.kt`
- **Reason**: Generic system language detection (Open Library mapping can stay or be made configurable)

#### 3. IdGenerator Service (Rename from BookshelfIdGenerator)
- **From**: `bookshelf/domain/service/BookshelfIdGenerator.kt` → **To**: `core/domain/service/IdGenerator.kt`
- **From**: `bookshelf/data/service/UuidBookshelfIdGenerator.kt` → **To**: `core/data/service/UuidIdGenerator.kt`
- **Reason**: Generic ID generation pattern, remove "Bookshelf" specificity

### 🔄 Medium Priority Moves (Utilities)

#### 4. Text Processing Utilities
- **Extract**: `levenshteinDistance` algorithm from `BookSorter.kt`
- **To**: `core/util/TextUtils.kt`
- **Reason**: Generic string similarity algorithms, reusable across features

#### 5. Color Generation (Rename from BookColorGenerator)
- **From**: `bookshelf/domain/service/BookColorGenerator.kt` → **To**: `core/presentation/util/MaterialColorGenerator.kt`
- **Reason**: HSL color conversion and matte finishing algorithms are generic visual utilities

## 🔧 Implementation Steps

1. **Create Core Directory Structure**
   - `core/domain/service/`
   - `core/data/service/`
   - `core/util/`

2. **Move Files with Renames**
   - Copy files to new locations
   - Update package declarations
   - Rename classes/interfaces where needed

3. **Update All References**
   - Update imports across all files
   - Update DI configuration (`AppModule.kt`)
   - Update test files

4. **Clean Up Original Files**
   - Remove original files from bookshelf package
   - Verify no orphaned references

5. **Test & Validate**
   - Run all tests to ensure no regressions
   - Build project to verify all imports resolved
   - Update any missed references

## 📝 Detailed Changes

### TimeProvider
```kotlin
// NEW: core/domain/service/TimeProvider.kt
interface TimeProvider {
    fun currentTimeMillis(): Long
}

// NEW: core/data/service/SystemTimeProvider.kt
class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
```

### IdGenerator (renamed from BookshelfIdGenerator)
```kotlin
// NEW: core/domain/service/IdGenerator.kt
interface IdGenerator {
    fun generateId(): String
}

// NEW: core/data/service/UuidIdGenerator.kt
class UuidIdGenerator : IdGenerator {
    override fun generateId(): String = UUID.randomUUID().toString()
}
```

### TextUtils (extracted from BookSorter)
```kotlin
// NEW: core/util/TextUtils.kt
object TextUtils {
    fun levenshteinDistance(s1: String, s2: String): Int {
        // Extracted algorithm
    }

    fun calculateStringSimilarity(s1: String, s2: String): Double {
        // New utility method
    }
}
```

## 🔍 Files to Update

### DI Configuration
- `di/AppModule.kt` - Update service bindings

### Existing Bookshelf Files Affected
- All files importing the moved services
- Test files referencing moved classes
- ViewModels using these services

### Benefits After Refactoring
- ✅ Cleaner separation of concerns
- ✅ Better testability of core utilities
- ✅ Improved reusability for future features
- ✅ More consistent architecture following existing core pattern
- ✅ Easier to mock generic services in tests

## 🚨 Risk Mitigation
- Move one service at a time to minimize breakage
- Keep comprehensive test coverage during refactoring
- Use IDE refactoring tools where possible
- Verify builds pass after each major change