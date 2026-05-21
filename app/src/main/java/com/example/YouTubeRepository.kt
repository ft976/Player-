package com.example

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class YouTubeVideoFormat(
    val url: String,
    val quality: String,
    val format: String,
    val videoOnly: Boolean
)

data class YouTubeAudioFormat(
    val url: String,
    val format: String,
    val bitrate: Int
)

data class YouTubeStreamInfo(
    val title: String,
    val videoUrl: String?,
    val audioUrl: String?,
    val thumbnailUrl: String?,
    val description: String?,
    val uploader: String?,
    val videoId: String,
    val videoFormats: List<YouTubeVideoFormat> = emptyList(),
    val audioFormats: List<YouTubeAudioFormat> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistVideos: List<YouTubeStreamInfo> = emptyList()
)

class YouTubeRepository {
    private val TAG = "YouTubeRepository"

    private val staticInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.private.coffee",
        "https://api.piped.projectsegfaut.im",
        "https://pipedapi.colby.gg",
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.tokhmi.xyz"
    )

    fun extractPlaylistId(youtubeUrl: String): String? {
        val trimmed = youtubeUrl.trim()
        val uri = try { Uri.parse(trimmed) } catch (e: Exception) { null }
        if (uri != null) {
            val listParam = uri.getQueryParameter("list")
            if (!listParam.isNullOrBlank()) return listParam
        }
        if (trimmed.contains("list=")) {
            val idx = trimmed.indexOf("list=") + 5
            val sub = trimmed.substring(idx)
            val endIdx = sub.indexOfAny(charArrayOf('?', '&', '/'))
            return if (endIdx == -1) sub else sub.substring(0, endIdx)
        }
        return null
    }

    suspend fun getStreamInfo(youtubeUrl: String): Result<YouTubeStreamInfo> = withContext(Dispatchers.IO) {
        try {
            val playlistId = extractPlaylistId(youtubeUrl)
            val isPlaylistRequest = playlistId != null

            val videoId = if (!isPlaylistRequest) {
                extractVideoId(youtubeUrl) ?: throw IllegalArgumentException("Invalid YouTube URL")
            } else null

            Log.d(TAG, "Request - playlistId: $playlistId, videoId: $videoId")

            // Create list of base API URLs starting with our static fallback ones
            val baseUrls = ArrayList<String>(staticInstances)

            // Try to fetch dynamic piped instances if static ones fail, or prepend them
            try {
                val dynamicInstances = fetchDynamicInstances()
                for (url in dynamicInstances) {
                    if (!baseUrls.contains(url)) {
                        baseUrls.add(url)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching dynamic instances, using static fallback pool", e)
            }

            var lastError: Exception? = null

            for (baseUrl in baseUrls) {
                try {
                    val apiUrl = if (isPlaylistRequest) {
                        "$baseUrl/playlists/$playlistId"
                    } else {
                        "$baseUrl/streams/$videoId"
                    }
                    Log.d(TAG, "Trying Piped Instance API URL: $apiUrl")
                    val url = URL(apiUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    connection.setRequestProperty("Accept", "application/json")
                    
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        
                        if (isPlaylistRequest) {
                            val name = json.optString("name", "YouTube Playlist")
                            val uploader = json.optString("uploader", "Various Artists")
                            val playlistThumbnail = json.optString("thumbnailUrl", "")
                            
                            val relatedStreams = json.optJSONArray("relatedStreams")
                            val playlistVideos = mutableListOf<YouTubeStreamInfo>()
                            if (relatedStreams != null) {
                                for (i in 0 until relatedStreams.length()) {
                                    val streamObj = relatedStreams.getJSONObject(i)
                                    val streamPath = streamObj.optString("url", "")
                                    val vId = if (streamPath.contains("v=")) {
                                        streamPath.substringAfter("v=")
                                    } else {
                                        streamPath.substringAfterLast("/")
                                    }
                                    val itemTitle = streamObj.optString("title", "Video Item")
                                    val itemThumbnail = streamObj.optString("thumbnail", "")
                                    val itemUploader = streamObj.optString("uploaderName", "")
                                    playlistVideos.add(
                                        YouTubeStreamInfo(
                                            title = itemTitle,
                                            videoUrl = null,
                                            audioUrl = null,
                                            thumbnailUrl = itemThumbnail,
                                            description = null,
                                            uploader = itemUploader,
                                            videoId = vId,
                                            isPlaylist = false
                                        )
                                    )
                                }
                            }
                            Log.d(TAG, "Successfully extracted playlist: $name with ${playlistVideos.size} items")
                            return@withContext Result.success(
                                YouTubeStreamInfo(
                                    title = name,
                                    videoUrl = null,
                                    audioUrl = null,
                                    thumbnailUrl = playlistThumbnail,
                                    description = "YouTube Playlist",
                                    uploader = uploader,
                                    videoId = playlistId ?: "",
                                    isPlaylist = true,
                                    playlistVideos = playlistVideos
                                )
                            )
                        } else {
                            val title = json.optString("title", "YouTube Video")
                            val description = json.optString("description", "")
                            val uploader = json.optString("uploader", "")
                            val piperThumbnail = json.optString("thumbnailUrl", "")
                            val thumbnailUrl = if (piperThumbnail.isNotBlank()) piperThumbnail else "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                            
                            val videoStreams = json.optJSONArray("videoStreams")
                            val parsedVideoFormats = mutableListOf<YouTubeVideoFormat>()
                            var bestVideoUrl: String? = null
                            if (videoStreams != null && videoStreams.length() > 0) {
                                for (i in 0 until videoStreams.length()) {
                                    val stream = videoStreams.getJSONObject(i)
                                    val streamUrl = stream.optString("url", "")
                                    val quality = stream.optString("quality", "")
                                    val format = stream.optString("format", "")
                                    val videoOnly = stream.optBoolean("videoOnly", false)
                                    if (streamUrl.isNotBlank()) {
                                        parsedVideoFormats.add(YouTubeVideoFormat(streamUrl, quality, format, videoOnly))
                                    }
                                    if (bestVideoUrl == null || (format == "MPEG_4" && !videoOnly)) {
                                        bestVideoUrl = streamUrl
                                    }
                                }
                            }
                            
                            val audioStreams = json.optJSONArray("audioStreams")
                            val parsedAudioFormats = mutableListOf<YouTubeAudioFormat>()
                            var bestAudioUrl: String? = null
                            if (audioStreams != null && audioStreams.length() > 0) {
                                for (i in 0 until audioStreams.length()) {
                                    val stream = audioStreams.getJSONObject(i)
                                    val streamUrl = stream.optString("url", "")
                                    val format = stream.optString("format", "M4A")
                                    val bitrate = stream.optInt("bitrate", 128)
                                    if (streamUrl.isNotBlank()) {
                                        parsedAudioFormats.add(YouTubeAudioFormat(streamUrl, format, bitrate))
                                    }
                                    if (bestAudioUrl == null) {
                                        bestAudioUrl = streamUrl
                                    }
                                }
                            }
                            
                            Log.d(TAG, "Successfully extracted streams logic. Videos: ${parsedVideoFormats.size}, Audios: ${parsedAudioFormats.size}")
                            return@withContext Result.success(
                                YouTubeStreamInfo(
                                    title = title,
                                    videoUrl = bestVideoUrl,
                                    audioUrl = bestAudioUrl,
                                    thumbnailUrl = thumbnailUrl,
                                    description = description,
                                    uploader = uploader,
                                    videoId = videoId ?: "",
                                    videoFormats = parsedVideoFormats,
                                    audioFormats = parsedAudioFormats,
                                    isPlaylist = false
                                )
                            )
                        }
                    } else {
                        Log.w(TAG, "Instance $baseUrl returned non-200 code: ${connection.responseCode}")
                        lastError = Exception("Instance $baseUrl returned code: ${connection.responseCode}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed connection to instance: $baseUrl", e)
                    lastError = e
                }
            }
            
            Result.failure(lastError ?: Exception("Could not fetch YouTube streams from Piped instances."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchDynamicInstances(): List<String> = withContext(Dispatchers.IO) {
        val list = ArrayList<String>()
        try {
            val url = URL("https://piped-instances.kavin.rocks/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val api_url = obj.optString("api_url", "")
                    val uptime7d = obj.optDouble("uptime_7d", 0.0)
                    // filter to instances that actually have a valid URL and reasonable uptime
                    if (api_url.isNotBlank() && (uptime7d > 95.0 || uptime7d.isNaN())) {
                        list.add(api_url.trimEnd('/'))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dynamic instance list fetch failed", e)
        }
        list
    }

    fun extractVideoId(youtubeUrl: String): String? {
        val trimmed = youtubeUrl.trim()
        if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?")) {
            return trimmed
        }
        
        if (trimmed.contains("/shorts/")) {
            val idx = trimmed.indexOf("/shorts/") + "/shorts/".length
            val sub = trimmed.substring(idx)
            val endIdx = sub.indexOfAny(charArrayOf('?', '&', '/'))
            return if (endIdx == -1) sub else sub.substring(0, endIdx)
        }
        
        val pattern = "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=))((\\w|-){11})(?:\\S+)?$"
        val regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = regex.matcher(trimmed)
        if (matcher.matches()) {
            return matcher.group(1)
        }
        
        try {
            val uri = Uri.parse(trimmed)
            if (uri.host?.contains("youtu.be") == true) {
                return uri.lastPathSegment
            } else if (uri.host?.contains("youtube.com") == true) {
                return uri.getQueryParameter("v") ?: uri.lastPathSegment
            }
        } catch (e: Exception) {
            // ignore
        }
        
        val backupPattern = "(?:v=([^\\&\\?]+)|youtu\\.be\\/([^\\&\\?]+)|embed\\/([^\\&\\?]+)|shorts\\/([^\\&\\?]+))"
        val backupMatcher = java.util.regex.Pattern.compile(backupPattern).matcher(trimmed)
        if (backupMatcher.find()) {
            for (i in 1..backupMatcher.groupCount()) {
                val g = backupMatcher.group(i)
                if (g != null && g.length == 11) return g
            }
        }
        
        return null
    }
}
