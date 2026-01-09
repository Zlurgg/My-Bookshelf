package uk.co.zlurgg.mybookshelf.sync.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.sync.data.engine.SyncEngine
import uk.co.zlurgg.mybookshelf.sync.data.repository.RemoteSyncDataSource
import uk.co.zlurgg.mybookshelf.sync.data.repository.SyncRepositoryImpl
import uk.co.zlurgg.mybookshelf.sync.data.repository.UserPreferencesRepositoryImpl
import uk.co.zlurgg.mybookshelf.sync.data.service.AndroidConnectivityMonitor
import uk.co.zlurgg.mybookshelf.sync.data.service.DefaultConflictResolver
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreRemoteDataSource
import uk.co.zlurgg.mybookshelf.sync.data.worker.SyncScheduler
import uk.co.zlurgg.mybookshelf.sync.domain.repository.SyncRepository
import uk.co.zlurgg.mybookshelf.sync.domain.repository.UserPreferencesRepository
import uk.co.zlurgg.mybookshelf.sync.domain.service.ConflictResolver
import uk.co.zlurgg.mybookshelf.sync.domain.service.ConnectivityMonitor
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.HasGuestDataUseCase
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.HasGuestDataUseCaseImpl
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.MigrateLocalDataUseCase
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.MigrateLocalDataUseCaseImpl
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.SyncUserPreferencesUseCase
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.SyncUserPreferencesUseCaseImpl

val syncModule = module {
    // Services
    single<ConnectivityMonitor> { AndroidConnectivityMonitor(get<Context>()) }
    single<ConflictResolver> { DefaultConflictResolver.lastWriteWins() }
    single { FirebaseFirestore.getInstance() }
    single<RemoteSyncDataSource> { FirestoreRemoteDataSource(get()) }
    single<SyncSchedulerService> { SyncScheduler(get()) }

    // Engine
    single {
        SyncEngine(
            bookshelfDao = get(),
            syncDao = get(),
            remoteDataSource = get(),
            conflictResolver = get(),
            connectivityMonitor = get(),
            timeProvider = get()
        )
    }

    // Repositories
    single<SyncRepository> {
        SyncRepositoryImpl(
            syncEngine = get(),
            syncDao = get(),
            bookshelfDao = get(),
            connectivityMonitor = get(),
            timeProvider = get()
        )
    }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get(), get(), get()) }

    // UseCases
    single<MigrateLocalDataUseCase> { MigrateLocalDataUseCaseImpl(get(), get(), get()) }
    single<HasGuestDataUseCase> { HasGuestDataUseCaseImpl(get()) }
    single<SyncUserPreferencesUseCase> { SyncUserPreferencesUseCaseImpl(get(), get()) }
}
