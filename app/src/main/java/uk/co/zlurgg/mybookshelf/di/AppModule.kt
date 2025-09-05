package uk.co.zlurgg.mybookshelf.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDatabase
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.DatabaseFactory
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.KtorRemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.BookshelfViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.shared.SharedMyBookshelfViewModel
import uk.co.zlurgg.mybookshelf.core.data.HttpClientFactory

val appModule = module {
    single<HttpClientEngine> {
        Android.create()
    }
    single { HttpClientFactory.create(get()) }

    singleOf(::KtorRemoteBookDataSource).bind<RemoteBookDataSource>()

    single<DatabaseFactory> { DatabaseFactory(get()) }

    single {
        get<DatabaseFactory>().create()
            .build()
    }
    single { get<BookshelfDatabase>().bookshelfDao }

    // Shared shelves VM
    viewModelOf(::SharedMyBookshelfViewModel)

    viewModel { (shelfId: String) ->
        BookshelfViewModel(
            bookshelfRepository = get(),
            shelfId = shelfId
        )
    }
    viewModelOf(::BookcaseViewModel)
    viewModel { (bookId: String, shelfId: String?) ->
        BookDetailViewModel(
            bookRepository = get(),
            bookId = bookId,
            shelfId = shelfId
        )
    }

    singleOf(::BookshelfRepositoryImpl).bind<BookshelfRepository>()
    singleOf(::BookcaseRepositoryImpl).bind<BookcaseRepository>()
    singleOf(::BookRepositoryImpl).bind<BookRepository>()
}
