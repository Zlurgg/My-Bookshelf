package uk.co.zlurgg.mybookshelf.update.di

import android.content.Context
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.update.data.remote.api.GitHubApiService
import uk.co.zlurgg.mybookshelf.update.data.repository.UpdateRepositoryImpl
import uk.co.zlurgg.mybookshelf.update.data.service.ApkDownloadService
import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateConfig
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdateRepository
import uk.co.zlurgg.mybookshelf.update.domain.usecases.CheckForUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.CheckForUpdateUseCaseImpl
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DismissUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DismissUpdateUseCaseImpl
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DownloadUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DownloadUpdateUseCaseImpl
import uk.co.zlurgg.mybookshelf.update.domain.usecases.GetCurrentVersionInfoUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.GetCurrentVersionInfoUseCaseImpl
import uk.co.zlurgg.mybookshelf.update.domain.usecases.UpdateUseCases

private const val GITHUB_OWNER = "Zlurgg"
private const val GITHUB_REPO = "My-Bookshelf"
private const val APP_NAME = "my-bookshelf"

val updateModule = module {
    // Config
    single {
        UpdateConfig(
            gitHubOwner = GITHUB_OWNER,
            gitHubRepo = GITHUB_REPO,
            appName = APP_NAME
        )
    }

    // Services
    single { GitHubApiService(httpClient = get()) }
    single { ApkDownloadService(context = get<Context>(), downloadTitle = get<UpdateConfig>().downloadTitle) }

    // Repository
    singleOf(::UpdateRepositoryImpl).bind<UpdateRepository>()

    // UseCases
    single<CheckForUpdateUseCase> { CheckForUpdateUseCaseImpl(get(), get(), BuildConfig.VERSION_NAME) }
    single<DismissUpdateUseCase> { DismissUpdateUseCaseImpl(get()) }
    single<DownloadUpdateUseCase> { DownloadUpdateUseCaseImpl(get(), get()) }
    single<GetCurrentVersionInfoUseCase> { GetCurrentVersionInfoUseCaseImpl(get(), BuildConfig.VERSION_NAME) }
    single { UpdateUseCases(get(), get(), get(), get()) }
}
