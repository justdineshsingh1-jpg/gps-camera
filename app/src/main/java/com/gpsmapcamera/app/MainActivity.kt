package com.gpsmapcamera.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gpsmapcamera.app.core.data.GalleryRepository
import com.gpsmapcamera.app.ui.camera.CameraScreen
import com.gpsmapcamera.app.ui.camera.CameraViewModel
import com.gpsmapcamera.app.ui.gallery.GalleryScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

enum class AppDestination {
    CAMERA,
    GALLERY
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    @Inject
    lateinit var galleryRepository: GalleryRepository

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO
    )

    private var hasPermissions by mutableStateOf(false)
    private var currentDestination by mutableStateOf(AppDestination.CAMERA)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val cameraGranted = results[Manifest.permission.CAMERA] == true
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermissions = cameraGranted && locationGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the screen on while camera is active to prevent display sleep / video recording interruption
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        checkAndRequestPermissions()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    if (hasPermissions) {
                        when (currentDestination) {
                            AppDestination.CAMERA -> {
                                CameraScreen(
                                    viewModel = cameraViewModel,
                                    onOpenGallery = { currentDestination = AppDestination.GALLERY }
                                )
                            }
                            AppDestination.GALLERY -> {
                                GalleryScreen(
                                    galleryRepository = galleryRepository,
                                    onBackToCamera = { currentDestination = AppDestination.CAMERA }
                                )
                            }
                        }
                    } else {
                        PermissionRationaleScreen(
                            onRequestPermissions = { checkAndRequestPermissions() }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val allGranted = requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            hasPermissions = true
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }
}

@Composable
fun PermissionRationaleScreen(
    onRequestPermissions: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GPS Map Camera",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This app requires Camera, Audio, and Location permissions to capture photos and videos with burned-in GPS stamps, Plus Codes, and date stamps — both online and in airplane mode.",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Grant Permissions", fontWeight = FontWeight.Bold)
            }
        }
    }
}
