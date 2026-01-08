package uk.co.zlurgg.mybookshelf.auth.di

import android.content.Context
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.auth.data.config.AuthConfig
import uk.co.zlurgg.mybookshelf.auth.data.repository.AuthStateRepositoryImpl
import uk.co.zlurgg.mybookshelf.auth.data.service.CurrentUserProviderImpl
import uk.co.zlurgg.mybookshelf.auth.data.service.GoogleAuthUiClient
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCase
import uk.co.zlurgg.mybookshelf.auth.presentation.SignInViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.data.usecase.ClearUserDataUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ClearUserDataUseCase

val authModule =
    module {
        // Config
        single {
            AuthConfig(
                webClientId = get<Context>().getString(R.string.web_client_id),
            )
        }

        // Services
        single<AuthService> {
            GoogleAuthUiClient(
                context = get(),
                authConfig = get(),
            )
        }
        single<AuthStateRepository> { AuthStateRepositoryImpl(get()) }
        single<CurrentUserProvider> { CurrentUserProviderImpl(get()) }

        // UseCases
        single { SignInUseCase(get(), get(), get()) }
        singleOf(::ClearUserDataUseCaseImpl).bind<ClearUserDataUseCase>()
        single { SignOutUseCase(get(), get(), get(), get(), get(), get()) }
        single { CheckSignInStatusUseCase(get(), get()) }
        single { SignInUseCases(get(), get(), get()) }

        // ViewModel
        viewModel { SignInViewModel(get(), get(), get(), get(), get(), get()) }
    }
