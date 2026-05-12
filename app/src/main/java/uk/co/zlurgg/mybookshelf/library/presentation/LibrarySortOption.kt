package uk.co.zlurgg.mybookshelf.library.presentation

import androidx.annotation.StringRes
import uk.co.zlurgg.mybookshelf.R

enum class LibrarySortOption(@param:StringRes val labelResId: Int) {
    RECENTLY_ADDED(R.string.sort_recently_added),
    TITLE_AZ(R.string.sort_title_az),
    AUTHOR_AZ(R.string.sort_author_az)
}
