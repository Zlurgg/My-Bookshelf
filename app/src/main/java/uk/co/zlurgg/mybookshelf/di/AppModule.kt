package uk.co.zlurgg.mybookshelf.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.bookcase.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.bookshelf.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookcase.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfViewModel

val appModule = module {
    single<BookcaseRepository> { BookcaseRepositoryImpl() }
    single<BookshelfRepository> { BookshelfRepositoryImpl() }

    viewModel { (shelfId: String) ->
        BookshelfViewModel(
            repository = get(),
            shelfId = shelfId
        )
    }
    viewModelOf(::BookcaseViewModel)
    viewModelOf(::BookDetailViewModel)
}
