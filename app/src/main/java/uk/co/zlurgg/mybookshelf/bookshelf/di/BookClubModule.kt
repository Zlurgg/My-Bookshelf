package uk.co.zlurgg.mybookshelf.bookshelf.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookClubRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.BookClubCodeGeneratorImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookClubCodeGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.AddBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.AddBookClubCommentUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.BookClubUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.CreateBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.CreateBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.DeleteBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.DeleteBookClubCommentUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.DeleteBookClubReviewUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.DeleteBookClubReviewUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.EditBookClubCommentUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.EditBookClubCommentUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GenerateInviteLinkUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GenerateInviteLinkUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubCommentsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubCommentsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubPreviewUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubPreviewUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubReviewsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.GetBookClubReviewsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.JoinBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.JoinBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.LeaveBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.LeaveBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.ParseClubCodeUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.ParseClubCodeUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.RestoreBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.RestoreBookClubMembershipsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.SyncBookClubUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.SyncBookClubUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.UpsertBookClubReviewUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.UpsertBookClubReviewUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.ValidateBookClubMembershipsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub.ValidateBookClubMembershipsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookclub.handlers.BookClubOperationsHandler
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

val bookClubModule =
    module {
        // Services
        single<BookClubCodeGenerator> { BookClubCodeGeneratorImpl(get()) }

        // Repository
        single<BookClubRepository> {
            BookClubRepositoryImpl(
                bookClubDao = get(),
                bookshelfDao = get(),
                remoteDataSource = get(),
                codeGenerator = get(),
                authService = get(),
                idGenerator = get(),
                timeProvider = get(),
            )
        }

        // UseCases
        singleOf(::CreateBookClubUseCaseImpl).bind<CreateBookClubUseCase>()
        single<GenerateInviteLinkUseCase> { GenerateInviteLinkUseCaseImpl(ApiConfig.shareBaseUrl) }
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

        // UseCase Facade
        single {
            BookClubUseCases(
                createBookClub = get(),
                generateInviteLink = get(),
                parseClubCode = get(),
                getBookClubPreview = get(),
                joinBookClub = get(),
                syncBookClub = get(),
                restoreBookClubMemberships = get(),
                leaveBookClub = get(),
                validateMemberships = get(),
                getBookClubReviews = get(),
                upsertBookClubReview = get(),
                deleteBookClubReview = get(),
                getBookClubComments = get(),
                addBookClubComment = get(),
                editBookClubComment = get(),
                deleteBookClubComment = get(),
            )
        }

        // Handler
        single { BookClubOperationsHandler(get()) }
    }
