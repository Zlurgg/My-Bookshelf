package uk.co.zlurgg.mybookshelf.sync.data.service

import uk.co.zlurgg.mybookshelf.sync.data.repository.BookClubRemoteDataSource
import uk.co.zlurgg.mybookshelf.sync.data.repository.BookSyncDataSource
import uk.co.zlurgg.mybookshelf.sync.data.repository.RemoteSyncDataSource
import uk.co.zlurgg.mybookshelf.sync.data.repository.ShelfSyncDataSource
import uk.co.zlurgg.mybookshelf.sync.data.repository.UserPreferencesDataSource

/**
 * Composite Firestore implementation that delegates to focused data sources.
 */
internal class FirestoreRemoteDataSource(
    bookSync: BookSyncDataSource,
    shelfSync: ShelfSyncDataSource,
    userPrefs: UserPreferencesDataSource,
    bookClub: BookClubRemoteDataSource,
) : RemoteSyncDataSource,
    BookSyncDataSource by bookSync,
    ShelfSyncDataSource by shelfSync,
    UserPreferencesDataSource by userPrefs,
    BookClubRemoteDataSource by bookClub
