# 📚 MyBookshelf Release Notes

**Personal Library Management for Android**

---

## v1.2.0 - Book Clubs (January 2026)

👥 **Collaborative Book Clubs!**

### What's New
- **Book Clubs** - Create collaborative shelves and invite friends to join
- **Shared Ratings** - Rate books and see your club's average ratings
- **Discussion Threads** - Comment on books with messaging-style conversations
- **Real-time Sync** - Books added by any member sync to everyone instantly

### Features
- Create book clubs from any shelf with 8-character invite codes
- Join clubs via invite link, deep link, or manual code entry
- Add/remove books collaboratively (syncs to all members)
- Rate books (1-5 stars) and see club average
- Threaded comments with edit/delete support
- Owner controls: rename, restyle, delete club
- Member controls: leave club (keeps books as personal shelf)
- Limits: 5 book clubs per user, 20 books per shelf

### Deep Link Support
- Share via `mybookshelf://club/{code}` deep links
- Web landing page at mybookshelf.co.uk/club/{code}
- Automatic app launch and join flow

### Technical
- 597 total tests (542 unit + 55 instrumented)
- 50 UseCases across 10 domains
- 16 new book club use cases
- Enterprise-grade Firestore security rules
- Full offline-first architecture maintained

### Privacy
- Club data stored securely in Firebase Firestore
- Only members can access club content
- Creator-only management controls
- Secure invite code system

---

## v1.1.0 - Cloud Sync (December 2025)

🎉 **First full release with cloud sync!**

### What's New
- **Cloud Sync** - Sign in with Google to sync your library across devices
- **Automatic Backup** - Your books and shelves sync to secure cloud storage
- **Offline-First** - Works without an account, sync is optional
- **Guest Data Import** - Choose whether to import existing data when signing in

### Features
- Cross-device sync via Firebase Firestore
- Background sync with WorkManager (every 15 minutes)
- Immediate sync on data changes
- Conflict resolution (last-write-wins)
- Per-user data isolation
- Clean sign-out with data clearing option

### Technical
- 510 total tests (448 unit + 62 integration)
- 280 production Kotlin files
- 35 UseCases across 8 domains
- Enterprise-grade sync architecture
- Full offline support maintained

### Privacy
- Synced data stored securely in Google Cloud (Firebase)
- No analytics or tracking
- No data shared with third parties
- Open source and transparent

---

## v1.0.2 - Google Sign-In (December 2025)

### What's New
- **Google Sign-In** - Sign in with your Google account for a personalized experience
- **Clean Architecture Auth** - Enterprise-grade authentication using modern Credential Manager API

### Features
- One-tap Google sign-in with automatic account selection
- Secure authentication via Firebase Auth
- Sign out option in settings menu
- Persistent sign-in state across app restarts

### Technical
- 26 new auth tests (337 unit tests total)
- Pure domain layer with zero Android dependencies
- Consistent Result pattern error handling

---

## v1.0.1 - Performance & Polish (December 2025)

### What's New
- **Search Performance** - 37% faster search with reduced debounce (450ms→250ms)
- **Image Loading** - Instant placeholder pattern eliminates perceived wait time
- **Book Details** - New reading status, notes, and metadata fields
- **Improved Logging** - Timber integration for better debugging

### Features
- Welcome screen and first-time onboarding experience
- Three-dot overflow menu for shelf and book actions
- Long-press to rename shelves
- Reading status tracking for books
- New book detail fields (personal notes, reading progress)

### Improvements
- Architecture refactoring with extracted handlers and callbacks
- Centralized image URL construction in ApiConfig
- Smaller image sizes for faster loading (10x improvement in search)
- Sticky shelf actions at bottom of detail screen
- Tutorial operations isolated from shelf/book operations
- Export/Import UseCases refactored for consistency
- Literal search queries instead of split word searches

### Bug Fixes
- Fixed race condition bug with search
- Fixed onBack overwriting save in viewmodel
- Fixed bug where change occurs before dialog closes
- Fixed Chrome security warnings on share links
- Prevented renaming of tutorial shelf
- Prevented sharing empty bookshelves

---

## v1.0.0 - Initial Release (January 2025)

🎉 **First stable release of MyBookshelf!**

### What's New
- **Production Ready** - No longer in alpha, fully tested and stable
- **Complete Feature Set** - All core features implemented and polished
- **Offline-First** - All your data stays on your device, no cloud required
- **Open Source** - MIT licensed, available on GitHub

### Key Features
- 📚 **Personal Library** - Create and organize custom bookshelves
- 🔍 **Book Search** - Find books from Open Library's 20M+ collection
- 🎨 **Beautiful UI** - Material 3 design with realistic shelf effects
- 📤 **Share Collections** - Export/import shelves between devices
- 🎯 **Drag & Drop** - Intuitive shelf and book organization
- 🔒 **Privacy First** - No tracking, no ads, no data collection

### Requirements
- Android 9.0+ (API 28+)
- ~15MB storage space
- Internet connection for book search only

### Installation
Download the APK from the release assets below and install on your Android device.

---

## Previous Alpha Releases

## v1.0.4-alpha - Testing Infrastructure & Documentation (October 2025)

### 🧪 **Testing Overhaul**
- **137 Total Tests** - 94 unit tests / 17 integration tests / 26 E2E tests
- **Google 70/20/10 Compliance** - Professional testing pyramid achieved (68/12/19 actual)
- **Complete Coverage** - All 4 ViewModels, 17+ UseCases, and 3 Repositories fully tested
- **E2E Workflows** - End-to-end tests for shelf creation, book addition/removal, reordering, and deletion
- **Integration Tests** - Real Room database testing with proper test isolation
- **Test Utilities** - Deterministic test infrastructure with TestIdGenerator and TestTimeProvider

### 📚 **Documentation Excellence**
- **TESTING_PATTERNS.md** - Comprehensive 61KB testing guide with patterns and examples
- **Privacy Policy** - Clear privacy-first statement in README (local-only, no tracking)
- **MIT License** - Open source licensing for portfolio transparency
- **Clean Architecture Docs** - Complete documentation of UseCase pattern implementation

### 🏗️ **Technical Improvements**
- **ProGuard Configuration** - Updated and validated for release builds
- **Build Validation** - All 137 tests passing, clean compilation
- **Code Quality** - Zero TODOs in production code, consistent patterns throughout
- **Architecture Compliance** - 100% adherence to Clean Architecture principles

### 📊 **Quality Metrics**
- **Test Coverage**: 38% file coverage (57 test files / 150 production files)
- **Test Distribution**: 68% unit / 12% integration / 19% E2E (near-perfect pyramid)
- **Production Readiness**: 98% complete with professional documentation

---

## v1.0.3-alpha - Web Page Redesign (October 2025)

### 🎨 **Visual Improvements**
- **Material 3 Theme** - GitHub Pages now match app's professional bookstore aesthetic
- **Authentic Shelf Textures** - Wood texture backgrounds from app's drawable resources
- **Consistent Branding** - Brown/cream color palette consistent across web and mobile
- **Enhanced Typography** - Roboto font with improved hierarchy and spacing

### 🌐 **Web Experience**
- **Redesigned Landing Page** - Professional showcase with updated v1.0.3-alpha badge
- **Improved Share Page** - Clean import flow with better visual feedback
- **Mobile Optimized** - Fully responsive design for all screen sizes
- **Performance** - Lightweight design with optimized asset loading

### 📦 **Assets**
- **New Assets Directory** - `docs/assets/` for web resources
- **Shelf Texture Integration** - Real wood texture at 15% opacity for subtle effect

---

## v1.0.1-alpha - Self-Contained Sharing (October 2025)

### 🚀 **New Features**
- **Self-Contained Share Links** - Share URLs now contain all data (no server/storage required)
- **Permanent Links** - Share links never expire and work even after closing the app
- **URL Compression** - GZip compression reduces share URL size by 70-80%
- **Shorter URLs** - Removed redundant parameters for cleaner sharing
- **URL Length Validation** - Automatic validation prevents sharing shelves that are too large

### 🔒 **Security & UX Improvements**
- **Fixed Chrome Security Warnings** - Removed auto-redirect behavior that triggered "unsafe site" warnings
- **Click-to-Open UX** - User now clicks to open app (more trustworthy, better UX)
- **Enhanced Error Messages** - More helpful messages for share link issues

### 🧪 **Testing & Quality**
- **25 New Tests** - Comprehensive test coverage for encoding/decoding functionality
- **TDD Implementation** - Base64Encoder and UrlEncodedShareTokenService built test-first
- **Clean Architecture** - Proper layer separation with no architecture violations

### 🏗️ **Technical Improvements**
- **Base64 + GZip Encoding** - Efficient data encoding with URL-safe characters
- **Domain Layer Cleanup** - Moved infrastructure concerns to proper layers
- **Error Handling** - New `SHARE_LINK_TOO_LARGE` error with user-friendly messages

---

## v1.0.0-alpha - Pre Release (October 2025)

**Personal Library Management for Android**

---

## ✨ Features

### 📖 **Core Functionality**
- **Personal Library Management** - Create and organize custom bookshelves with drag & drop
- **Book Search Integration** - Powered by Open Library API with access to 20M+ books
- **Visual Organization** - 3D book spine effects with realistic shadows and materials
- **Export/Import System** - Share collections via deep links between devices
- **Offline-First Design** - All data stored locally with Room database

### 🎨 **User Experience**
- **Material 3 Design** - Modern, polished Android UI
- **Drag & Drop Reordering** - Intuitive shelf organization with visual feedback
- **Advanced Search** - Author/title filtering with debounced search
- **Custom Shelf Styles** - Multiple materials (wood, metal, etc.) with color options
- **Professional Animations** - Smooth transitions and loading states

---

## 🏗️ Technical Highlights

### **Architecture Excellence**
- **Clean Architecture** - Domain/Data/Presentation layers with proper separation
- **MVVM Pattern** - ViewModels with reactive StateFlow management
- **UseCase Pattern** - 17+ business logic UseCases with consistent error handling
- **Repository Pattern** - Clean abstraction between data sources
- **Dependency Injection** - Koin 4.1.1 with proper scoping

### **Modern Android Stack**
- **Jetpack Compose** - 100% declarative UI with Material 3
- **Room Database** - Local persistence with schema versioning (v5)
- **Ktor 3.3.0** - Enterprise HTTP client with retry policies
- **Coil3** - Modern image loading with network integration
- **Navigation Compose** - Type-safe navigation with deep link support

### **Production Quality**
- **Comprehensive Testing** - 53 unit tests with proper mocking
- **Error Handling** - Result pattern with standardized error formatting
- **ProGuard Enabled** - Optimized release builds with R8 minification
- **API Compliance** - Proper User-Agent headers and production logging

---

## 📱 Installation

### **Requirements**
- Android 9.0+ (API 28+)
- ~15MB storage space
- Internet connection for book search

### **Download & Install**
1. Download `MyBookshelf-v1.0.0-alpha.apk` from the Assets section below
2. Enable "Install from unknown sources" in Android Settings > Security
3. Install and enjoy your personal bookshelf organizer!

**Note**: This is an alpha pre-release for portfolio demonstration. The app is fully functional but may receive updates. No data collection or analytics.

---

## 🛠️ For Developers

### **Project Structure**
```
uk.co.zlurgg.mybookshelf/
├── app/                 # Application & navigation
├── core/               # Shared infrastructure
├── bookshelf/          # Feature domain
│   ├── data/          # Repository implementations
│   ├── domain/        # Business logic & entities
│   └── presentation/  # UI layer
└── di/                # Dependency injection
```

### **Key Patterns Demonstrated**
- **Clean Architecture** with proper domain boundaries
- **Reactive Programming** with Kotlin coroutines & flows
- **Modern Testing** with shared utilities and fakes
- **Configuration Management** with BuildConfig integration
- **Professional Git Workflow** with proper release management

### **Source Code**
- **Repository**: [GitHub.com/zlurgg/MyBookshelf](https://github.com/zlurgg/MyBookshelf)

---

## 🎯 Portfolio Context

This app demonstrates:
- **Production Android Development** - Complete app lifecycle from architecture to release
- **Modern Kotlin Expertise** - Latest frameworks and best practices
- **Software Architecture Skills** - Enterprise-level patterns and structure
- **Testing Proficiency** - Comprehensive unit testing with proper patterns
- **DevOps Knowledge** - Professional build and release processes

Built by **Joseph Brightman** ([@zlurgg](https://github.com/zlurgg)) as part of Android development portfolio.

---

## 📧 Contact

**Developer**: Joseph Brightman
**GitHub**: [@zlurgg](https://github.com/zlurgg)
**Location**: Leicester, UK

---

*Released: October 2025 | Version: 1.0.0-alpha | Build: Pre-Release*