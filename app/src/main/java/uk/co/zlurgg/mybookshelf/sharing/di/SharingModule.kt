package uk.co.zlurgg.mybookshelf.sharing.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.sharing.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.sharing.data.service.AndroidBookshelfExportService
import uk.co.zlurgg.mybookshelf.sharing.data.service.AndroidShareService
import uk.co.zlurgg.mybookshelf.sharing.data.service.BookshelfImportValidatorImpl
import uk.co.zlurgg.mybookshelf.sharing.data.service.DatabaseBookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.sharing.data.service.JsonBookshelfSerializer
import uk.co.zlurgg.mybookshelf.sharing.data.service.UrlEncodedShareTokenService
import uk.co.zlurgg.mybookshelf.sharing.domain.service.BookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.sharing.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.sharing.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.sharing.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.sharing.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.CheckImportConflictUseCase
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.CheckImportConflictUseCaseImpl
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.DeepLinkImportUseCase
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.DeepLinkImportUseCaseImpl
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.ExportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.ExportBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.ImportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.ImportBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.sharing.presentation.DeepLinkViewModel

val sharingModule = module {
    // Services
    single<ShareTokenService> { UrlEncodedShareTokenService() }
    singleOf(::AndroidBookshelfExportService).bind<BookshelfExportService>()
    singleOf(::AndroidShareService)
    singleOf(::JsonBookshelfSerializer).bind<BookshelfSerializer>()
    singleOf(::BookshelfImportValidatorImpl).bind<BookshelfImportValidator>()
    singleOf(::DatabaseBookshelfDataOrchestrator).bind<BookshelfDataOrchestrator>()
    singleOf(::BookshelfExportMapper)

    // UseCases
    singleOf(::ExportBookshelfUseCaseImpl).bind<ExportBookshelfUseCase>()
    singleOf(::ImportBookshelfUseCaseImpl).bind<ImportBookshelfUseCase>()
    singleOf(::CheckImportConflictUseCaseImpl).bind<CheckImportConflictUseCase>()
    singleOf(::DeepLinkImportUseCaseImpl).bind<DeepLinkImportUseCase>()

    // ViewModel
    viewModelOf(::DeepLinkViewModel)
}
