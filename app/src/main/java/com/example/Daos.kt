package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItem)

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItem)
}

@Dao
interface VideoHistoryDao {
    @Query("SELECT * FROM video_history ORDER BY lastPlayedAt DESC")
    fun getHistory(): Flow<List<VideoHistory>>

    @Query("SELECT * FROM video_history WHERE videoUri = :uri LIMIT 1")
    suspend fun getHistoryByUri(uri: String): VideoHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: VideoHistory)

    @Update
    suspend fun updateHistory(history: VideoHistory)

    @Query("DELETE FROM video_history")
    suspend fun clearHistory()
}
