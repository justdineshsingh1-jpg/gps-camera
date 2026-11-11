package com.gpsmapcamera.app.ui.gallery

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Title/body for a delete confirmation. Pure so it is unit-tested. */
object DeleteConfirmationText {
    const val BODY = "This can't be undone."

    fun title(photoCount: Int, videoCount: Int): String {
        val total = photoCount + videoCount
        return when {
            total <= 1 && videoCount == 1 -> "Delete this video?"
            total <= 1 -> "Delete this photo?"
            videoCount == 0 -> "Delete $total photos?"
            photoCount == 0 -> "Delete $total videos?"
            else -> "Delete $total items?"
        }
    }
}

/**
 * The single confirmation dialog used by every delete path (viewer, multi-select in the grid).
 * Nothing is deleted until the user taps Delete.
 */
@Composable
fun ConfirmDeleteDialog(
    photoCount: Int,
    videoCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = Color(0xFFF87171)) },
        title = {
            Text(
                text = DeleteConfirmationText.title(photoCount, videoCount),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = { Text(text = DeleteConfirmationText.BODY, color = Color(0xFFCBD5E1)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp)
    )
}
