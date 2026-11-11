package com.gpsmapcamera.app.ui.gallery

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gpsmapcamera.app.core.data.GalleryRepository
import com.gpsmapcamera.app.core.data.MediaFileItem
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    // Fullscreen viewer: index into mediaList, or null for the grid
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    // Multi-select in the grid (long-press to start)
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val selectionMode = selectedIds.isNotEmpty()
    // Items waiting for the user's confirmation. Every delete path sets this; nothing is deleted
    // until the user taps Delete in ConfirmDeleteDialog.
    var pendingDelete by remember { mutableStateOf<List<MediaFileItem>?>(null) }

    fun refreshMedia() {
        scope.launch {
            isLoading = mediaList.isEmpty()
            val list = galleryRepository.getCapturedMedia()
            mediaList = list
            selectedIds = selectedIds.filter { id -> list.any { it.id == id } }.toSet()
            viewerIndex = viewerIndex?.let { if (list.isEmpty()) null else it.coerceAtMost(list.size - 1) }
            isLoading = false
        }
    }

    // System consent for items this install does not own (Android 11+), after our own confirmation.
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { refreshMedia() }

    fun performDelete(items: List<MediaFileItem>) {
        scope.launch {
            val outcome = galleryRepository.deleteMedia(items.map { it.uri })
            selectedIds = emptySet()
            outcome.consentRequest?.let { sender ->
                consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
            refreshMedia()
        }
    }

    LaunchedEffect(Unit) {
        refreshMedia()
    }

    BackHandler(enabled = viewerIndex != null || selectionMode) {
        if (viewerIndex != null) viewerIndex = null else selectedIds = emptySet()
    }

    pendingDelete?.let { items ->
        ConfirmDeleteDialog(
            photoCount = items.count { !it.isVideo },
            videoCount = items.count { it.isVideo },
            onConfirm = {
                pendingDelete = null
                performDelete(items)
            },
            onDismiss = { pendingDelete = null }
        )
    }

    val openIndex = viewerIndex
    if (openIndex != null && mediaList.isNotEmpty()) {
        MediaViewer(
            items = mediaList,
            initialIndex = openIndex.coerceIn(0, mediaList.size - 1),
            onClose = { viewerIndex = null },
            onPageSettled = { viewerIndex = it },
            onRequestDelete = { item -> pendingDelete = listOf(item) },
            onShare = { item ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = if (item.isVideo) "video/mp4" else "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, item.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Stamped Media"))
            },
            onPlay = { item ->
                val playIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(item.uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(playIntent)
            }
        )
    } else {
        // Thumbnail Grid Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectionMode) "${selectedIds.size} selected" else "GPS Map Gallery",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (selectionMode) {
                            IconButton(onClick = { selectedIds = emptySet() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Cancel selection", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = onBackToCamera) {
                                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    },
                    actions = {
                        if (selectionMode) {
                            IconButton(onClick = {
                                selectedIds = if (selectedIds.size == mediaList.size) emptySet()
                                else mediaList.map { it.id }.toSet()
                            }) {
                                Icon(Icons.Rounded.SelectAll, contentDescription = "Select all", tint = Color(0xFF38BDF8))
                            }
                            IconButton(onClick = {
                                val items = mediaList.filter { it.id in selectedIds }
                                if (items.isNotEmpty()) pendingDelete = items
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete selected", tint = Color(0xFFFF5252))
                            }
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
                        Icon(
                            Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                        itemsIndexed(mediaList, key = { _, media -> media.id }) { index, item ->
                            val isSelected = item.id in selectedIds
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                        else Modifier
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                            } else {
                                                viewerIndex = index
                                            }
                                        },
                                        onLongClick = {
                                            selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                        }
                                    )
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val s = if (isSelected) 0.9f else 1f
                                            scaleX = s
                                            scaleY = s
                                        }
                                )

                                if (item.isVideo) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xCC000000))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            formatDuration(item.durationMs),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (selectionMode) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = if (isSelected) "Selected" else "Not selected",
                                        tint = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x66000000))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

/** Fullscreen swipeable viewer: photos support pinch-to-zoom, pan and double-tap zoom. */
@Composable
private fun MediaViewer(
    items: List<MediaFileItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    onPageSettled: (Int) -> Unit,
    onRequestDelete: (MediaFileItem) -> Unit,
    onShare: (MediaFileItem) -> Unit,
    onPlay: (MediaFileItem) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }
    // Page currently zoomed in; while zoomed, drags pan the photo instead of swiping pages.
    var zoomedPage by remember { mutableIntStateOf(-1) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { onPageSettled(it) }
    }

    val current = items.getOrNull(pagerState.currentPage.coerceIn(0, (items.size - 1).coerceAtLeast(0)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> items.getOrNull(page)?.id ?: page.toLong() },
            userScrollEnabled = zoomedPage != pagerState.currentPage,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { onPlay(item) },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            } else {
                ZoomableImage(
                    model = item.uri,
                    contentDescription = item.name,
                    // Zoom resets as soon as this page is no longer the settled one (swiped away).
                    isActive = page == pagerState.settledPage,
                    onZoomChanged = { zoomed ->
                        zoomedPage = when {
                            zoomed -> page
                            zoomedPage == page -> -1
                            else -> zoomedPage
                        }
                    }
                )
            }
        }

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
                onClick = onClose,
                modifier = Modifier.clip(CircleShape).background(Color(0x80000000))
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = current?.name ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${pagerState.currentPage + 1} / ${items.size}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { current?.let(onRequestDelete) },
                modifier = Modifier.clip(CircleShape).background(Color(0x80000000))
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
            }
        }

        // Bottom action row: Share and Play
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { current?.let(onShare) },
                modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0x991E293B))
            ) {
                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color(0xFF38BDF8))
            }

            if (current?.isVideo == true) {
                IconButton(
                    onClick = { onPlay(current) },
                    modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0x991E293B))
                ) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = "Play", tint = Color(0xFF00E676))
                }
            }
        }
    }
}

/**
 * Photo with pinch-to-zoom (1x–5x), pan (clamped to the edges) and double-tap to zoom in/out.
 * One-finger drags at 1x are left to the pager so swiping between items still works.
 */
@Composable
private fun ZoomableImage(
    model: Any,
    contentDescription: String,
    isActive: Boolean,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isActive) {
        if (!isActive) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    val isZoomed = scale > 1.01f
    LaunchedEffect(isZoomed) { onZoomChanged(isZoomed) }

    fun animateZoom(targetScale: Float, targetX: Float, targetY: Float) {
        val s0 = scale
        val x0 = offsetX
        val y0 = offsetY
        scope.launch {
            animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(durationMillis = 260)) { v, _ ->
                scale = s0 + (targetScale - s0) * v
                offsetX = x0 + (targetX - x0) * v
                offsetY = y0 + (targetY - y0) * v
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1.01f) {
                            animateZoom(1f, 0f, 0f)
                        } else {
                            val target = ZoomMath.DOUBLE_TAP_SCALE
                            animateZoom(
                                target,
                                ZoomMath.offsetForTap(tap.x, viewport.width.toFloat(), target),
                                ZoomMath.offsetForTap(tap.y, viewport.height.toFloat(), target)
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers > 1 || scale > 1.01f) {
                            val newScale = ZoomMath.clampScale(scale * event.calculateZoom())
                            val pan = event.calculatePan()
                            scale = newScale
                            offsetX = ZoomMath.clampOffset(offsetX + pan.x, viewport.width.toFloat(), newScale)
                            offsetY = ZoomMath.clampOffset(offsetY + pan.y, viewport.height.toFloat(), newScale)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}
