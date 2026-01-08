package uk.co.zlurgg.mybookshelf.sync.domain.model

import androidx.compose.runtime.Immutable

/**
 * Information about orphan (guest) data in the local database.
 */
@Immutable
data class GuestDataInfo(
    val bookCount: Int,
    val shelfCount: Int,
) {
    /**
     * Returns true if there is any guest data to potentially import.
     */
    val hasData: Boolean
        get() = bookCount > 0 || shelfCount > 0

    /**
     * Total count of orphan entities.
     */
    val totalCount: Int
        get() = bookCount + shelfCount
}
