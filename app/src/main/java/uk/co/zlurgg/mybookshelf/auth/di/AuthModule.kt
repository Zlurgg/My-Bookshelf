package uk.co.zlurgg.mybookshelf.auth.di

import android.content.Context
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.auth.data.config.AuthConfig
import uk.co.zlurgg.mybookshelf.auth.data.repository.AuthStateRepositoryImpl
import uk.co.zlurgg.mybookshelf.auth.data.service.CurrentUserProviderImpl
import uk.co.zlurgg.mybookshelf.auth.data.service.GoogleAuthUiClient
import uk.co.zlurgg.mybookshelf.auth.data.service.GoogleCredentialFetcher
import uk.co.zlurgg.mybookshelf.auth.presentation.service.CredentialFetcher
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.AuthUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetCurrentUserIdUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetCurrentUserIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetSignedInUserUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetSignedInUserUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.ResumeSessionUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.ResumeSessionUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.presentation.SignInViewModel

val authModule = module {
    // Config
    single {
        AuthConfig(
            webClientId = get<Context>().getString(R.string.web_client_id)
        )
    }

    // Services
    single<CredentialFetcher> { GoogleCredentialFetcher(authConfig = get()) }
    single<AuthService> {
        GoogleAuthUiClient(
            context = get()
        )
    }
    single<AuthStateRepository> { AuthStateRepositoryImpl(get()) }
    single<CurrentUserProvider> { CurrentUserProviderImpl(get()) }

    // UseCases
    // Note: DevSignInUseCase is provided by DebugModule in debug builds
    single<SignInUseCase> { SignInUseCaseImpl(get(), get()) }
    singleOf(::ResumeSessionUseCaseImpl).bind<ResumeSessionUseCase>()
    singleOf(::SignOutUseCaseImpl).bind<SignOutUseCase>()
    singleOf(::CheckSignInStatusUseCaseImpl).bind<CheckSignInStatusUseCase>()
    single<GetCurrentUserIdUseCase> { GetCurrentUserIdUseCaseImpl(get()) }
    single<GetSignedInUserUseCase> { GetSignedInUserUseCaseImpl(get()) }
    single { AuthUseCases(get(), get(), get(), get(), get()) }

    // ViewModels
    viewModel {
        SignInViewModel(
            authUseCases = get(),
            shouldShowWelcome = get(),
            resumeSession = get(),
            devSignInUseCase = if (BuildConfig.DEBUG) getOrNull() else null
        )
    }
}
