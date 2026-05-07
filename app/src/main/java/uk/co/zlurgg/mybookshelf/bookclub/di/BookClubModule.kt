package uk.co.zlurgg.mybookshelf.bookclub.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.book.domain.service.BookReviewProvider
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.bookclub.data.repository.BookClubManagementRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookclub.data.repository.BookClubMembershipRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookclub.data.repository.BookClubRepositoryHelper
import uk.co.zlurgg.mybookshelf.bookclub.data.repository.BookClubRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookclub.data.repository.BookClubReviewRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookclub.data.repository.BookClubSyncRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubManagementRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubMembershipRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubReviewRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubSyncRepository
import uk.co.zlurgg.mybookshelf.bookclub.data.service.BookClubCodeGeneratorImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.service.BookClubCodeGenerator
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.AddBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.AddBookClubCommentUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.BookClubOperationUseCases
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.BookClubReviewUseCases
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.ClearClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.ClearClubMembershipsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.CreateBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.CreateBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.DeleteBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.DeleteBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetClubMembershipsForUserUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetClubMembershipsForUserUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetClubsCreatedByUserUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetClubsCreatedByUserUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.DeleteBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.DeleteBookClubCommentUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.DeleteBookClubReviewUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.DeleteBookClubReviewUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.EditBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.EditBookClubCommentUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetBookClubCommentsUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetBookClubCommentsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetBookClubPreviewUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetBookClubPreviewUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetBookClubReviewsUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.GetBookClubReviewsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.JoinBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.JoinBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.LeaveBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.LeaveBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.ParseClubCodeUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.ParseClubCodeUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RemoveBookFromClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RemoveBookFromClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RemoveUserFromClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RemoveUserFromClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RenameBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RenameBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.RestoreBookClubMembershipsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.SyncBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.SyncBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.SyncBookToClubUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.SyncBookToClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.UpdateClubStyleUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.UpdateClubStyleUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.UpsertBookClubReviewUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.UpsertBookClubReviewUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.ValidateBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.ValidateBookClubMembershipsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookclub.presentation.handlers.BookReviewProviderImpl
import uk.co.zlurgg.mybookshelf.bookclub.presentation.handlers.ClubOperationsImpl
import uk.co.zlurgg.mybookshelf.sync.data.repository.BookClubRemoteDataSource
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreBookClubRemoteDataSourceImpl
import com.google.firebase.firestore.FirebaseFirestore

val bookClubModule = module {
    // Firestore
    single { FirebaseFirestore.getInstance() }
    single<BookClubRemoteDataSource> { FirestoreBookClubRemoteDataSourceImpl(get()) }

    // Services
    single<BookClubCodeGenerator> { BookClubCodeGeneratorImpl(get()) }

    // Repository Helper
    single {
        BookClubRepositoryHelper(
            bookClubDao = get(),
            bookshelfDao = get(),
            remoteDataSource = get(),
            authService = get(),
            timeProvider = get()
        )
    }

    // Focused Repositories
    single<BookClubManagementRepository> {
        BookClubManagementRepositoryImpl(
            bookClubDao = get(),
            bookshelfDao = get(),
            remoteDataSource = get(),
            codeGenerator = get(),
            authService = get(),
            idGenerator = get(),
            timeProvider = get(),
            helper = get()
        )
    }
    single<BookClubMembershipRepository> {
        BookClubMembershipRepositoryImpl(
            bookClubDao = get(),
            bookshelfDao = get(),
            remoteDataSource = get(),
            authService = get(),
            idGenerator = get(),
            timeProvider = get(),
            helper = get()
        )
    }
    single<BookClubSyncRepository> {
        BookClubSyncRepositoryImpl(
            bookClubDao = get(),
            bookshelfDao = get(),
            remoteDataSource = get(),
            authService = get(),
            timeProvider = get(),
            helper = get()
        )
    }
    single<BookClubReviewRepository> {
        BookClubReviewRepositoryImpl(
            remoteDataSource = get(),
            authService = get(),
            timeProvider = get()
        )
    }

    // Composite Repository
    single<BookClubRepository> {
        BookClubRepositoryImpl(
            management = get(),
            membership = get(),
            sync = get(),
            review = get()
        )
    }

    // UseCases
    singleOf(::CreateBookClubUseCaseImpl).bind<CreateBookClubUseCase>()
    single<ParseClubCodeUseCase> { ParseClubCodeUseCaseImpl() }
    singleOf(::GetBookClubPreviewUseCaseImpl).bind<GetBookClubPreviewUseCase>()
    singleOf(::JoinBookClubUseCaseImpl).bind<JoinBookClubUseCase>()
    singleOf(::SyncBookClubUseCaseImpl).bind<SyncBookClubUseCase>()
    singleOf(::RestoreBookClubMembershipsUseCaseImpl).bind<RestoreBookClubMembershipsUseCase>()
    singleOf(::LeaveBookClubUseCaseImpl).bind<LeaveBookClubUseCase>()
    singleOf(::ValidateBookClubMembershipsUseCaseImpl).bind<ValidateBookClubMembershipsUseCase>()
    singleOf(::GetBookClubReviewsUseCaseImpl).bind<GetBookClubReviewsUseCase>()
    singleOf(::UpsertBookClubReviewUseCaseImpl).bind<UpsertBookClubReviewUseCase>()
    singleOf(::DeleteBookClubReviewUseCaseImpl).bind<DeleteBookClubReviewUseCase>()
    singleOf(::GetBookClubCommentsUseCaseImpl).bind<GetBookClubCommentsUseCase>()
    singleOf(::AddBookClubCommentUseCaseImpl).bind<AddBookClubCommentUseCase>()
    singleOf(::EditBookClubCommentUseCaseImpl).bind<EditBookClubCommentUseCase>()
    singleOf(::DeleteBookClubCommentUseCaseImpl).bind<DeleteBookClubCommentUseCase>()

    // Club management UseCases (pure delegation)
    singleOf(::DeleteBookClubUseCaseImpl).bind<DeleteBookClubUseCase>()
    singleOf(::SyncBookToClubUseCaseImpl).bind<SyncBookToClubUseCase>()
    singleOf(::RemoveBookFromClubUseCaseImpl).bind<RemoveBookFromClubUseCase>()
    singleOf(::UpdateClubStyleUseCaseImpl).bind<UpdateClubStyleUseCase>()
    singleOf(::ClearClubMembershipsUseCaseImpl).bind<ClearClubMembershipsUseCase>()
    singleOf(::RenameBookClubUseCaseImpl).bind<RenameBookClubUseCase>()
    singleOf(::GetClubsCreatedByUserUseCaseImpl).bind<GetClubsCreatedByUserUseCase>()
    singleOf(::GetClubMembershipsForUserUseCaseImpl).bind<GetClubMembershipsForUserUseCase>()
    singleOf(::RemoveUserFromClubUseCaseImpl).bind<RemoveUserFromClubUseCase>()

    // UseCase Aggregators
    single {
        BookClubOperationUseCases(
            createBookClub = get(),
            parseClubCode = get(),
            getBookClubPreview = get(),
            joinBookClub = get(),
            syncBookClub = get(),
            restoreBookClubMemberships = get(),
            leaveBookClub = get(),
            validateMemberships = get(),
            deleteBookClub = get(),
            syncBookToClub = get(),
            removeBookFromClub = get(),
            updateClubStyle = get(),
            clearClubMemberships = get(),
            renameBookClub = get(),
            getClubsCreatedByUser = get(),
            getClubMembershipsForUser = get(),
            removeUserFromClub = get()
        )
    }

    single {
        BookClubReviewUseCases(
            getBookClubReviews = get(),
            upsertBookClubReview = get(),
            deleteBookClubReview = get(),
            getBookClubComments = get(),
            addBookClubComment = get(),
            editBookClubComment = get(),
            deleteBookClubComment = get()
        )
    }

    // Interface implementations for cross-feature bridges
    single<ClubOperations> { ClubOperationsImpl(get()) }
    single<BookReviewProvider> { BookReviewProviderImpl(get()) }
}
