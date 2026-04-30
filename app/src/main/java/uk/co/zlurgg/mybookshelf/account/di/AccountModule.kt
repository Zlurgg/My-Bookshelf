package uk.co.zlurgg.mybookshelf.account.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.account.domain.usecase.DeleteAccountUseCase
import uk.co.zlurgg.mybookshelf.account.domain.usecase.DeleteAccountUseCaseImpl
import uk.co.zlurgg.mybookshelf.account.presentation.AccountViewModel

val accountModule = module {
    singleOf(::DeleteAccountUseCaseImpl).bind<DeleteAccountUseCase>()
    viewModel { AccountViewModel(get(), get()) }
}
