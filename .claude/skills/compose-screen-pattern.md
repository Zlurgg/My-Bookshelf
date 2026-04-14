---
name: Compose Screen Pattern
description: How to implement screens with Root/Screen split, state hoisting, side effects, and previews
---

# Compose Screen Pattern

Load the full spec before implementing:
@docs/specs/patterns/compose-screens.md

Key points:
- `ScreenRoot` composable: owns ViewModel, handles navigation, passes state down
- `Screen` composable: pure UI, receives state + action lambda, no ViewModel reference
- State hoisting: pass `State` down, `(Action) -> Unit` up
- `LaunchedEffect` for navigation triggers and one-shot operations
- Extract reusable components to `components/` package
- Always provide `@Preview` with sample data
- Min touch target 48dp, content descriptions for accessibility
- Navigation via `NavHost` in the app-level navigation composable
