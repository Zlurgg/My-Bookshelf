# Commit Changes

Review and commit staged/unstaged changes with a conventional commit message.

## Instructions

1. Run `git status` and `git diff` to understand all changes
2. Run `git log --oneline -5` to see recent commit style
3. Categorize changes: feat, fix, refactor, test, docs, build, chore
4. Determine the scope from the module/feature touched
5. Draft a concise commit message following conventional commits: `type(scope): description`
6. Stage only relevant files (not generated files, secrets, or build artifacts)
7. Create the commit
8. Show the final `git log --oneline -3` to confirm

IMPORTANT: No "Co-Authored-By" footers. No "Generated with Claude Code" signatures.
