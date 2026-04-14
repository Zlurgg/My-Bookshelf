# Run Tests

Run unit tests and report results. Optional: specify a module or test class.

## Arguments
$ARGUMENTS (optional: module name, test class, or "all")

## Instructions

1. If no arguments provided, run all debug unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

2. If a specific module/class is provided, scope the test run accordingly

3. Parse the output and report:
   - Total tests run
   - Passed / Failed / Skipped counts
   - Any failure details with file:line references
   - Link to HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`

4. If tests fail, suggest fixes based on the error messages
