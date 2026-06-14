# Play Store Screenshots — design & capture spec

What a designer (or you in Figma/Canva) needs to produce the phone screenshots for the Play Store listing. Captions and ordering align with `play-store-listing.md` — same offline-first / clubs framing, same warm tone, no claims for features that don't ship.

## Format requirements

| Field | Value |
| --- | --- |
| File type | PNG (24-bit, no alpha) or JPEG |
| Size | 1080 × 1920 px (9:16 portrait). Min 320 px, max 3840 px per side. |
| Count | Min 2, max 8. Recommend 7 (see below). |
| Aspect | Between 16:9 and 9:16. 9:16 portrait is standard for phone listings. |
| Tablet variants | **Skip** — app isn't tablet-optimised; ship phone screenshots only. |

Play crops aggressively above the fold — the **first 2 screenshots are what 80% of browsers see**, so they have to land the value proposition on their own.

## Feature graphic (separate field)

| Field | Value |
| --- | --- |
| Size | 1024 × 500 px (~2:1 landscape) |
| Use | Top banner of the Play listing, plus YouTube preview frame if a promo video is added later. |
| Required | Yes, listing won't publish without it. |

**Recommended composition:** app icon left, headline "Your library, on your phone" centre-right, simple book-spine motif background in brand colours. No screenshot inside the feature graphic — Play already shows screenshots below it.

## Visual style (apply to every screenshot)

- **Background:** brand-tinted gradient or solid pastel — picks one and uses it consistently for slots 1–7.
- **Phone frame:** a stylised Pixel/generic Android frame around each captured screen (Canva / Figma both have free Android device mockups). Don't ship raw screenshots without a frame — that's what the tester report flagged.
- **Caption bar:** top third of the canvas. Headline (large, bold) above sub-caption (smaller, regular). Keep headline ≤5 words, sub-caption ≤10 words.
- **Theme mix:** slots 1–4 + 6–7 in **light** theme. Slot 5 in **dark** theme so the dark-mode capability is visible without spending a caption on it.
- **No emoji**, no fake hand pointers, no faux-3D book stacks.

## Slots (in upload order)

### Slot 1 — Hero: bookcase home

| | |
| --- | --- |
| **Source** | `bookcase/` home screen with at least 3 shelves visible, each populated with several real-looking book spines. |
| **Theme** | Light |
| **Headline** | Your library, on your phone |
| **Sub-caption** | Track every book you've read, want to read, or own |
| **Annotation** | None — let the shelves speak. |
| **Why first** | Single image conveys the entire app's value. Above-the-fold. |

### Slot 2 — Add a book: search

| | |
| --- | --- |
| **Source** | `BookSearchDialog` showing a populated result list (e.g. search for "Murakami"). |
| **Theme** | Light |
| **Headline** | Find any book, fast |
| **Sub-caption** | Search Google Books and Open Library |
| **Annotation** | Soft outline around the search results list. |
| **Why second** | Answers the immediate question "how do I add a book?". Above-the-fold. |

### Slot 3 — Book detail: rate, note, track

| | |
| --- | --- |
| **Source** | `bookdetail/` for a real book showing star rating set, personal note populated, reading status = Reading, Purchased toggle on. |
| **Theme** | Light |
| **Headline** | Track what matters to you |
| **Sub-caption** | Rating, notes, reading status, owned |
| **Annotation** | Three small callouts on the three cards (rating / notes / status). |
| **Why third** | Shows depth of the per-book tracking — the main "why this over a spreadsheet" moment. |

### Slot 4 — Shelves: organise your way

| | |
| --- | --- |
| **Source** | Either the multi-shelf view from slot 1 at a different angle/zoom, or a screen showing "create shelf" or shelf-rename flow. Pick whichever reads clearest. |
| **Theme** | Light |
| **Headline** | Organise your way |
| **Sub-caption** | As many shelves as you like — rename, reorder, delete |
| **Annotation** | None, or a single small "+" callout to imply add-shelf. |
| **Why fourth** | Reinforces flexibility / control. |

### Slot 5 — Dark mode: book detail at night

| | |
| --- | --- |
| **Source** | Same screen as slot 3 (book detail) but captured in **dark theme**. |
| **Theme** | Dark |
| **Headline** | Easy on the eyes |
| **Sub-caption** | Follows your phone's dark mode automatically |
| **Annotation** | None. |
| **Why fifth** | Shows the dark-mode feature without burning a slot on a copy-only slide. |

### Slot 6 — Book clubs

| | |
| --- | --- |
| **Source** | Book club detail / members view with a couple of member reviews visible. Use realistic-looking review text, not lorem ipsum. |
| **Theme** | Light |
| **Headline** | Read together |
| **Sub-caption** | Create clubs, post reviews, see what friends thought |
| **Annotation** | Soft outline around one review card. |
| **Why sixth** | Surfaces the social side; differentiates from a plain tracker. |

### Slot 7 — Offline & private

| | |
| --- | --- |
| **Source** | Bookcase or book-detail screen with a small "airplane mode" status icon visible top-right (composite it in if the real capture doesn't have it). Optionally a small overlay graphic showing a phone with a closed cloud icon and a lock. |
| **Theme** | Light |
| **Headline** | Yours, on your device |
| **Sub-caption** | Your library works offline and stays private |
| **Annotation** | A subtle "no network" indicator. |
| **Why seventh** | Frames the (deliberate) lack of personal-library sync as a privacy/offline strength, matching the listing copy. |

## Out of scope

- **8th screenshot.** 7 is enough — adding a thin one weakens the set. If a strong "Continue as Guest" capture exists, that's the natural 8th slot, but only if it's genuinely good.
- **Tablet 7" / 10" screenshots.** App is phone-portrait only. Don't supply tablet variants until adaptive layouts ship (see [[known-issues]]).
- **Promo video.** Nice-to-have, not blocking. Skip for v1 listing.
- **Localised screenshots.** Initial launch UK-English only. Add per-locale variants if/when localisation lands.

## How to capture the source screens

1. Build a debug APK (`./gradlew installDebug`) and run on a Pixel-class device or AVD set to 1080×1920 (or 1080×2400 — Play will accept any 9:16 portrait).
2. Seed the app with realistic-looking content — real book titles/authors, several shelves, a club with a couple of reviews. Don't ship "Untitled Shelf" or "TestUser1".
3. Capture with Android Studio's device screenshot tool (DDMS / Logcat → Screenshot button), or `adb shell screencap -p /sdcard/shot.png && adb pull /sdcard/shot.png`.
4. For dark mode (slot 5), toggle the OS dark mode before capture — the app follows system theme.
5. Hand the raw PNGs to the designer along with this spec.

## Designer deliverables

For each slot, deliver:
- Final 1080 × 1920 PNG with frame + caption + background
- Stripped 1080 × 1920 PNG (no caption, no frame) for re-use in social posts later
- Source Figma/Canva file in shared drive

Plus:
- Feature graphic 1024 × 500 PNG.
