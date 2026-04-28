package uk.co.zlurgg.mybookshelf.bookcase.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.CreateShelfUseCase
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.CreateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.DeleteShelfUseCase
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.DeleteShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.DuplicateShelfUseCase
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.DuplicateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.GetAllShelvesUseCase
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.GetAllShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.GetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.GetShelfByIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.RenameShelfUseCase
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.RenameShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.ReorderShelvesUseCase
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.ReorderShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.UpdateShelfStyleUseCase
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.UpdateShelfStyleUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookcase.presentation.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers.ShelfManagementHandler
import uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers.ShelfOperationsHandler

val bookcaseModule = module {
    // Bookcase UseCases
    singleOf(::CreateShelfUseCaseImpl).bind<CreateShelfUseCase>()
    singleOf(::DeleteShelfUseCaseImpl).bind<DeleteShelfUseCase>()
    singleOf(::GetAllShelvesUseCaseImpl).bind<GetAllShelvesUseCase>()
    singleOf(::ReorderShelvesUseCaseImpl).bind<ReorderShelvesUseCase>()
    singleOf(::GetShelfByIdUseCaseImpl).bind<GetShelfByIdUseCase>()
    singleOf(::RenameShelfUseCaseImpl).bind<RenameShelfUseCase>()
    singleOf(::UpdateShelfStyleUseCaseImpl).bind<UpdateShelfStyleUseCase>()
    singleOf(::DuplicateShelfUseCaseImpl).bind<DuplicateShelfUseCase>()

    // UseCase Facade
    single {
        BookcaseUseCases(
            getAllShelves = get(),
            createShelf = get(),
            deleteShelf = get(),
            reorderShelves = get(),
            getShelfById = get(),
            renameShelf = get(),
            updateShelfStyle = get(),
            duplicateShelf = get()
        )
    }

    // Presentation Handlers
    single { ShelfOperationsHandler(get()) }
    single { ShelfManagementHandler(get(), get(), get()) }

    // ViewModels
    viewModel {
        BookcaseViewModel(
            shelfOperations = get(),
            shelfManagement = get(),
            bookcaseUseCases = get(),
            bookClubOperations = get(),
            authUseCases = get()
        )
    }
}
