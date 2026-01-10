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
import uk.co.zlurgg.mybookshelf.auth.data.service.DevAuthService
import uk.co.zlurgg.mybookshelf.auth.data.service.GoogleAuthUiClient
import uk.co.zlurgg.mybookshelf.auth.data.usecase.DevSignInUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthStateRepository
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.CheckSignInStatusUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.DevSignInUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetCurrentUserIdUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.GetCurrentUserIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCase
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignInUseCases
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.SignOutUseCase
import uk.co.zlurgg.mybookshelf.auth.presentation.SignInViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ClearUserDataUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ClearUserDataUseCaseImpl

val authModule = module {
    // Config
    single {
        AuthConfig(
            webClientId = get<Context>().getString(R.string.web_client_id)
        )
    }

    // Services
    single<AuthService> {
        GoogleAuthUiClient(
            context = get(),
            authConfig = get()
        )
    }
    single<AuthStateRepository> { AuthStateRepositoryImpl(get()) }
    single<CurrentUserProvider> { CurrentUserProviderImpl(get()) }

    // Dev sign-in (debug builds only - uses Firebase Auth Emulator)
    if (BuildConfig.DEBUG) {
        single { DevAuthService() }
        single<DevSignInUseCase> { DevSignInUseCaseImpl(get(), get()) }
    }

    // UseCases
    single { SignInUseCase(get(), get(), get()) }
    singleOf(::ClearUserDataUseCaseImpl).bind<ClearUserDataUseCase>()
    single { SignOutUseCase(get(), get(), get(), get(), get(), get()) }
    single { CheckSignInStatusUseCase(get(), get()) }
    single<GetCurrentUserIdUseCase> { GetCurrentUserIdUseCaseImpl(get()) }
    single { SignInUseCases(get(), get(), get()) }

    // ViewModel
    viewModel {
        SignInViewModel(
            signInUseCases = get(),
            shouldShowWelcome = get(),
            hasGuestDataUseCase = get(),
            migrateLocalDataUseCase = get(),
            syncUserPreferencesUseCase = get(),
            restoreBookClubMembershipsUseCase = get(),
            devSignInUseCase = if (BuildConfig.DEBUG) getOrNull() else null
        )
    }
}
