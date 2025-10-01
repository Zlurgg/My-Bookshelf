# Plan: Replace Token-Based Sharing with Base64 URL-Encoded Data

## Status: ✅ READY TO IMPLEMENT (Deep Scan Complete)

**Overall Risk Level**: 🟢 **LOW** - No breaking changes identified. Clean architecture swap with minor optimizations needed.

**Estimated Timeline**: 3-4 hours (includes GZip compression + comprehensive testing)

---

## Current Architecture

### Export Flow
1. Shelf → JSON serialization
2. JSON → Generate UUID token
3. Store token → JSON mapping in memory (`ConcurrentHashMap`)
4. Share URL with token: `https://zlurgg.github.io/My-Bookshelf/share/?name=Release+Test#9fdf859f-0a6e-4d74-923d-ee4c98fded65`

### Import Flow
1. Token extracted from URL hash
2. Lookup token in memory storage
3. Retrieve JSON data
4. Deserialize and import bookshelf

### Problem
- **Tokens stored in RAM only** (`ConcurrentHashMap` in `LocalShareTokenService`)
- **Data lost when sender closes app** - recipient gets broken link
- **7-day expiration meaningless** - tokens rarely survive app restart
- **Poor sharing UX** - sender must keep app running for recipient to import

## New Architecture

### Export Flow
1. Shelf → JSON serialization
2. JSON → Base64 URL-safe encoding
3. Share URL with encoded data: `https://zlurgg.github.io/My-Bookshelf/share/?name=Release+Test#{base64EncodedShelfData}`

### Import Flow
1. Encoded data extracted from URL hash
2. Base64 decode to JSON
3. Deserialize and import bookshelf

### Benefits
- ✅ **Self-contained URLs** - all data in the link itself
- ✅ **No storage required** - no database, no memory, no server
- ✅ **Works forever** - links never expire
- ✅ **Independent sharing** - sender can close app immediately
- ✅ **Simpler architecture** - removes entire token storage layer

## Implementation Steps

### 1. Create Base64 Encoding Utility with GZip Compression
**File**: `app/src/main/java/uk/co/zlurgg/mybookshelf/core/util/Base64Encoder.kt`

**⚠️ IMPORTANT**: GZip compression is **REQUIRED**, not optional. See URL length analysis below.

```kotlin
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object Base64Encoder {
    fun encode(data: String): String {
        // Step 1: GZip compression (70-80% size reduction)
        val compressed = ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { gzip ->
                gzip.write(data.toByteArray(Charsets.UTF_8))
            }
            bos.toByteArray()
        }

        // Step 2: URL-safe Base64 encoding
        return android.util.Base64.encodeToString(
            compressed,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
    }

    fun decode(encoded: String): String {
        // Step 1: Base64 decode
        val compressed = android.util.Base64.decode(
            encoded,
            android.util.Base64.URL_SAFE
        )

        // Step 2: GZip decompression
        return GZIPInputStream(compressed.inputStream()).use { gzip ->
            gzip.readBytes().toString(Charsets.UTF_8)
        }
    }
}
```

**Why GZip is Required**:
- JSON is highly compressible (70-80% reduction)
- Without compression: 5-book shelf = ~3.3KB Base64 (exceeds 2KB browser limit)
- With compression: 5-book shelf = ~670 bytes Base64 ✅
- See "URL Length Analysis" section for detailed breakdown

### 2. Update ShareTokenService Interface
**File**: `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/domain/service/ShareTokenService.kt`

**Option A**: Keep interface unchanged, swap implementation
- `generateToken(shelfJsonData)` → returns Base64-encoded data as "token"
- `getShelfDataByToken(token)` → decodes Base64 "token" back to JSON
- Remove `cleanupExpiredTokens()` or make it a no-op

**Option B**: Rename interface to reflect new purpose
- Rename to `ShareDataEncoder` or similar
- Update method names for clarity

**Recommendation**: Option A (minimal changes)

### 3. Implement UrlEncodedShareTokenService
**File**: `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/data/service/UrlEncodedShareTokenService.kt`

```kotlin
import uk.co.zlurgg.mybookshelf.core.util.Base64Encoder
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class UrlEncodedShareTokenService : ShareTokenService {
    override suspend fun generateToken(shelfJsonData: String): Result<String, DataError.Local> {
        return try {
            val encoded = Base64Encoder.encode(shelfJsonData)
            Result.Success(encoded)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getShelfDataByToken(token: String): Result<String, DataError.Local> {
        return try {
            val decoded = Base64Encoder.decode(token)
            Result.Success(decoded)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun cleanupExpiredTokens(): Result<Unit, DataError.Local> {
        // No-op: no storage means nothing to clean up
        return Result.Success(Unit)
    }
}
```

### 4. Update Dependency Injection
**File**: `app/src/main/java/uk/co/zlurgg/mybookshelf/di/AppModule.kt`

Replace:
```kotlin
single<ShareTokenService> {
    LocalShareTokenService(get(), get())
}
```

With:
```kotlin
single<ShareTokenService> {
    UrlEncodedShareTokenService()
}
```

### 5. Testing Strategy

#### Unit Tests
- **Base64Encoder**: Test encoding/decoding with various shelf sizes
- **UrlEncodedShareTokenService**: Test encode/decode operations
- **URL Length Validation**: Ensure URLs don't exceed browser limits (~2000 chars for compatibility)

#### Integration Tests
- **Export Flow**: Create shelf → export → verify Base64 in URL
- **Import Flow**: Take Base64 URL → import → verify shelf created correctly
- **Edge Cases**:
  - Empty shelves
  - Large shelves (many books)
  - Special characters in shelf names
  - Unicode book titles

#### Manual Testing
1. Share a shelf from device A
2. Close app on device A
3. Click link on device B
4. Verify shelf imports successfully

### 6. Update GitHub Pages (Optional)
**File**: `docs/share/index.html`

Current JavaScript extracts token from URL hash and passes to deep link handler:
```javascript
const hash = window.location.hash.substring(1); // Gets token
const appUrl = `mybookshelf://share/${shareToken}`;
```

**No changes required** - the encoded data is treated as a token, so existing logic works.

## URL Length Analysis (Deep Scan Findings)

### Maximum URL Lengths
- **Browser standard (safe)**: 2000 characters (IE, older browsers)
- **Modern browsers**: ~100KB (Chrome, Firefox)
- **Android Intent**: ~100KB (very generous)
- **GitHub Pages**: No specific limit (uses modern browser limits)
- **Recommended**: Keep under 2KB for maximum compatibility

### JSON Payload Structure Analysis

**Sample Export Data** (from `BookshelfExportData.kt`):
```json
{
  "formatVersion": 1,
  "exportedAt": "2025-10-01T16:29:00Z",
  "appName": "My Bookshelf",
  "bookshelf": {
    "name": "Release Test",
    "shelfStyle": "OAK",
    "books": [
      {
        "id": "OL123456W",
        "title": "The Lord of the Rings",
        "authors": ["J.R.R. Tolkien"],
        "imageUrl": "https://covers.openlibrary.org/b/id/12345-L.jpg",
        "description": "Epic fantasy novel...",
        "languages": ["eng"],
        "firstPublishYear": "1954",
        "averageRating": 4.5,
        "ratingCount": 10000,
        "numPages": 1178,
        "numEditions": 500,
        "purchased": false,
        "spineColor": -8355712
      }
    ]
  }
}
```

**Per-Book Size**:
- With `prettyPrint = true`: ~450 bytes
- With `prettyPrint = false`: ~320 bytes
- After GZip compression: ~80 bytes

### Size Estimates by Shelf Size

#### Without Compression (Base64 only)
| Books | JSON (pretty) | JSON (minified) | Base64 | URL Total | Status |
|-------|--------------|----------------|--------|-----------|--------|
| 5     | ~2.5KB       | ~1.7KB         | ~3.3KB | ~3.4KB    | ❌ Exceeds 2KB limit |
| 10    | ~5KB         | ~3.5KB         | ~6.6KB | ~6.7KB    | ❌ Far exceeds |
| 15    | ~7.5KB       | ~5KB           | ~10KB  | ~10KB     | ❌ Far exceeds |

#### With GZip + Base64 (Recommended)
| Books | JSON (minified) | GZip | Base64 | URL Total | Status |
|-------|----------------|------|--------|-----------|--------|
| 1     | ~500B          | ~200B | ~270B  | ~350B     | ✅ Perfect |
| 5     | ~1.7KB         | ~500B | ~670B  | ~750B     | ✅ Excellent |
| 10    | ~3.5KB         | ~1KB  | ~1.3KB | ~1.4KB    | ✅ Good |
| 15    | ~5KB           | ~1.5KB| ~2KB   | ~2.1KB    | ⚠️ May fail on IE/old browsers |
| 20    | ~7KB           | ~2KB  | ~2.7KB | ~2.8KB    | ⚠️ Likely to fail on old browsers |
| 50    | ~17KB          | ~5KB  | ~6.6KB | ~6.7KB    | ❌ May exceed some limits |

**Compression Ratio**: 70-80% size reduction (JSON is highly compressible)

### Required Changes

1. **✅ CRITICAL: Disable Pretty Print**
   - **File**: `JsonBookshelfSerializer.kt` (line 22)
   - **Change**: `prettyPrint = false` (currently `true`)
   - **Impact**: Saves 30-40% space immediately

2. **✅ CRITICAL: Implement GZip Compression**
   - **File**: `Base64Encoder.kt` (new file)
   - **Impact**: Additional 70-80% reduction after minification
   - **Result**: 5-book shelf fits comfortably under 2KB limit

3. **⚠️ RECOMMENDED: Add 15-Book Warning**
   - Show warning dialog: "This shelf has {count} books. Share links for large shelves may not work on older devices. Consider sharing in smaller batches."
   - Display before sharing for shelves >15 books
   - Optional "Share Anyway" button

4. **✅ OPTIONAL: Add URL Length Validation**
   - Validate encoded URL length before sharing
   - Log warning in debug builds if >2KB
   - Show error if >10KB (definitely too large)

## Migration Path

### Phase 1: Implement New System
1. Create `Base64Encoder` utility
2. Implement `UrlEncodedShareTokenService`
3. Update DI to use new implementation
4. Test thoroughly

### Phase 2: Deploy & Monitor
1. Build and test APK
2. Monitor URL lengths in testing
3. Verify sharing works across devices
4. Check for edge cases (special characters, large shelves)

### Phase 3: Cleanup (Optional)
1. Remove `LocalShareTokenService` (old implementation)
2. Remove unused `ConcurrentHashMap` storage code
3. Update documentation

## Deep Scan Findings

### ✅ Architecture Compatibility
- **ShareTokenService** used in only 2 places:
  - `ExportBookshelfUseCase.kt` (line 28: `generateToken()`)
  - `DeepLinkImportUseCaseImpl.kt` (line 22: `getShelfDataByToken()`)
- **No direct coupling** to `LocalShareTokenService` implementation
- **Clean interface swap** confirmed - zero breaking changes

### ✅ URL Generation (AndroidShareService.kt)
- Uses `URLEncoder.encode()` for shelf name (line 26) ✅
- Format: `${baseUrl}/?name=${encodedName}#${token}` (line 27)
- **Compatible**: Token is opaque string - can be UUID or Base64 data

### ✅ Deep Link Handling (MyBookShelfApp.kt)
- Extract token: `uri.path?.removePrefix("/")` (line 196)
- Pass to: `DeepLinkAction.ImportFromToken(token)` (line 198)
- **Compatible**: Treats token as opaque string

### ✅ JSON Serialization (JsonBookshelfSerializer.kt)
- Uses `kotlinx.serialization` with `@Serializable` annotations
- **Current setting**: `prettyPrint = true` (line 22) ⚠️ **Must change to `false`**
- **Structure**: `BookshelfExportData` → `ExportedBookshelf` → `ExportedBook[]`
- **No changes needed** to serialization logic

### ✅ Error Handling
- Proper `Result<T, DataError>` pattern throughout
- `ErrorFormatter.formatOperationError()` for user-facing messages
- **Compatible**: Base64 decode errors map to `DataError.Local.UNKNOWN`

### ⚠️ Error Message Update Required
**File**: `DeepLinkViewModel.kt` (line 59)
- **Current**: `"Link may be expired or invalid: ${result.error}"`
- **Change to**: `"Link is invalid or corrupted: ${result.error}"`
- **Reason**: Links never expire with Base64 encoding

### ✅ GitHub Pages (docs/share/index.html)
- Extract token: `const hash = window.location.hash.substring(1);` (line 216)
- Deep link: `mybookshelf://share/${shareToken}` (line 244)
- **Compatible**: JavaScript treats token as opaque string - no changes needed

### ✅ Android Deep Link (AndroidManifest.xml)
- Scheme: `mybookshelf://share` (line 32)
- **Compatible**: Deep link handler receives full path/fragment

### ✅ Special Characters Handling
- `kotlinx.serialization`: Properly escapes JSON strings ✅
- `android.util.Base64.URL_SAFE`: Avoids `/`, `+`, `=` characters ✅
- No risk of URI parsing issues ✅

### ⚠️ Testing Gap
- **Current tests**: Only `BookshelfRepositoryImplTest.kt` found
- **No export/import tests** exist
- **Required**: New tests for encoding/decoding logic

---

## Files to Modify

### New Files (Create)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/core/util/Base64Encoder.kt`
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/data/service/UrlEncodedShareTokenService.kt`
- `app/src/test/java/uk/co/zlurgg/mybookshelf/core/util/Base64EncoderTest.kt`
- `app/src/test/java/uk/co/zlurgg/mybookshelf/bookshelf/data/service/UrlEncodedShareTokenServiceTest.kt`

### Modified Files (Edit)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/di/AppModule.kt` (lines 97-102: DI configuration)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/data/service/JsonBookshelfSerializer.kt` (line 22: `prettyPrint = false`)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/presentation/deeplink/DeepLinkViewModel.kt` (line 59: error message)

### Optional Modifications
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/domain/service/ShareTokenService.kt` (interface rename - optional)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/presentation/bookshelf/BookshelfViewModel.kt` (add 15-book warning - recommended)

### Files to Delete (After Testing)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/data/service/LocalShareTokenService.kt` (old implementation - no longer needed)

## Risk Assessment

### Overall Risk Level: 🟢 **LOW**

| Category | Risk Level | Mitigation |
|----------|-----------|------------|
| Architecture Compatibility | 🟢 None | Clean interface swap confirmed via deep scan |
| Breaking Changes | 🟢 None | Zero breaking changes - fully backward compatible |
| URL Length Issues | 🟡 Medium | GZip compression (required) + 15-book warning |
| Special Characters | 🟢 None | Properly handled by JSON serialization + URL_SAFE Base64 |
| Deep Link Parsing | 🟢 None | URL_SAFE Base64 avoids problematic characters |
| Error Handling | 🟢 None | Existing Result pattern handles all cases |
| Testing Coverage | 🟡 Medium | New tests required for encoding logic |
| GitHub Pages | 🟢 None | No changes needed - treats token as opaque string |

### Mitigation Strategies
1. **✅ URL Length**: GZip compression (REQUIRED) + optional 15-book warning
2. **✅ Encoding Errors**: Comprehensive error handling with user-friendly messages (already in place)
3. **✅ Testing**: Add comprehensive unit tests for Base64Encoder and UrlEncodedShareTokenService
4. **✅ Error Messages**: Update "expired" language to "corrupted" in DeepLinkViewModel

## Success Criteria

- ✅ Share a bookshelf and close the sender's app
- ✅ Recipient can still import the bookshelf successfully
- ✅ URLs work indefinitely (no expiration)
- ✅ All existing tests pass
- ✅ New tests cover encoding/decoding edge cases
- ✅ URL lengths stay under 2000 chars for typical shelves (5-15 books)

## Timeline Estimate

- **Research & Planning**: ✅ Completed (deep scan done)
- **Implementation**: 3-4 hours
  - Base64Encoder with GZip: 45 min
  - UrlEncodedShareTokenService: 30 min
  - JsonBookshelfSerializer update: 5 min
  - DeepLinkViewModel error message: 5 min
  - DI configuration update: 10 min
  - Unit tests (Base64Encoder): 45 min
  - Unit tests (UrlEncodedShareTokenService): 30 min
  - Integration testing: 30 min
  - Optional 15-book warning UI: 30 min
- **Total**: 3-4 hours for complete implementation and testing

---

## Next Session Checklist

### Phase 1: Core Implementation (Required)
- [ ] Create `Base64Encoder.kt` with GZip compression
- [ ] Create `UrlEncodedShareTokenService.kt`
- [ ] Update `JsonBookshelfSerializer.kt` (`prettyPrint = false`)
- [ ] Update `DeepLinkViewModel.kt` error message
- [ ] Update `AppModule.kt` DI configuration
- [ ] Delete `LocalShareTokenService.kt` (after testing)

### Phase 2: Testing (Required)
- [ ] Create `Base64EncoderTest.kt` with comprehensive tests
- [ ] Create `UrlEncodedShareTokenServiceTest.kt`
- [ ] Manual end-to-end testing:
  - [ ] Share 5-book shelf → close app → import on another device
  - [ ] Share 10-book shelf → verify URL length <2KB
  - [ ] Test special characters in titles/authors
  - [ ] Test Unicode characters (international names)
  - [ ] Test large descriptions

### Phase 3: Enhancements (Optional)
- [ ] Add 15-book warning dialog in `BookshelfViewModel`
- [ ] Add URL length validation before sharing
- [ ] Add debug logging for compression statistics

### Phase 4: Cleanup (After Testing)
- [ ] Verify all tests pass
- [ ] Remove `LocalShareTokenService.kt`
- [ ] Build release APK
- [ ] Update GitHub release with new APK
