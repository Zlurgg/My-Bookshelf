package uk.co.zlurgg.mybookshelf.welcome.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.GetOrCreateTutorialBookUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.GetOrCreateTutorialBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.GetOrCreateTutorialShelfUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.GetOrCreateTutorialShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.HandleTutorialAccessUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.HandleTutorialAccessUseCaseImpl
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.InitializeWelcomeUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.InitializeWelcomeUseCaseImpl
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.MarkWelcomeShownUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.MarkWelcomeShownUseCaseImpl
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.ShouldShowWelcomeUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.ShouldShowWelcomeUseCaseImpl
import uk.co.zlurgg.mybookshelf.welcome.presentation.WelcomeViewModel

val welcomeModule = module {
    // Tutorial UseCases
    singleOf(::GetOrCreateTutorialBookUseCaseImpl).bind<GetOrCreateTutorialBookUseCase>()
    singleOf(::GetOrCreateTutorialShelfUseCaseImpl).bind<GetOrCreateTutorialShelfUseCase>()
    singleOf(::HandleTutorialAccessUseCaseImpl).bind<HandleTutorialAccessUseCase>()

    // Welcome UseCases
    singleOf(::InitializeWelcomeUseCaseImpl).bind<InitializeWelcomeUseCase>()
    singleOf(::ShouldShowWelcomeUseCaseImpl).bind<ShouldShowWelcomeUseCase>()
    singleOf(::MarkWelcomeShownUseCaseImpl).bind<MarkWelcomeShownUseCase>()

    // ViewModel
    viewModelOf(::WelcomeViewModel)
}
