# Plan: Tech Debt — Code Review Fixes

## Context

Issues identified during code review of the `remove-sharing-add-qr-code-bc` branch. Most are pre-existing patterns; item 3 (dead state fields) was introduced in this branch.

## Issues

### 1. Hardcoded strings in BookshelfScreen

**Files:** `BookshelfScreen.kt`

Hardcoded strings that should use `stringResource()`:
- `"BC"` badge text (line ~118)
- `"Switch to natural arrangement"` / `"Tidy shelf"` content descriptions (line ~148-149)

**Fix:** Extract to `strings.xml`, use `stringResource()`.

### 2. Club code logged in plaintext

**Files:** `BookClubCodeGeneratorImpl.kt`

Generated codes are logged via Timber at debug level. Verify that the release Timber configuration does not plant a debug tree. If it does, remove or redact the code from log messages.

**Fix:** Audit `Application.onCreate()` Timber setup. If release builds plant a tree, change log to `"Generated code attempt %d"` without the code value.

### 3. Unrendered state fields: syncMessage / isSyncing

**Files:** `BookshelfState.kt`, `BookshelfViewModel.kt`, `BookshelfScreen.kt`

`syncMessage` and `isSyncing` are actively set during `syncBookClubBooks()` in the ViewModel but never rendered in the UI. The fields are not dead — they are maintained correctly — but the user never sees sync status.

**Fix:** Decide: display sync status to the user (e.g. a snackbar or progress indicator), or remove the fields and the code that sets them.

### 4. Missing tests: BookClubCodeGeneratorImpl

**Files:** `BookClubCodeGeneratorImpl.kt`

No unit tests. Key scenarios:
- Successful generation on first attempt
- Retry logic when code already exists
- Failure after MAX_RETRIES exhausted
- Network error propagation from `remoteDataSource`

**Fix:** Add test class with stub `RemoteSyncDataSource`.

### 5. Missing tests: ClubOperationsImpl

**Files:** `ClubOperationsImpl.kt`

Non-trivial logic (two-step lookup/join flow, `@Volatile lastLookedUpCode`, result mapping) with no dedicated tests.

**Fix:** Add test class covering lookup → join flow, error mapping, and concurrent access edge cases.

### 6. Magic number: book club limit (duplicated constant)

**Files:** `BookcaseScreen.kt` (~line 354, 538), `CreateBookClubUseCaseImpl.kt`, `JoinBookClubUseCaseImpl.kt`

`5` is hardcoded for the book club limit in `BookcaseScreen.kt`. `MAX_BOOK_CLUBS = 5` already exists but is duplicated across two use case companions (`CreateBookClubUseCaseImpl` and `JoinBookClubUseCaseImpl`), and the UI references neither — it hardcodes the literal.

**Fix:** Define a single `MAX_BOOK_CLUBS` constant in one canonical location (e.g. a domain-level companion or constants object). Remove the duplicates in both use cases and the hardcoded `5` in BookcaseScreen, referencing the single constant everywhere.

### 7. ClubOperationsImpl bypasses UseCase layer

**Files:** `ClubOperationsImpl.kt`

Several methods (`deleteBookClub`, `syncBookToClub`, `removeBookFromClub`, `updateClubStyle`, etc.) call the repository directly, bypassing UseCases. The project constitution requires ViewModel → UseCase → Repository.

**Fix:** Create UseCases for each direct repository call. This is the largest item — may warrant its own PR.

### 8. Smart cast workaround in ClubOperationsImpl

**Files:** `ClubOperationsImpl.kt` (line ~48-49)

```kotlin
if (parseResult is Result.Error) { return ... }
val code = (parseResult as Result.Success).data
```

The explicit `as` cast is redundant after the `is` check. A `when` block is cleaner.

**Fix:** Refactor to `when (parseResult)` block.

### 9. BookshelfViewModel else → Unit catch-all

**Files:** `BookshelfViewModel.kt` (line ~136)

`else -> Unit` silently swallows unhandled actions. New actions won't trigger a compile error if unhandled.

**Fix:** Remove `else` to make the `when` exhaustive, or add a log for unhandled actions.

## Priority

| # | Effort | Risk | Priority |
|---|--------|------|----------|
| 1 | Small  | Low  | Do first — quick string extraction |
| 2 | Small  | Med  | Verify Timber config, quick fix |
| 3 | Small  | Low  | Decide display vs remove (fields are active, just unrendered) |
| 4 | Medium | Med  | Important for regression safety |
| 5 | Medium | Med  | Important for regression safety |
| 6 | Small  | Low  | Consolidate duplicated constant + UI reference |
| 7 | Large  | Low  | Architectural consistency, own PR |
| 8 | Small  | Low  | Nitpick, do with other refactors |
| 9 | Small  | Low  | Compile-time safety improvement |

## Execution

Items 1, 2, 3, 6, 8, 9 can be done in a single cleanup PR. Items 4 and 5 are a testing PR. Item 7 is its own PR due to scope.
