# Release Command

Create a new release for MyBookshelf app.

## Arguments
Version number: $ARGUMENTS (e.g., "1.0.1")

## Instructions

### Step 1: Validate Version
- If no version provided, read current version from `app/build.gradle.kts` and ask user what the new version should be
- Validate format is semver (x.y.z) - reject versions like "1.0" or "v1.0.1"
- Confirm version is higher than current

### Step 2: Update Version Numbers

**app/build.gradle.kts:**
- Find `versionCode = X` and increment by 1
- Find `versionName = "X.Y.Z"` and set to new version

**CLAUDE.md:**
- Update any version references (search for old version string)

### Step 3: Generate Changelog

Get commits since last tag:
```bash
git log $(git describe --tags --abbrev=0 2>/dev/null || echo "HEAD~20")..HEAD --oneline
```

Categorize commits into sections based on commit message keywords:
- **Features**: commits containing "add", "new", "feature", "implement"
- **Improvements**: commits containing "update", "improve", "enhance", "refactor"
- **Bug Fixes**: commits containing "fix", "bug", "resolve", "correct"

### Step 4: Update RELEASE_NOTES.md

Add new version section at the TOP of the file (after the header), using this format:

```markdown
## v{VERSION} - {Feature Summary} ({Month} {Year})

### What's New
- {categorized changes from git log}

### Features
- {feature items}

### Improvements
- {improvement items}

### Bug Fixes
- {bug fix items}

---
```

### Step 5: Build Release APK

```bash
./gradlew assembleRelease
```

- Verify build succeeds (exit code 0)
- Report APK location: `app/build/outputs/apk/release/app-release.apk`
- Report APK size

### Step 6: Create GitHub Release

```bash
"C:\Program Files\GitHub CLI\gh.exe" release create v{VERSION} \
  app/build/outputs/apk/release/app-release.apk \
  --title "MyBookshelf v{VERSION}" \
  --notes-file RELEASE_NOTES.md
```

- Report the release URL on success

### Step 7: Report Status

Show summary of what was done:
- Files modified: `app/build.gradle.kts`, `RELEASE_NOTES.md`, `CLAUDE.md`
- New version: v{VERSION}
- Release URL

Remind user to review and commit:
```
Please review the changes, then commit:
  git add .
  git commit -m "Release v{VERSION}"
  git push
```

## Error Handling

- If build fails, stop and report the error
- If GitHub release fails, stop and report the error (changes are still saved locally)
- If no git tags exist, use last 20 commits for changelog
