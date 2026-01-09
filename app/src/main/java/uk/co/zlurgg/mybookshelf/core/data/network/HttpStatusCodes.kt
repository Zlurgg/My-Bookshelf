package uk.co.zlurgg.mybookshelf.core.data.network

/**
 * HTTP status code constants for consistent handling across the app.
 * Centralizes all status codes to avoid magic numbers and enable reuse.
 *
 * @see ErrorMapper.mapHttpStatusToDataError
 */
object HttpStatusCodes {
    // Client Error (4xx)
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val REQUEST_TIMEOUT = 408
    const val UNPROCESSABLE_ENTITY = 422
    const val TOO_MANY_REQUESTS = 429

    // Server Error (5xx)
    const val INTERNAL_SERVER_ERROR = 500
    const val BAD_GATEWAY = 502
    const val SERVICE_UNAVAILABLE = 503
    const val GATEWAY_TIMEOUT = 504

    // Ranges for categorization
    val SUCCESS_RANGE = 200..299
    val SERVER_ERROR_RANGE = 500..599
}
