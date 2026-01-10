package uk.co.zlurgg.mybookshelf.di

import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.auth.data.service.DevAuthService
import uk.co.zlurgg.mybookshelf.auth.data.usecase.DevSignInUseCaseImpl
import uk.co.zlurgg.mybookshelf.auth.domain.usecase.DevSignInUseCase
import uk.co.zlurgg.mybookshelf.core.data.firebase.FirebaseEmulatorConfig

/**
 * Debug-only Koin module providing development/testing dependencies.
 * This module only exists in debug builds.
 */
val debugModule = module {
    // Firebase Emulator configuration
    single { FirebaseEmulatorConfig }

    // Dev authentication for emulator testing
    single { DevAuthService() }
    single<DevSignInUseCase> { DevSignInUseCaseImpl(get(), get()) }
}
