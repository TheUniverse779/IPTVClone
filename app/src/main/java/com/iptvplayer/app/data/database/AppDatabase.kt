package com.iptvplayer.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class, ChannelEntity::class, SingleStreamEntity::class,
        XtreamProfileEntity::class, XtreamCategoryEntity::class, XtreamLiveEntity::class,
        XtreamVodEntity::class, XtreamSeriesEntity::class, XtreamEpisodeEntity::class,
        SearchHistoryEntity::class, FavouriteMatchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun singleStreamDao(): SingleStreamDao
    abstract fun xtreamDao(): XtreamDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun matchDao(): MatchDao
}
