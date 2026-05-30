package uk.co.zlurgg.mybookshelf.library.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.library.domain.usecase.DeleteBooksFromLibraryUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.DeleteBooksFromLibraryUseCaseImpl
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetAllLibraryBooksUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetAllLibraryBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetNonRemovableBookIdsUseCase
import uk.co.zlurgg.mybookshelf.library.domain.usecase.GetNonRemovableBookIdsUseCaseImpl
import uk.co.zlurgg.mybookshelf.library.domain.usecase.LibraryUseCases
import uk.co.zlurgg.mybookshelf.library.presentation.LibraryViewModel

val libraryModule = module {
    singleOf(::GetAllLibraryBooksUseCaseImpl).bind<GetAllLibraryBooksUseCase>()
    singleOf(::DeleteBooksFromLibraryUseCaseImpl).bind<DeleteBooksFromLibraryUseCase>()
    singleOf(::GetNonRemovableBookIdsUseCaseImpl).bind<GetNonRemovableBookIdsUseCase>()
    single {
        LibraryUseCases(
            getAllLibraryBooks = get(),
            searchBooks = get(),
            cacheSearchPreviews = get(),
            upsertBook = get(),
            deleteBooks = get(),
            getNonRemovableBookIds = get()
        )
    }
    viewModelOf(::LibraryViewModel)
}
