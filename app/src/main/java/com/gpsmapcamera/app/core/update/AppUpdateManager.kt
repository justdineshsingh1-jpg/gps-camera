package com.gpsmapcamera.app.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object NoUpdateAvailable : UpdateState
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val apkSize: Long
    ) : UpdateState
    data class Error(val message: String) : UpdateState
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val releasesApiUrl = "https://api.github.com/repos/justdineshsingh1-jpg/gps-camera/releases/latest"

    /**
     * Checks GitHub Releases API for a newer version than currently installed.
     * @param silent If true, ignores NoUpdateAvailable and network errors (used on startup).
     */
    suspend fun checkForUpdates(silent: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                if (!silent) _updateState.value = UpdateState.Checking

                val currentVersion = getCurrentVersionName()
                val (latestVersion, releaseNotes, downloadUrl, apkSize) = fetchLatestReleaseInfo()

                if (isVersionNewer(latestVersion, currentVersion) && downloadUrl.isNotBlank()) {
                    _updateState.value = UpdateState.UpdateAvailable(
                        latestVersion = latestVersion,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl,
                        apkSize = apkSize
                    )
                } else {
                    if (!silent) {
                        _updateState.value = UpdateState.NoUpdateAvailable
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _updateState.value = UpdateState.Error(e.message ?: "Failed to check for updates")
                }
            }
        }
    }

    /**
     * Opens the direct download URL in the device browser / download manager cleanly
     * without needing sensitive REQUEST_INSTALL_PACKAGES permissions.
     */
    fun startDownload(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _updateState.value = UpdateState.Idle
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Could not open browser: ${e.message}")
        }
    }

    fun dismiss() {
        _updateState.value = UpdateState.Idle
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun fetchLatestReleaseInfo(): ReleaseInfo {
        val url = URL(releasesApiUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "GPSMapCamera-Android")
        conn.connectTimeout = 10000
        conn.readTimeout = 15000

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)

        val rawTag = json.optString("tag_name", "")
        // Clean tag (e.g. "v1.2.1-android" -> "1.2.1")
        val cleanTag = rawTag.removePrefix("v").substringBefore("-")
        val notes = json.optString("body", "")

        var downloadUrl = ""
        var apkSize = 0L

        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        return ReleaseInfo(
            version = cleanTag,
            notes = notes,
            downloadUrl = downloadUrl,
            size = apkSize
        )
    }

    /**
     * Compares two semantic version strings (e.g. "1.2.2" > "1.2.1").
     */
    fun isVersionNewer(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private data class ReleaseInfo(
        val version: String,
        val notes: String,
        val downloadUrl: String,
        val size: Long
    )
}
