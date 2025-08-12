package uk.co.zlurgg.mybookshelf.app

import kotlinx.serialization.Serializable

sealed interface NavigationRoute {
    @Serializable
    data object MyBookshelfGraph: NavigationRoute
    @Serializable
    data object Bookcase: NavigationRoute
    @Serializable
    data object BookSearch: NavigationRoute
    @Serializable
    data class Bookshelf(val id: String): NavigationRoute
    @Serializable
    data class BookDetail(val id: String): NavigationRoute
}

