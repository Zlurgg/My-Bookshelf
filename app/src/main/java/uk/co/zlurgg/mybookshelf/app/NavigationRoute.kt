package uk.co.zlurgg.mybookshelf.app

import kotlinx.serialization.Serializable

sealed interface NavigationRoute {
    @Serializable
    data object MyBookshelfGraph : NavigationRoute {
        const val ROUTE = "my_bookshelf_graph"
    }

    @Serializable
    data object SignIn : NavigationRoute {
        const val ROUTE = "sign_in"
        fun createRoute() = ROUTE
    }

    @Serializable
    data object Welcome : NavigationRoute {
        const val ROUTE = "welcome"
        fun createRoute() = ROUTE
    }

    @Serializable
    data object Bookcase : NavigationRoute {
        const val ROUTE = "bookcase"
        const val ARG_NEW_SHELF = "new_shelf"
        const val ARG_SWITCH_TO_BOOK_CLUBS = "switch_to_book_clubs"
        const val ARG_CREATE_CLUB_FOR_SHELF = "create_club_for_shelf"
        fun createRoute(isNew: Boolean = false, switchToBookClubs: Boolean = false) =
            "bookcase?$ARG_NEW_SHELF=$isNew&$ARG_SWITCH_TO_BOOK_CLUBS=$switchToBookClubs"
    }

    @Serializable
    data class Bookshelf(val id: String) : NavigationRoute {
        companion object {
            const val ROUTE = "bookshelf/{id}"
            const val KEY_ID = "id"
            fun createRoute(id: String) = "bookshelf/$id"
        }
    }

    @Serializable
    data object Account : NavigationRoute {
        const val ROUTE = "account"
        fun createRoute() = ROUTE
    }

    @Serializable
    data object BookClubs : NavigationRoute {
        const val ROUTE = "bookclubs"
        fun createRoute() = ROUTE
    }

    @Serializable
    data object Library : NavigationRoute {
        const val ROUTE = "library"
        fun createRoute() = ROUTE
    }

    @Serializable
    data class BookDetail(val id: String, val shelfId: String? = null) : NavigationRoute {
        companion object {
            const val ROUTE = "bookdetail/{id}?shelfId={shelfId}"
            const val KEY_ID = "id"
            const val KEY_SHELF_ID = "shelfId"
            fun createRoute(id: String, shelfId: String? = null) =
                if (shelfId != null) "bookdetail/$id?shelfId=$shelfId" else "bookdetail/$id"
        }
    }
}
