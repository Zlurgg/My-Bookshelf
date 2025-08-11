package uk.co.zlurgg.mybookshelf.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_search.BookSearchViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfViewModel

val appModule = module {
    viewModel { BookshelfViewModel() }
    viewModel { BookcaseViewModel() }
    viewModel { BookshelfViewModel() }
    viewModel { BookSearchViewModel() }
}
