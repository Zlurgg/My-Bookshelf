package uk.co.zlurgg.mybookshelf.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfViewModel

val appModule = module {
    viewModelOf(::BookshelfViewModel)
    viewModelOf(::BookcaseViewModel)
    viewModelOf(::BookDetailViewModel)
}
