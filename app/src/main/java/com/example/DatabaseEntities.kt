package com.example

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val videoUri: String,
    val title: String,
    val isAudioOnly: Boolean,
    val durationMillis: Long = 0,
    val size: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_history")
data class VideoHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUri: String,
    val title: String,
    val isAudioOnly: Boolean,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playbackPosition: Long = 0,
    val durationMillis: Long = 0
)
