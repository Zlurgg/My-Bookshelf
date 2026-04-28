package uk.co.zlurgg.mybookshelf.welcome.presentation

sealed interface WelcomeAction {
    data object OnGetStartedClick : WelcomeAction
}
