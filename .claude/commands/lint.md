# Run Detekt Lint

Run detekt static analysis and report any violations.

## Instructions

1. Run detekt:
   ```bash
   ./gradlew detekt
   ```

2. Parse the output and report:
   - Total violations found
   - Group by rule category (complexity, style, naming, etc.)
   - Show file:line references for each violation

3. If violations are found, offer to fix them

4. If detekt passes, confirm clean result
