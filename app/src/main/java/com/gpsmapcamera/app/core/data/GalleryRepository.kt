package com.gpsmapcamera.app.core.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of deleting media. [consentRequest] is set on Android 11+ when some items are not owned by
 * this install (e.g. after a reinstall): launching it shows the system's own delete consent for them.
 */
data class DeleteOutcome(
    val deletedCount: Int,
    val pendingUris: List<Uri> = emptyList(),
    val consentRequest: IntentSender? = null
)

data class MediaFileItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val isVideo: Boolean,
    val durationMs: Long = 0L
)

@Singleton
class GalleryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getCapturedMedia(): List<MediaFileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaFileItem>()

        // 1. Query Images in Pictures/GPSMapCamera
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val imageSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        } else null
        val imageSelectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%GPSMapCamera%")
        } else null

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            imageSelection,
            imageSelectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val date = cursor.getLong(dateCol) * 1000
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                items.add(MediaFileItem(id, uri, name, date, isVideo = false))
            }
        }

        // 2. Query Videos in Movies/GPSMapCamera
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )
        val videoSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        } else null
        val videoSelectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%GPSMapCamera%")
        } else null

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            videoSelection,
            videoSelectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val date = cursor.getLong(dateCol) * 1000
                val duration = cursor.getLong(durationCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                items.add(MediaFileItem(id, uri, name, date, isVideo = true, durationMs = duration))
            }
        }

        items.sortedByDescending { it.dateTaken }
    }

    /** Newest photo or video saved by the app (for the camera's gallery thumbnail), or null. */
    suspend fun getLatestMedia(): MediaFileItem? = getCapturedMedia().firstOrNull()

    /**
     * Deletes media. Callers must have shown the user a confirmation first
     * (see ui/gallery/DeleteConfirmation) — every delete path in the app goes through it.
     */
    suspend fun deleteMedia(uri: Uri): Boolean = deleteMedia(listOf(uri)).deletedCount > 0

    suspend fun deleteMedia(uris: List<Uri>): DeleteOutcome = withContext(Dispatchers.IO) {
        var deleted = 0
        val needConsent = mutableListOf<Uri>()
        for (uri in uris) {
            try {
                if (context.contentResolver.delete(uri, null, null) > 0) deleted++
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    needConsent.add(uri)
                }
            } catch (e: Exception) {
                // Skip items that vanished or cannot be deleted
            }
        }
        val request = if (needConsent.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createDeleteRequest(context.contentResolver, needConsent).intentSender
            } catch (e: Exception) {
                null
            }
        } else null
        DeleteOutcome(deleted, needConsent, request)
    }
}
