package com.example

import android.content.Context
import kotlinx.coroutines.flow.Flow

class DatabaseRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val playlistDao = database.playlistDao()
    private val historyDao = database.videoHistoryDao()

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val fullHistory: Flow<List<VideoHistory>> = historyDao.getHistory()

    suspend fun createPlaylist(name: String) = playlistDao.insertPlaylist(Playlist(name = name))
    suspend fun updatePlaylist(playlist: Playlist) = playlistDao.updatePlaylist(playlist)
    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)

    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>> = playlistDao.getPlaylistItems(playlistId)
    suspend fun addVideoToPlaylist(item: PlaylistItem) = playlistDao.insertPlaylistItem(item)
    suspend fun removeVideoFromPlaylist(item: PlaylistItem) = playlistDao.deletePlaylistItem(item)

    suspend fun getHistoryForVideo(uri: String): VideoHistory? = historyDao.getHistoryByUri(uri)
    
    suspend fun saveHistory(videoUri: String, title: String, isAudioOnly: Boolean, position: Long, duration: Long) {
        val existing = historyDao.getHistoryByUri(videoUri)
        if (existing != null) {
            historyDao.updateHistory(existing.copy(
                lastPlayedAt = System.currentTimeMillis(),
                playbackPosition = position,
                durationMillis = duration
            ))
        } else {
            historyDao.insertHistory(VideoHistory(
                videoUri = videoUri,
                title = title,
                isAudioOnly = isAudioOnly,
                playbackPosition = position,
                durationMillis = duration
            ))
        }
    }

    suspend fun clearHistory() = historyDao.clearHistory()
}
