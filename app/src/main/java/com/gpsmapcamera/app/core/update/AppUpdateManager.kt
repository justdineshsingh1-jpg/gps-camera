package com.gpsmapcamera.app.core.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState
    data class DownloadCompleted(val apkFile: File) : UpdateState
    data class PermissionNeeded(val apkFile: File) : UpdateState
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

    suspend fun startDownload(downloadUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                _updateState.value = UpdateState.Downloading(0, 0L, 0L)

                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.cacheDir
                val destFile = File(downloadDir, "gps-map-camera-latest.apk")
                if (destFile.exists()) {
                    destFile.delete()
                }

                var currentUrl = downloadUrl
                var connection: HttpURLConnection
                var redirectCount = 0

                // Follow HTTP redirects (GitHub Releases redirect to AWS S3 / fastly)
                while (true) {
                    val url = URL(currentUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.setRequestProperty("User-Agent", "GPSMapCamera-Android")

                    val status = connection.responseCode
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER ||
                        status == 307 || status == 308
                    ) {
                        currentUrl = connection.getHeaderField("Location")
                        redirectCount++
                        if (redirectCount > 8) throw IllegalStateException("Too many redirects")
                        continue
                    }
                    break
                }

                val totalLength = connection.contentLengthLong
                var downloadedLength = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var lastReportedPercent = -1

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedLength += read

                            val percent = if (totalLength > 0) {
                                ((downloadedLength * 100) / totalLength).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }

                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                _updateState.value = UpdateState.Downloading(
                                    progressPercent = percent,
                                    downloadedBytes = downloadedLength,
                                    totalBytes = totalLength
                                )
                            }
                        }
                        output.flush()
                    }
                }

                _updateState.value = UpdateState.DownloadCompleted(destFile)
                // Trigger installation prompt immediately after download completes
                installApk(destFile)
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Download failed: ${e.message}")
            }
        }
    }

    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                _updateState.value = UpdateState.Error("APK file not found")
                return
            }

            // Android 8.0+ (API 26+) requires REQUEST_INSTALL_PACKAGES permission toggle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    _updateState.value = UpdateState.PermissionNeeded(file)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Install failed: ${e.message}")
        }
    }

    fun requestInstallPermission(activity: Activity? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        // Clean tag (e.g. "v1.1.9-android" -> "1.1.9")
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
     * Compares two semantic version strings (e.g. "1.2.0" > "1.1.9").
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
