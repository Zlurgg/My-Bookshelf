# OpenLibrary Search API Reference

Last Updated: 2025-10-16
API Documentation: https://openlibrary.org/dev/docs/api/search
Search Guide: https://openlibrary.org/search/howto

## API Endpoints

### Search Books
```
GET https://openlibrary.org/search.json
```

**Parameters:**
- `q` - Search query (required)
- `limit` - Number of results (default: 100, max: 100)
- `offset` - Pagination offset
- `fields` - Comma-separated list of fields to return
- `sort` - Sort order (new, old, random, etc.)
- `language` - Language filter (e.g., "eng", "spa")

### Get Book Details
```
GET https://openlibrary.org/works/{WORK_ID}.json
```

---

## Search Query Syntax

OpenLibrary uses **Apache Solr** for search, supporting powerful query syntax.

### 1. Basic Search

```
q=hobbit
```
Searches ALL fields for the term "hobbit"

### 2. Exact Phrase Search (RECOMMENDED)

```
q="the hobbit"
```
Searches for the **exact phrase** "the hobbit" in order.

**Test Results:**
- Without quotes: `q=the hobbit` → **394 results** (too many)
- With quotes: `q="the hobbit"` → **284 results** (more precise) ✅

**Recommendation**: Use quotes for multi-word queries to improve relevance.

---

## Field-Specific Searches

Syntax: `field:value`

### Common Fields

| Field | Description | Example |
|-------|-------------|---------|
| `title` | Book title | `title:"the hobbit"` |
| `author` | Author name | `author:tolkien` |
| `subject` | Subject/genre | `subject:"science fiction"` |
| `place` | Place mentioned | `place:istanbul` |
| `person` | Person mentioned | `person:gandalf` |
| `publisher` | Publisher name | `publisher:harpercollins` |
| `language` | Language code | `language:eng` |
| `first_publish_year` | Publication year | `first_publish_year:1937` |

### Test Results: Title Field

- `title:the hobbit` → **348 results** (AND behavior)
- `title:"the hobbit"` → **274 results** (exact phrase) ✅

**Insight**: Quotes work with field-specific searches too!

---

## Boolean Operators

### AND (default)
```
q=lord rings
```
Matches documents with **BOTH** "lord" AND "rings"

### OR
```
q=hobbit OR "lord of the rings"
```
Matches documents with **EITHER** term

### NOT / Negation
```
q=tolkien -subject:"juvenile fiction"
```
Matches "tolkien" but **excludes** juvenile fiction

### Grouping with Parentheses
```
q=author:tolkien AND (title:hobbit OR title:"lord of the rings")
```

---

## Range Searches

### Year Ranges
```
q=first_publish_year:[1900 TO 2000]
```
Books published between 1900-2000

```
q=first_publish_year:[* TO 1800]
```
Books published before 1800

### Numeric Ranges
```
q=number_of_pages:[400 TO *]
```
Books with 400+ pages

---

## Wildcard Searches

Use `*` for partial matching:

```
q=title:harr*
```
Matches "Harry Potter", "Harrison", etc.

```
q=ddc:200*
```
Matches Dewey Decimal 200-299 (Religion)

---

## Complex Query Examples

### 1. Science Fiction Books by Asimov
```
q=author:asimov AND subject:"science fiction"
```

### 2. Travel Books about Istanbul
```
q=subject:travel AND place:istanbul
```

### 3. Children's Books OR Young Adult
```
q=subject:("juvenile fiction" OR "young adult")
```

### 4. Recent Fantasy Books
```
q=subject:fantasy AND first_publish_year:[2020 TO *]
```

---

## Current Implementation in MyBookshelf

### ViewModel Query Building (BookshelfViewModel.kt:232-238)

```kotlin
val (generalQuery, titleQuery, authorQuery) = when {
    searchByTitle && searchByAuthor → Triple(query, null, null)
    !searchByTitle && !searchByAuthor → Triple(query, null, null)
    searchByTitle && !searchByAuthor → Triple("", query, null)
    else → Triple("", null, query)
}
```

### Data Layer Query Building (KtorRemoteBookDataSource.kt:37-61)

```kotlin
private fun buildQuery(
    baseQuery: String,
    authorFilter: String?,
    titleFilter: String?
): String {
    val queryParts = mutableListOf<String>()

    if (baseQuery.isNotBlank()) {
        queryParts.add(baseQuery.trim())  // Currently NO quotes
    }

    authorFilter?.takeIf { it.isNotBlank() }?.let {
        queryParts.add("author:${it.trim()}")  // Currently NO quotes
    }

    titleFilter?.takeIf { it.isNotBlank() }?.let {
        queryParts.add("title:${it.trim()}")  // Currently NO quotes
    }

    return queryParts.joinToString(" ").ifBlank { "*" }
}
```

### Current Behavior Examples

| User Input | Filter State | Query Sent | Results |
|------------|--------------|------------|---------|
| `the hobbit` | Both checked | `the hobbit` | 394 results (AND) |
| `the hobbit` | Title only | `title:the hobbit` | 348 results (AND) |
| `tolkien` | Author only | `author:tolkien` | All Tolkien books |

---

## Recommended Improvements

### Option 1: Always Use Quotes for Multi-Word Queries

```kotlin
private fun buildQuery(...): String {
    val queryParts = mutableListOf<String>()

    if (baseQuery.isNotBlank()) {
        val trimmed = baseQuery.trim()
        val formatted = if (trimmed.contains(" ")) "\"${trimmed}\"" else trimmed
        queryParts.add(formatted)
    }

    // Same for authorFilter and titleFilter
}
```

**Results**:
- `the hobbit` → `"the hobbit"` → 284 results ✅
- `tolkien` → `tolkien` → All Tolkien books ✅

### Option 2: User Control (Advanced Search)

Add a toggle for "Exact phrase" vs "All words":
- ☑ Exact phrase → Use quotes
- ☐ All words → No quotes

---

## Performance Notes

- **Limit results**: Default is 100, but 15-20 is sufficient for mobile UI
- **Select fields**: Use `fields=` to reduce response size
- **Caching**: OpenLibrary responses are cacheable
- **Rate limiting**: Be respectful with API calls

### Our Current Settings
```kotlin
resultLimit = 15  // Good for mobile ✅
fields = Not specified (gets all fields) // Could optimize
language = System language // Good for localization ✅
```

---

## API Response Format

```json
{
  "numFound": 284,
  "start": 0,
  "numFoundExact": true,
  "docs": [
    {
      "key": "/works/OL262758W",
      "title": "The Hobbit",
      "author_name": ["J.R.R. Tolkien"],
      "first_publish_year": 1937,
      "isbn": ["0345339681", "9780345339683"],
      "cover_i": 8814811,
      "cover_edition_key": "OL9701406M",
      "subject": ["Fantasy", "Adventure"],
      "language": ["eng"],
      "publisher": ["Houghton Mifflin"],
      "publish_year": [1937, 1966, 1973, ...],
      "number_of_pages_median": 300
    }
  ]
}
```

---

## Testing Commands

```bash
# Test 1: AND behavior (current)
curl "https://openlibrary.org/search.json?q=the+hobbit&limit=5"

# Test 2: Exact phrase (recommended)
curl "https://openlibrary.org/search.json?q=\"the+hobbit\"&limit=5"

# Test 3: Title-specific AND
curl "https://openlibrary.org/search.json?q=title:the+hobbit&limit=5"

# Test 4: Title-specific exact phrase
curl "https://openlibrary.org/search.json?q=title:\"the+hobbit\"&limit=5"

# Test 5: Complex query
curl "https://openlibrary.org/search.json?q=author:tolkien+AND+subject:fantasy&limit=5"
```

---

## References

- Official API Docs: https://openlibrary.org/dev/docs/api/search
- Search Guide: https://openlibrary.org/search/howto
- Apache Solr Query Syntax: https://solr.apache.org/guide/solr/latest/query-guide/standard-query-parser.html
- Cover Images: https://openlibrary.org/dev/docs/api/covers

---

## Key Takeaways for MyBookshelf

1. ✅ **Quotes reduce noise**: `"the hobbit"` gives 284 results vs 394 without quotes
2. ✅ **Field searches work**: `title:`, `author:` are supported
3. ✅ **Quotes work with fields**: `title:"the hobbit"` gives most precise results (274)
4. 🎯 **Recommendation**: Add quotes to multi-word queries for better UX
5. 🎯 **Easy win**: Single-line code change in `buildQuery()`

### Proposed Change

```kotlin
// Before: "the hobbit" → 394 results
queryParts.add(baseQuery.trim())

// After: "the hobbit" → 284 results (28% fewer, more relevant)
val formatted = if (baseQuery.contains(" ")) "\"${baseQuery.trim()}\"" else baseQuery.trim()
queryParts.add(formatted)
```