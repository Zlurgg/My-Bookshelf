# Known Issues

Living list of issues and tester feedback observed during closed testing. Update as items surface; remove (or move to a tracked PR) when resolved.

Why this exists: Google looks for evidence of iteration based on tester feedback before promoting from closed → production. Tracking issues here — including ones we choose not to fix — is the audit trail.

## UI

### Bright white flash on screen transitions (dark mode)

Navigation transitions briefly show a white frame between destinations in dark mode. Possibly present in light mode too but less perceptible against the lighter palette.

Likely the window/root surface background isn't following the theme during the NavHost enter/exit window, or one of the screens defaults to `Color.White` before its own `Surface` paints.

**Where to look:** `MainActivity` window background, root `Scaffold` / `Surface` in `MyBookShelfApp`, default `NavHost` transition.

## Book club

### Personal data not visible in club context

When viewing a book inside a club, the user can't see their own rating or notes — they have to leave the club flow to reference or update them.

**Proposed direction:** add a view-swap toggle on the book-in-club screen so the user can flip between **club view** (everyone's reviews) and **my data** (their own rating + notes), without cluttering either view.

## Search

Follow-ups from the Google Books outage fix (shipped in 1.0.3, see `search-google-outage.md`).

### Occasional very slow searches (20–60s+)

Two of about 15 timed searches on 1.0.3 took about 22s and over 60s; the same queries retried in about 5s. The likely cause is a stalled Open Library request: `HTTP_TIMEOUT_MILLIS` is 20s, and `HttpClientFactory` retries socket timeouts up to 3 times. Release builds don't log, so it's unconfirmed. This predates the outage fix.

**Decision:** don't act unless users report slow searches. If they do, consider a shorter timeout or fewer exception retries for Open Library.

### Open Library result quirks

- Some results show a literal `null` as the year (seen on "Breaking The Code of SSB Psy…" for `test`).
- The English-only filter is loose: a `harry potter` search included a Spanish edition ("Los Mundos Magicos De Harry Potter").

**Where to look:** `OpenLibraryRemoteBookDataSource` and the OL DTO → `Book` mapper (year formatting, language filter).

## Play Store listing

### Screenshots are raw mobile captures, not designed assets

Raised in the 2026-06-14 tester feedback report (item #2): the current Play Store screenshots are unframed device captures without callouts or branded backgrounds, which weakens listing conversion.

**Decision:** spec'd but deferred. See `play-store-screenshots.md` for the slot-by-slot design brief. Closed-testing reviewers don't depend on listing screenshots, so we won't block iteration on the redesign. Target executing the spec before the closed → production promotion.

## Process

New tester-reported issues land here first.

- Crash / data loss / sign-in blocker → fix-PR immediately, don't park here.
- UX paper-cut or feature request → add a section above, prioritise next iteration.
- Out-of-scope or won't-fix → still log here so we can show we triaged it.
