# Plan: Detekt Setup for Android (NowInAndroid Approach)

## Overview
Set up Detekt with the formatting module (which wraps ktlint) following NowInAndroid's configuration approach. This gives us static analysis AND code formatting in a single tool.

## Key Learnings from Previous Attempt
- Separate ktlint plugin caused issues with inline comments and trailing commas
- NowInAndroid uses Spotless+ktlint, but detekt-formatting wraps ktlint the same way
- Need to disable problematic rules upfront, not after hitting issues
- Use `ktlint_function_naming_ignore_when_annotated_with` for Compose/Test functions

---

## Commit Strategy

Commit at logical checkpoints to keep changes reviewable and allow rollback:

| Commit | After Phase | Message |
|--------|-------------|---------|
| 1 | Phase 1 | `build: Enable Gradle parallel, caching, and configuration-cache` |
| 2 | Phase 2 | `build: Add Detekt with formatting module (ktlint wrapper)` |
| 3 | Phase 3 | `chore: Add pre-commit hooks for Detekt` |
| 4 | Phase 4 | `perf(compose): Add stability annotations to state classes` |
| 5 | Phase 5 | `refactor: Extract magic numbers to named constants` |
| 6 | Phase 6 | `docs: Add Detekt setup documentation` |

---

## Phase 1: Gradle Build Optimizations

**File: `gradle.properties`**

```properties
# Build Performance
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

**COMMIT 1:** `build: Enable Gradle parallel, caching, and configuration-cache`

---

## Phase 2: Detekt with Formatting Module

### Step 2.1: Add to version catalog
**File: `gradle/libs.versions.toml`**

```toml
[versions]
detekt = "1.23.7"

[plugins]
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }

[libraries]
detekt-formatting = { module = "io.gitlab.arturbosch.detekt:detekt-formatting", version.ref = "detekt" }
```

### Step 2.2: Configure root build
**File: `build.gradle.kts` (root)**

```kotlin
plugins {
    // ... existing plugins
    alias(libs.plugins.detekt) apply false
}
```

### Step 2.3: Apply and configure in app module
**File: `app/build.gradle.kts`**

```kotlin
plugins {
    // ... existing plugins
    alias(libs.plugins.detekt)
}

// At bottom of file
detekt {
    config.setFrom("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

dependencies {
    // Add formatting module (wraps ktlint)
    detektPlugins(libs.detekt.formatting)
}
```

### Step 2.4: Create .editorconfig (NowInAndroid approach)
**File: `.editorconfig` (project root)**

```editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_size = 4
max_line_length = 120

# Trailing commas - allow but don't enforce
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true

# Skip function naming for Compose and Test (NowInAndroid approach)
ktlint_function_naming_ignore_when_annotated_with = Composable, Test

# Disabled rules (following NowInAndroid)
ktlint_standard_filename = disabled
ktlint_standard_function-signature = disabled
ktlint_standard_class-signature = disabled
ktlint_standard_function-expression-body = disabled
ktlint_standard_binary-expression-wrapping = disabled
ktlint_standard_chain-method-continuation = disabled
ktlint_standard_condition-wrapping = disabled
ktlint_standard_multiline-loop = disabled
ktlint_standard_backing-property-naming = disabled
ktlint_standard_function-literal = disabled
ktlint_standard_function-type-modifier-spacing = disabled
# Don't enforce trailing commas (allow them, but don't add them)
ktlint_standard_trailing-comma-on-call-site = disabled
ktlint_standard_trailing-comma-on-declaration-site = disabled
# Don't enforce comment placement (caused 90+ issues before)
ktlint_standard_comment-wrapping = disabled

[*.xml]
indent_size = 4

[*.{json,yml,yaml}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

### Step 2.5: Create Detekt configuration
**File: `app/detekt.yml`**

```yaml
# Detekt Configuration for MyBookshelf
# https://detekt.dev/docs/rules/

build:
  maxIssues: 0
  excludeCorrectable: false

config:
  validation: true
  warningsAsErrors: false

# Formatting rules (from detekt-formatting module / ktlint)
# Most config is in .editorconfig, but we can disable categories here
formatting:
  active: true
  # Let .editorconfig handle specific rule config

complexity:
  LongMethod:
    threshold: 100
    excludes: ['**/presentation/**']
  LongParameterList:
    functionThreshold: 10
    constructorThreshold: 15
    excludes: ['**/presentation/**']
  LargeClass:
    threshold: 500
  ComplexCondition:
    threshold: 5
  CyclomaticComplexMethod:
    threshold: 20
  TooManyFunctions:
    thresholdInFiles: 25
    thresholdInClasses: 25
    thresholdInInterfaces: 25
    thresholdInObjects: 20
    thresholdInEnums: 15

coroutines:
  active: true
  GlobalCoroutineUsage:
    active: true

empty-blocks:
  active: true
  EmptyCatchBlock:
    allowedExceptionNameRegex: "_|(ignore|expected).*"

exceptions:
  active: true
  TooGenericExceptionCaught:
    excludes: ['**/test/**', '**/androidTest/**']
  TooGenericExceptionThrown:
    active: true

naming:
  active: true
  FunctionNaming:
    # Handled by ktlint via .editorconfig (ignore @Composable, @Test)
    active: false
  PackageNaming:
    # Project uses underscores (book_detail, search_components)
    active: false

performance:
  active: true
  SpreadOperator:
    excludes: ['**/test/**', '**/androidTest/**']

potential-bugs:
  active: true

style:
  active: true
  MagicNumber:
    excludes: ['**/test/**', '**/androidTest/**', '**/presentation/**']
    ignorePropertyDeclaration: true
    ignoreLocalVariableDeclaration: true
    ignoreConstantDeclaration: true
    ignoreCompanionObjectPropertyDeclaration: true
    ignoreAnnotation: true
    ignoreNamedArgument: true
    ignoreEnums: true
  MaxLineLength:
    excludes: ['**/test/**', '**/androidTest/**']
    maxLineLength: 120
  ReturnCount:
    max: 4
    excludeGuardClauses: true
  UnusedPrivateMember:
    allowedNames: "(_|ignored|expected|serialVersionUID|.*Preview)"
  WildcardImport:
    excludes: ['**/test/**', '**/androidTest/**']
```

**COMMIT 2:** `build: Add Detekt with formatting module (ktlint wrapper)`

---

## Phase 3: Git Pre-commit Hooks

### Step 3.1: Create pre-commit hook
**File: `scripts/pre-commit`**

```bash
#!/bin/bash
# Pre-commit hook for MyBookshelf
# Runs detekt (includes formatting checks via detekt-formatting)

echo "Running detekt..."
./gradlew detekt --daemon

if [ $? -ne 0 ]; then
    echo "❌ Detekt found issues. Please fix them before committing."
    exit 1
fi

echo "✅ All checks passed!"
exit 0
```

### Step 3.2: Create hook installation script
**File: `scripts/install-hooks.sh`**

```bash
#!/bin/bash
# Install git hooks for MyBookshelf

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOKS_DIR="$SCRIPT_DIR/../.git/hooks"

cp "$SCRIPT_DIR/pre-commit" "$HOOKS_DIR/pre-commit"
chmod +x "$HOOKS_DIR/pre-commit"

echo "✅ Git hooks installed successfully!"
```

**COMMIT 3:** `chore: Add pre-commit hooks for Detekt`

---

## Phase 4: Compose Stability Annotations

**Add `@Stable` to state classes with mutable collections:**
1. `bookshelf/presentation/bookshelf/search_components/BookSearchState.kt`
2. `auth/presentation/SignInState.kt`
3. `bookshelf/presentation/book_detail/BookDetailState.kt`
4. `bookshelf/presentation/bookcase/BookcaseState.kt`
5. `bookshelf/presentation/bookshelf/BookshelfState.kt`
6. `sync/domain/model/SyncState.kt`

**Add `@Immutable` to fully immutable data classes:**
1. `bookshelf/presentation/bookcase/components/ShelfDisplayState.kt`
2. `bookshelf/presentation/deeplink/DeepLinkState.kt`
3. `bookshelf/domain/model/BookClub.kt`
4. `bookshelf/domain/model/BookClubReview.kt`
5. `bookshelf/domain/model/BookClubComment.kt`
6. `sync/domain/model/GuestDataInfo.kt`
7. `update/domain/model/UpdateInfo.kt`
8. `sync/domain/model/SyncProgress.kt`

**Add `@Stable` to domain models with collections:**
1. `bookshelf/domain/model/Book.kt`
2. `bookshelf/domain/model/Bookshelf.kt`

**COMMIT 4:** `perf(compose): Add stability annotations to state classes`

---

## Phase 5: Magic Number Fixes

**Files to update:**
1. `core/domain/error/ErrorMapper.kt` - Extract HTTP status codes
2. `core/data/image/ImageLoaderFactory.kt` - Extract timeout constants
3. `core/data/network/HttpClientFactory.kt` - Extract MAX_RETRIES
4. `bookshelf/domain/usecase/book_detail/UpdateBookMetadataUseCaseImpl.kt` - Extract MAX_RATING, MAX_NOTES_LENGTH
5. `bookshelf/domain/usecase/bookclub/UpsertBookClubReviewUseCaseImpl.kt` - Extract MAX_RATING

**COMMIT 5:** `refactor: Extract magic numbers to named constants`

---

## Phase 6: Documentation Updates

### Step 6.1: Update CLAUDE.md
Add Code Quality section with:
- Static analysis commands (`./gradlew detekt`)
- Configuration files (`detekt.yml` + `.editorconfig`)
- Pre-commit hook installation instructions

### Step 6.2: Update docs/PROJECT_STANDARDS_TEMPLATE.md
Add "Code Quality Tools" section covering:
- Detekt setup and commands
- NowInAndroid configuration approach
- Key `.editorconfig` settings
- Pre-commit hooks

**COMMIT 6:** `docs: Add Detekt setup documentation`

---

## Files Summary

| File | Action | Phase |
|------|--------|-------|
| `gradle.properties` | Edit | 1 |
| `gradle/libs.versions.toml` | Edit | 2 |
| `build.gradle.kts` (root) | Edit | 2 |
| `app/build.gradle.kts` | Edit | 2 |
| `.editorconfig` | Create | 2 |
| `app/detekt.yml` | Create | 2 |
| `scripts/pre-commit` | Create | 3 |
| `scripts/install-hooks.sh` | Create | 3 |
| 16 State/Domain class files | Edit | 4 |
| 5 UseCase/Utility files | Edit | 5 |
| `CLAUDE.md` | Edit | 6 |
| `docs/PROJECT_STANDARDS_TEMPLATE.md` | Edit | 6 |

---

## Verification

1. **Build:** `./gradlew assembleDebug` - should compile cleanly
2. **Detekt:** `./gradlew detekt` - should pass with 0 issues (or baseline if needed)
3. **Tests:** `./gradlew testDebugUnitTest` - all 542 tests should pass
4. **Pre-commit:** Stage a file and commit - hook should run detekt

---

## Key Differences from Previous Attempt

| Issue Before | Solution Now |
|--------------|--------------|
| Separate ktlint plugin | Use detekt-formatting (ktlint wrapper) |
| Trailing commas enforced | Disabled via .editorconfig |
| Inline comments flagged | `comment-wrapping` disabled |
| Compose function naming | `ktlint_function_naming_ignore_when_annotated_with` |
| Multiple tools to run | Single `./gradlew detekt` command |
| Inconsistent config | Everything in detekt.yml + .editorconfig |