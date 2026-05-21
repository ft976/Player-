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

data class YouTubeStreamInfo(
    val title: String,
    val videoUrl: String?,
    val audioUrl: String?,
    val thumbnailUrl: String?,
    val description: String?,
    val uploader: String?,
    val videoId: String
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

    suspend fun getStreamInfo(youtubeUrl: String): Result<YouTubeStreamInfo> = withContext(Dispatchers.IO) {
        try {
            val videoId = extractVideoId(youtubeUrl) ?: throw IllegalArgumentException("Invalid YouTube URL")
            Log.d(TAG, "Extracted Video ID: $videoId")

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
                    val apiUrl = "$baseUrl/streams/$videoId"
                    Log.d(TAG, "Trying Piped Instance API URL: $apiUrl")
                    val url = URL(apiUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 3500
                    connection.readTimeout = 3500
                    connection.setRequestProperty("Accept", "application/json")
                    
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        val title = json.optString("title", "YouTube Video")
                        val description = json.optString("description", "")
                        val uploader = json.optString("uploader", "")
                        val piperThumbnail = json.optString("thumbnailUrl", "")
                        val thumbnailUrl = if (piperThumbnail.isNotBlank()) piperThumbnail else "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                        
                        val videoStreams = json.optJSONArray("videoStreams")
                        var bestVideoUrl: String? = null
                        if (videoStreams != null && videoStreams.length() > 0) {
                            for (i in 0 until videoStreams.length()) {
                                val stream = videoStreams.getJSONObject(i)
                                // Standard quality preference or grab standard mp4
                                val videoFormat = stream.optString("format", "")
                                val quality = stream.optString("quality", "")
                                if (videoFormat == "MPEG_4" || quality.contains("720p") || quality.contains("1080p") || quality.contains("360p")) {
                                    bestVideoUrl = stream.optString("url")
                                    if (videoFormat == "MPEG_4") break // prioritize mp4
                                }
                            }
                            if (bestVideoUrl == null) bestVideoUrl = videoStreams.getJSONObject(0).optString("url")
                        }
                        
                        val audioStreams = json.optJSONArray("audioStreams")
                        var bestAudioUrl: String? = null
                        if (audioStreams != null && audioStreams.length() > 0) {
                            bestAudioUrl = audioStreams.getJSONObject(0).optString("url")
                        }
                        
                        Log.d(TAG, "Successfully extracted stream urls from instance: $baseUrl")
                        return@withContext Result.success(YouTubeStreamInfo(title, bestVideoUrl, bestAudioUrl, thumbnailUrl, description, uploader, videoId))
                    } else {
                        Log.w(TAG, "Instance $baseUrl returned non-200 code: ${connection.responseCode}")
                        lastError = Exception("Instance $baseUrl returned code: ${connection.responseCode}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed connection to instance: $baseUrl", e)
                    lastError = e
                }
            }
            
            Result.failure(lastError ?: Exception("Could not fetch YouTube streams from any Piped instance."))
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
