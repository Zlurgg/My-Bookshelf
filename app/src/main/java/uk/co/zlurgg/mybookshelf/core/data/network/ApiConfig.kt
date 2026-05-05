package uk.co.zlurgg.mybookshelf.core.data.network

import uk.co.zlurgg.mybookshelf.BuildConfig

object ApiConfig {

    private val openLibraryBaseUrl: String = BuildConfig.OPEN_LIBRARY_BASE_URL
    private val siteBaseUrl: String = BuildConfig.SITE_BASE_URL
    val httpTimeoutMillis: Long = BuildConfig.HTTP_TIMEOUT_MILLIS

    object Site {
        val privacyPolicyUrl: String = "$siteBaseUrl/privacy.html"
        val deleteAccountUrl: String = "$siteBaseUrl/delete-account.html"
    }

    object OpenLibrary {
        val baseUrl: String = openLibraryBaseUrl
        val searchEndpoint: String = "$baseUrl/search.json"
        fun workDetailsEndpoint(workId: String): String = "$baseUrl/works/$workId.json"

        object DefaultParams {
            const val SEARCH_FIELDS =
                "key,title,author_name,author_key,cover_edition_key,cover_i," +
                    "ratings_average,ratings_count,first_publish_year,language," +
                    "number_of_pages_median,edition_count,isbn,publisher,publish_date,ia"
        }

        /**
         * Cover image URL construction for OpenLibrary cover API.
         * Centralizes all cover URL logic for consistency and maintainability.
         */
        object CoverUrls {
            private const val COVER_BASE_URL = "https://covers.openlibrary.org"

            enum class CoverSize(val suffix: String) {
                SMALL("-S.jpg"),
                MEDIUM("-M.jpg"),
                LARGE("-L.jpg")
            }

            /**
             * Builds a cover URL from coverKey and size.
             * Handles both OLID format (e.g., "OL123M") and numeric ID format.
             *
             * @param coverKey The cover identifier (OLID like "OL123M" or numeric like "12345")
             * @param size The desired image size
             * @return Complete cover URL, or empty string if coverKey is null
             */
            fun buildCoverUrl(coverKey: String?, size: CoverSize): String {
                return when {
                    coverKey == null -> ""
                    coverKey.startsWith("OL") -> "$COVER_BASE_URL/b/olid/$coverKey${size.suffix}"
                    else -> "$COVER_BASE_URL/b/id/$coverKey${size.suffix}"
                }
            }
        }
    }

    object Http {
        val socketTimeout: Long = httpTimeoutMillis
        val requestTimeout: Long = httpTimeoutMillis
        val connectTimeout: Long = httpTimeoutMillis
        val USER_AGENT = "MyBookshelf/${BuildConfig.VERSION_NAME} (Android; github.com/zlurgg/mybookshelf)"

        /** Maximum delay between retry attempts (10 seconds) */
        const val MAX_RETRY_DELAY_MS = 10_000L
    }
}
