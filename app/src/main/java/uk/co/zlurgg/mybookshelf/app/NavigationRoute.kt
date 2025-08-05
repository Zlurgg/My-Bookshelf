package uk.co.zlurgg.mybookshelf.app

import kotlinx.serialization.Serializable

sealed interface NavigationRoute {
    @Serializable
    data object Bookshelf: NavigationRoute
}

