# 📚 My Bookshelf

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**My Bookshelf** is a beautifully designed Android app built with Kotlin and Jetpack Compose that lets users create, customize, and organize their personal book collections. Designed for readers and collectors who want a simple, elegant way to manage their library.

> 📚 Organize your collection.
> 🔍 Discover new books.
> 📤 Share your shelves with friends.

---

## ✨ Features

- 🧱 **Custom Shelf Organization** – Create and rearrange shelves in your bookcase
- 🔍 **Search & Add Books** – Use Open Library API for easy book lookup
- 🔗 **Bookshelf Sharing** – Export and share your shelves via deep links
- 👥 **Book Clubs** – Create collaborative shelves with friends, share ratings and comments
- 🎨 **Custom Shelf Styles** – Choose from different materials and colors
- ☁️ **Cloud Sync** – Optional Google sign-in to sync across devices
- 📱 **Offline-First** – Works without an account, data stored locally

---

## 🔒 Privacy & Data

**MyBookshelf is a privacy-first application:**
- ✅ Works fully offline – no account required
- ✅ Optional Google sign-in enables cloud sync across devices
- ✅ Synced data stored securely in Google Cloud (Firebase)
- ✅ No analytics, tracking, or telemetry
- ✅ No data shared with third parties
- ✅ Internet used for book search (Open Library) and optional cloud sync
- ✅ Open source and transparent – review the code yourself

**Your data stays yours – use it locally or sync it to the cloud.**

---

## 🧱 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room (local SQLite database)
- **Cloud Sync**: Firebase Firestore with WorkManager
- **Authentication**: Google Sign-In (optional)
- **APIs**: Open Library API for book search and metadata
- **Image Loading**: Coil3 with Ktor3 network integration
- **Dependency Injection**: Koin
- **Navigation**: Jetpack Navigation Compose
- **Sharing**: Deep link sharing system

---

## 🛠 Setup (Coming Soon)

Instructions for setting up the project locally will be added here.

---

## 🚀 Planned Features

- Personal ratings and reading status tracking
- Search and filter books within shelves
- Manga & comic support via AniList/ComicVine APIs
- Affiliate link integration (Amazon Associates, Bookshop.org)
- Reading habit reminders and progress tracking
- Advanced shelf themes and customization
- Enhanced search filters and book recommendations

---

## 📸 Preview

### Welcome & Tutorial
<div style="text-align: center;">
  <img src="docs/screenshots/welcome_screen.jpg" width="400" alt="Welcome Screen">
  <img src="docs/screenshots/tutorial-book.jpg" width="400" alt="Tutorial Book">
</div>

### Bookcase Management
<div style="text-align: center;">
  <img src="docs/screenshots/bookcase-default.jpg" width="400" alt="Bookcase View">
  <img src="docs/screenshots/dropdown-menu.jpg" width="400" alt="Shelf Menu">
  <img src="docs/screenshots/bookcase-reorder.jpg" width="400" alt="Reorder Shelves">
</div>

### Shelf Customization
<div style="text-align: center;">
  <img src="docs/screenshots/create-new-shelf.jpg" width="400" alt="Create New Shelf">
  <img src="docs/screenshots/rename-shelf.jpg" width="400" alt="Rename Shelf">
  <img src="docs/screenshots/style-change.jpg" width="400" alt="Change Shelf Style">
</div>

### Book Organization
<div style="text-align: center;">
  <img src="docs/screenshots/empty-bookshelf.jpg" width="400" alt="Empty Bookshelf">
  <img src="docs/screenshots/messy-bookshelf.jpg" width="400" alt="Messy Bookshelf">
  <img src="docs/screenshots/tidy-bookshelf.jpg" width="400" alt="Tidy Bookshelf">
</div>
<div style="text-align: center;">
  <img src="docs/screenshots/slide-to-delete.jpg" width="400" alt="Slide to Delete">
</div>

### Book Search & Details
<div style="text-align: center;">
  <img src="docs/screenshots/book-search.jpg" width="400" alt="Book Search">
  <img src="docs/screenshots/book-search-2.jpg" width="400" alt="Search Results">
</div>
<div style="text-align: center;">
  <img src="docs/screenshots/bookdetails-top.jpg" width="400" alt="Book Details - Top">
  <img src="docs/screenshots/bookdetails-bottom.jpg" width="400" alt="Book Details - Bottom">
</div>

### Sharing
<div style="text-align: center;">
  <img src="docs/screenshots/share-shelf.jpg" width="400" alt="Share Shelf">
</div>

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Copyright © 2025 Joseph Brightman ([@zlurgg](https://github.com/zlurgg))

---

## 📧 Contact

Built by Joseph Brightman ([@zlurgg](https://github.com/zlurgg))
