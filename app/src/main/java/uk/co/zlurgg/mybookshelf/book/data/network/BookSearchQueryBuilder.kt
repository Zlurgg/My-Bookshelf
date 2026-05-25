package uk.co.zlurgg.mybookshelf.book.data.network

/**
 * Shared query-string assembler for the Google Books and OpenLibrary
 * search APIs.
 *
 * Both providers accept a Lucene-flavoured `q` parameter of space-joined
 * `prefix:value` fragments — only the prefix names differ (OL uses
 * `author`/`title`/`subject`, Google uses `inauthor`/`intitle`/`subject`).
 * The construction rules — trim, strip embedded double quotes, wrap
 * multi-word values in quotes, drop blank fields — are identical, so
 * they live here behind a [prefixes] map parameterised per provider.
 *
 * @property prefixes lookup from a [FilterField] to the wire-level prefix
 *           token used by the target API. Missing entries cause that
 *           field to be omitted from the rendered query.
 */
internal class BookSearchQueryBuilder(
    private val prefixes: Map<FilterField, String>,
) {

    /**
     * Logical fields the builder understands. Each provider maps these
     * to its own wire-level prefix via the [prefixes] table.
     */
    enum class FilterField { AUTHOR, TITLE, SUBJECT }

    /**
     * Render a query string from the optional [baseQuery] plus any
     * supplied filter fields. Blank inputs are silently skipped; the
     * result is a single space-joined string suitable for use as the
     * `q` URL parameter (it may legitimately be empty).
     */
    fun build(
        baseQuery: String,
        authorFilter: String? = null,
        titleFilter: String? = null,
        subjectFilter: String? = null,
    ): String {
        val parts = mutableListOf<String>()

        if (baseQuery.isNotBlank()) {
            parts.add(sanitize(baseQuery))
        }

        appendIfPresent(parts, authorFilter, FilterField.AUTHOR)
        appendIfPresent(parts, titleFilter, FilterField.TITLE)
        appendIfPresent(parts, subjectFilter, FilterField.SUBJECT)

        return parts.joinToString(" ")
    }

    private fun appendIfPresent(
        parts: MutableList<String>,
        raw: String?,
        field: FilterField,
    ) {
        val prefix = prefixes[field] ?: return
        raw?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatField(it, prefix))
        }
    }

    private fun formatField(raw: String, prefix: String): String {
        val sanitized = sanitize(raw)
        val quoted = if (sanitized.contains(" ")) "\"$sanitized\"" else sanitized
        return "$prefix:$quoted"
    }

    private fun sanitize(input: String): String = input.trim().replace("\"", "")
}
