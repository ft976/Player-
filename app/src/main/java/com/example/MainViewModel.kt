package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val localVideoRepository = LocalVideoRepository(application)
    private val youtubeRepository = YouTubeRepository()
    private val databaseRepository = DatabaseRepository(application)

    val allPlaylists = databaseRepository.allPlaylists
    val fullHistory = databaseRepository.fullHistory

    private val _localVideos = MutableStateFlow<List<LocalVideo>>(emptyList())
    val localVideos: StateFlow<List<LocalVideo>> = _localVideos.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    private val _youtubeStreamInfo = MutableStateFlow<Result<YouTubeStreamInfo>?>(null)
    val youtubeStreamInfo: StateFlow<Result<YouTubeStreamInfo>?> = _youtubeStreamInfo.asStateFlow()
    
    private val _isLoadingYoutube = MutableStateFlow(false)
    val isLoadingYoutube: StateFlow<Boolean> = _isLoadingYoutube.asStateFlow()

    fun fetchLocalVideos() {
        viewModelScope.launch {
            _isLoadingVideos.value = true
            try {
                val videos = localVideoRepository.getLocalVideos()
                _localVideos.value = videos
            } finally {
                _isLoadingVideos.value = false
            }
        }
    }

    fun fetchYoutubeStream(url: String) {
        viewModelScope.launch {
            _isLoadingYoutube.value = true
            _youtubeStreamInfo.value = null
            try {
                val result = youtubeRepository.getStreamInfo(url)
                _youtubeStreamInfo.value = result
            } finally {
                _isLoadingYoutube.value = false
            }
        }
    }
    
    fun clearYoutubeStreamInfo() {
        _youtubeStreamInfo.value = null
    }

    // Playlist Methods
    fun createPlaylist(name: String) = viewModelScope.launch { databaseRepository.createPlaylist(name) }
    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch { databaseRepository.deletePlaylist(playlist) }
    fun renamePlaylist(playlist: Playlist, newName: String) = viewModelScope.launch { databaseRepository.updatePlaylist(playlist.copy(name = newName)) }
    fun getPlaylistItems(playlistId: Long) = databaseRepository.getPlaylistItems(playlistId)
    fun addVideoToPlaylist(playlistId: Long, uri: String, title: String, isAudioOnly: Boolean, durationMillis: Long = 0, size: Long = 0) = viewModelScope.launch {
        databaseRepository.addVideoToPlaylist(PlaylistItem(
            playlistId = playlistId,
            videoUri = uri,
            title = title,
            isAudioOnly = isAudioOnly,
            durationMillis = durationMillis,
            size = size
        ))
    }
    fun removeVideoFromPlaylist(item: PlaylistItem) = viewModelScope.launch { databaseRepository.removeVideoFromPlaylist(item) }

    // History Methods
    fun saveHistory(videoUri: String, title: String, isAudioOnly: Boolean, position: Long, duration: Long) = viewModelScope.launch {
        databaseRepository.saveHistory(videoUri, title, isAudioOnly, position, duration)
    }
    fun clearHistory() = viewModelScope.launch { databaseRepository.clearHistory() }
    
    // Get last position
    suspend fun getHistoryPosition(uri: String): Long {
        return databaseRepository.getHistoryForVideo(uri)?.playbackPosition ?: 0L
    }
}
