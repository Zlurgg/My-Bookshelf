# Handover: bookshelf/ Package Split Refactor

## What to do

Execute the plan at `docs/specs/plans/bookshelf-package-split.md`. It's a 3-phase mechanical refactor — move files, update packages/imports, rewire DI. No logic changes.

## Branch

`book-input-alt` — already checked out, clean working tree.

## Execution order

**Phase 1** (bookdetail → book/): 13 use case files + ~18 presentation files + 7 test files. Lowest risk, do first.

**Phase 2** (bookcase → bookcase/): Start with dead code removal (Step 2.1), then update BookshelfViewModel (Step 2.3), then move 17 use case + ~20 presentation files + 7 test files. Create new BookcaseModule.kt. **Important**: edit BookcaseViewModelTest (remove shareShelf from facade) BEFORE moving it in Step 2.9.

**Phase 3** (cleanup): Move 5 stale test files, remove empty dirs, update docs.

## Key decisions already made

- GetShelfByIdUseCase stays in **bookcase/** (not book/) — creates bookshelf/ → bookcase/ one-way dep
- Dead code: remove shareShelf from BookcaseUseCases + ShelfOperationsHandler.shareShelf()
- Handler singletons are correct (stateless). BookcaseClubActionHandler is VM-scoped (manual instantiation) — don't add to DI
- ClearUserDataUseCase binding stays in AuthModule.kt — only import path changes
- bookcase/ → welcome/ coupling is documented as a TODO, not addressed in this refactor

## Watch out for

- Use `git mv` for file moves (history preservation)
- `./gradlew clean` after each phase — stale class files from old packages cause confusing errors
- No `checkModules` Koin tests exist — do a manual smoke test (launch app, navigate all 3 screens) after Phase 2
- BookshelfModule.kt needs `import bookcase.domain.usecase.GetShelfByIdUseCase` for the VM binding (easy to miss — Step 2.8)
- MockUseCases.kt has 8 bookcase imports changing + MockShareBookshelfUseCase uses FQN (stays unchanged)

## Verification per phase

```bash
# After Phase 1 and 2:
./gradlew assembleDebug && ./gradlew testDebugUnitTest

# After Phase 3 (full):
./gradlew clean && ./gradlew assembleDebug && ./gradlew testDebugUnitTest && ./gradlew detekt
```

## Commits

1. `refactor(book): Move bookdetail feature from bookshelf/ to book/ package`
2. `refactor(bookcase): Extract bookcase feature into top-level package`
3. `chore: Fix stale test packages and update documentation`
