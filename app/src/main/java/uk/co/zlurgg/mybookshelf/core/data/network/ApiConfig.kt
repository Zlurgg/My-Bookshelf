package uk.co.zlurgg.mybookshelf.core.data.network

import uk.co.zlurgg.mybookshelf.BuildConfig

object ApiConfig {

    private val openLibraryBaseUrl: String = BuildConfig.OPEN_LIBRARY_BASE_URL
    val shareBaseUrl: String = BuildConfig.SHARE_BASE_URL
    val httpTimeoutMillis: Long = BuildConfig.HTTP_TIMEOUT_MILLIS

    object OpenLibrary {
        val baseUrl: String = openLibraryBaseUrl
        val searchEndpoint: String = "$baseUrl/search.json"
        fun workDetailsEndpoint(workId: String): String = "$baseUrl/works/$workId.json"

        object DefaultParams {
            const val SEARCH_FIELDS = "key,title,author_name,author_key,cover_edition_key,cover_i,ratings_average,ratings_count,first_publish_year,language,number_of_pages_median,edition_count"
        }
    }

    object Http {
        val socketTimeout: Long = httpTimeoutMillis
        val requestTimeout: Long = httpTimeoutMillis
        val connectTimeout: Long = httpTimeoutMillis
        const val USER_AGENT = "MyBookshelf/1.0 (Android App; github.com/zlurgg/mybookshelf)"
    }
}