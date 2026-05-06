# Plan: Tech Debt — Code Review Fixes

## Context

Issues identified during code review of the `remove-sharing-add-qr-code-bc` branch. All are pre-existing — none were introduced by the sharing removal or QR code work.

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

### 3. Dead state fields: syncMessage / isSyncing

**Files:** `BookshelfState.kt`, `BookshelfViewModel.kt`, `BookshelfScreen.kt`

`syncMessage` and `isSyncing` are set in state but never rendered in the UI. Either display them (e.g. a snackbar or banner during sync) or remove them.

**Fix:** Decide: display sync status to the user, or remove the dead fields.

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

### 6. Magic number: book club limit

**Files:** `BookcaseScreen.kt` (~line 354, 538)

`5` is hardcoded for the book club limit. `ShelfOperationsHandler.MAX_PERSONAL_SHELVES` exists for personal shelves — book clubs should have a similar constant.

**Fix:** Add `MAX_BOOK_CLUBS` constant, reference it from UI and handler.

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
| 3 | Small  | Low  | Decide display vs remove |
| 4 | Medium | Med  | Important for regression safety |
| 5 | Medium | Med  | Important for regression safety |
| 6 | Small  | Low  | Quick constant extraction |
| 7 | Large  | Low  | Architectural consistency, own PR |
| 8 | Small  | Low  | Nitpick, do with other refactors |
| 9 | Small  | Low  | Compile-time safety improvement |

## Execution

Items 1, 2, 3, 6, 8, 9 can be done in a single cleanup PR. Items 4 and 5 are a testing PR. Item 7 is its own PR due to scope.
