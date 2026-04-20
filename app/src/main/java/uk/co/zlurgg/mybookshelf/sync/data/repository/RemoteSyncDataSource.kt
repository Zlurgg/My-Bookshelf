package uk.co.zlurgg.mybookshelf.sync.data.repository

/**
 * Composite interface for all remote sync data operations.
 * Extends focused interfaces for better separation of concerns while maintaining
 * a single injection point for consumers.
 *
 * Individual interfaces:
 * - [BookSyncDataSource]: Book upload/download/delete (5 functions)
 * - [ShelfSyncDataSource]: Shelf upload/download/delete/share (10 functions)
 * - [UserPreferencesDataSource]: User preferences (2 functions)
 * - [BookClubRemoteDataSource]: Book club operations (22 functions)
 */
interface RemoteSyncDataSource :
    BookSyncDataSource,
    ShelfSyncDataSource,
    UserPreferencesDataSource,
    BookClubRemoteDataSource
