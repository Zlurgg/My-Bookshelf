package uk.co.zlurgg.mybookshelf.bookshelf.presentation.welcome

sealed interface WelcomeAction {
    data object OnGetStartedClick : WelcomeAction
}
