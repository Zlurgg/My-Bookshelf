# Pre-commit Check

Run compilation, tests, and lint in sequence to verify everything is clean before committing.

## Instructions

1. **Compile**:
   ```bash
   ./gradlew compileDebugKotlin
   ```
   Stop and report if compilation fails.

2. **Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
   Stop and report if tests fail.

3. **Lint**:
   ```bash
   ./gradlew detekt
   ```
   Report any violations.

4. **Summary**: Report pass/fail for each step. If all pass, confirm ready to commit.
