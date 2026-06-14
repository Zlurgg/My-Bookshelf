# Play Store Listing — copy & ASO

Drop-in copy for the Play Console listing fields. Warm/personal voice, only claims features that ship today (see audit notes at bottom).

## App title

`My Bookshelf — Book Tracker` (30/30 chars)

Keeps the brand name first; "Book Tracker" earns the listing in search results for the most-typed query.

## Short description (80 char limit)

```
Track every book you've read, want to read, or own — your library, on your phone.
```

80/80 chars. Leads with the verb users search for (`track`) and three concrete states. Avoids puffery.

**Alt #1 (warmer):**
```
A personal bookshelf for every book you've read, want to read, or own.
```
70/80 chars. Less keyword-dense, slightly friendlier.

**Alt #2 (club-led):**
```
Track your books, organise your shelves, and read together with friends.
```
72/80 chars. Surfaces the clubs feature in the short description.

Pick one before publishing. Recommend the first.

## Full description (4000 char limit)

```
My Bookshelf is a private, offline-first way to keep track of every book you've read, want to read, or own — without handing your reading life over to a social network.

Add a book once and it stays on your phone. Search, rate, take notes, set a reading status, mark it as purchased — all without needing a signal. Your shelves, your data, your device.

When you do want to share, join a book club. Create a club for your friends or reading group, post reviews, leave comments, and see what everyone else thought — all kept in sync across members.

WHAT YOU CAN DO

• Build a personal library — Add books from Google Books and Open Library. Organise them across as many shelves as you like. Rename, reorder, delete whenever.

• Track what matters to you — Reading status (Not read / Reading / Finished), star rating, personal notes, and whether you actually own a copy.

• Work offline — Your library lives on your device. Browse, rate, and annotate without a network. No background syncing of personal data, no surprise cloud bills, no leaks.

• Join book clubs — Create a club, invite friends, post reviews, leave comments, see who's in. Club activity syncs across members in real time.

• Sign in your way — Sign in with Google to back up club memberships and reviews, or skip sign-in entirely and use the app as a guest.

• Looks right at night — Follows your phone's dark mode automatically.

• You're in control — Delete your account from inside the app whenever you want. We tell you exactly what gets removed before you confirm.

WHO IT'S FOR

• Readers who like a clean, focused tracker without ads, paywalls, or a feed
• People who want their personal reading list to stay personal
• Book clubs and reading groups who want a shared space for reviews
• Anyone tired of social book apps that turn reading into a leaderboard

PRIVACY

Your personal shelves, books, ratings, and notes are stored only on your device — they never leave it. Only book club activity (the part you choose to share) is synced to the cloud.

For sign-in, cloud sync, crash reports, and usage analytics, the app uses Google's Firebase services (Authentication, Firestore, Crashlytics, Analytics). These follow Google's standard privacy controls. There are no ads and no third-party advertising trackers.

GOOD TO KNOW

• Active development — feedback goes straight to a real inbox from inside the app (Account → Send Feedback). Send us what's broken, what's missing, what you wish was there.
• Designed for phones in portrait. Tablet and landscape support is on the roadmap.
• No ISBN scanner today — books are added by search. Scanner is on the roadmap if there's demand.

Built by a one-person team. If the app earns a place on your phone, a Play Store rating is the kindest way to say so.
```

~2100 chars used of 4000. Keyword coverage embedded naturally: `book tracker`, `personal library`, `book club`, `reading list`, `shelves`, `Google Books`, `Open Library`, `reading status`, `offline`, `private`, `rating`, `notes`. No claim made about features that don't ship.

## Target keywords

Picked for relevance to actual feature set, ordered by volume × competitiveness fit for a niche personal-library app.

| Keyword | Reason to target | Where it lives in copy |
| --- | --- | --- |
| `book tracker` | Highest-intent query for this category. App title earns it. | Title, body |
| `book club` | Real shipping feature, high search volume, less crowded than `book tracker`. | Title (alt #2), body |
| `personal library` | Matches the positioning (private, on-device). | Body |
| `reading list` | Common search term, fits the use case. | Body |
| `book log` / `reading log` | Long-tail, fits trackers. | Body (`log` not present yet — add if needed) |
| `book journal` | Maps onto the "notes" feature. | Body |
| `to read list` / `tbr` | "TBR" is reader slang for to-be-read. Worth a mention if natural. | (not present — consider adding) |
| `offline book tracker` | Differentiator — most competitors require accounts. | Body |
| `bookshelf` | Brand match. | Title, body |

Skip: `Goodreads alternative` (Play discourages competitor name-dropping), `ISBN scanner` (we don't have one — promising it would attract uninstalls).

## Localisation note

Initial launch UK-English only. If/when adding locales, the short description should be re-translated rather than auto-translated — the keyword phrasing matters per language.

## Audit notes (what informed the copy)

Verified by direct codebase check on 2026-06-14. Anything claimed in the description has a code-level proof point:

- Offline-first personal library — `book/data/repository/BookRepositoryImpl.kt` reads/writes via Room DAO; no Firestore writes for shelves/books.
- Multiple shelves (CRUD) — `book/domain/usecase/{Create,Rename,Delete}ShelfUseCase`.
- Search across Google Books + Open Library — both `RemoteBookDataSource` implementations present with automatic fallback.
- Reading status enum — `book/domain/model/ReadingStatus.kt` confirms `NOT_READ`, `READING`, `FINISHED` (no `ABANDONED` despite older docs).
- Rating, notes, purchased toggle — `bookdetail/presentation/BookDetailScreen.kt` (`PersonalNotesCard`, `PurchasedToggleCard`, rating field).
- Book clubs cloud-synced — `bookclub/data/remote/FirestoreBookClubRemoteDataSourceImpl.kt`; collections: `bookClubs`, `members`, `reviews`, `comments`.
- Google sign-in + Guest mode — `auth/presentation/components/SignInButton.kt`, `ContinueAsGuestButton.kt`.
- Dark mode — `core/presentation/ui/theme/Theme.kt` uses `darkColorScheme` + `isSystemInDarkTheme()`.
- Account deletion — `account/domain/usecase/DeleteAccountUseCaseImpl.kt`.
- In-app feedback (mentioned in body) — `account/presentation/AccountScreenRoot.kt` (added in `feat(account): add Send Feedback`).

Things deliberately NOT claimed because they don't ship:
- ISBN barcode scanner
- Cross-device sync of personal library
- Tablet / landscape adaptive layouts
- CSV / library export
- Lending or loan tracker

If any of the above are added later, update the description in the same release.
