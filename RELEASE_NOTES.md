# 📚 MyBookshelf Release Notes

**Personal Library Management for Android**

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