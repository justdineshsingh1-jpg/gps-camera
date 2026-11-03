package com.gpsmapcamera.app.ui.gallery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gpsmapcamera.app.core.data.GalleryRepository
import com.gpsmapcamera.app.core.data.MediaFileItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    galleryRepository: GalleryRepository,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mediaList by remember { mutableStateOf<List<MediaFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<MediaFileItem?>(null) }

    fun refreshMedia() {
        scope.launch {
            isLoading = true
            mediaList = galleryRepository.getCapturedMedia()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshMedia()
    }

    if (selectedItem != null) {
        // Fullscreen Viewer
        val item = selectedItem!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top bar controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedItem = null },
                    modifier = Modifier.clip(CircleShape).background(Color(0x80000000))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        scope.launch {
                            galleryRepository.deleteMedia(item.uri)
                            selectedItem = null
                            refreshMedia()
                        }
                    },
                    modifier = Modifier.clip(CircleShape).background(Color(0x80000000))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                }
            }

            // Bottom action row: Share and Open
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Share Button
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (item.isVideo) "video/mp4" else "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, item.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Stamped Media"))
                    },
                    modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0x991E293B))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }

                // Play Video in native player (if video)
                if (item.isVideo) {
                    IconButton(
                        onClick = {
                            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(item.uri, "video/mp4")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(playIntent)
                        },
                        modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0x991E293B))
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = Color(0xFF00E676))
                    }
                }
            }
        }
    } else {
        // Thumbnail Grid Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GPS Map Gallery", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackToCamera) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
            },
            containerColor = Color.Black,
            modifier = modifier
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF2563EB)
                    )
                } else if (mediaList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No stamped photos or videos yet", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(mediaList, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { selectedItem = item }
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (item.isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xCC000000))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("VIDEO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
