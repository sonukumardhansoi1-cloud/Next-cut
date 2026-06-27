package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.data.model.AspectRatio
import com.example.data.repository.ProjectState
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.PremiumSlider
import com.example.ui.theme.LocalThemeConfig
import com.example.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Imported Clip"
}

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    projectId: String,
    onBack: () -> Unit
) {
    val config = LocalThemeConfig.current
    val projectState by viewModel.projectState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }

    // Gesture States on video preview
    var previewScale by remember { mutableStateOf(1.0f) }
    var previewOffsetX by remember { mutableStateOf(0f) }
    var previewOffsetY by remember { mutableStateOf(0f) }

    // Ensure project is loaded on entry
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    // Interactive simulated playhead progress loop
    LaunchedEffect(viewModel.isPlaying) {
        if (viewModel.isPlaying) {
            while (viewModel.isPlaying) {
                delay(30)
                viewModel.currentSeekMs = (viewModel.currentSeekMs + 30L)
                if (viewModel.currentSeekMs >= viewModel.totalDurationMs) {
                    viewModel.currentSeekMs = 0L
                    viewModel.isPlaying = false
                }
            }
        }
    }

    val state = projectState ?: return // Guard loading state

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(config.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ----------------------------------------------------
            // TOP CONTROLS BAR (Undo, Redo, Export)
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            viewModel.isPlaying = false
                            onBack()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(config.surface.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = state.project.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }

                // Undo/Redo & Render Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(config.surface.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White)
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(config.surface.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = Color.White)
                    }

                    GlassButton(
                        onClick = { showExportDialog = true },
                        text = "Export",
                        config = config,
                        isPrimary = true,
                        modifier = Modifier.height(38.dp)
                    )
                }
            }

            // ----------------------------------------------------
            // VIDEO PREVIEW BOX
            // ----------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            previewScale = (previewScale * zoom).coerceIn(0.5f, 4.0f)
                            previewOffsetX += pan.x
                            previewOffsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background Canvas Options
                when (state.project.canvasType) {
                    CanvasType.BLUR -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(20.dp)
                                .background(Color(state.project.canvasColor).copy(alpha = 0.3f))
                        )
                    }
                    CanvasType.SOLID -> {
                        Box(modifier = Modifier.fillMaxSize().background(Color(state.project.canvasColor)))
                    }
                    CanvasType.GRADIENT -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(state.project.canvasColor), config.background)
                                    )
                                )
                        )
                    }
                }

                // Active Aspect Ratio Screen Viewport
                val screenRatio = state.project.aspectRatio.ratio
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        .aspectRatio(screenRatio)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.DarkGray.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Display Visual Scene based on active frames
                    VideoScenePreview(
                        viewModel = viewModel,
                        state = state,
                        scale = previewScale,
                        offsetX = previewOffsetX,
                        offsetY = previewOffsetY,
                        config = config
                    )
                }
            }

            // ----------------------------------------------------
            // PLAYBACK CONTROLS STRIP
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timestamp
                Text(
                    text = formatTime(viewModel.currentSeekMs) + " / " + formatTime(viewModel.totalDurationMs),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Main Play controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.stepFrame(false) }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Step Frame Back", tint = Color.White)
                    }

                    IconButton(
                        onClick = { viewModel.isPlaying = !viewModel.isPlaying },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(config.primary)
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play / Pause",
                            tint = config.background,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.stepFrame(true) }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Step Frame Forward", tint = Color.White)
                    }
                }

                // Snap/Quick Trim indicator
                IconButton(onClick = {
                    previewScale = 1.0f
                    previewOffsetX = 0f
                    previewOffsetY = 0f
                }) {
                    Icon(Icons.Outlined.ZoomOutMap, contentDescription = "Reset Zoom", tint = Color.White.copy(alpha = 0.5f))
                }
            }

            // ----------------------------------------------------
            // MULTI-LAYER TIMELINE
            // ----------------------------------------------------
            TimelineLayout(
                viewModel = viewModel,
                state = state,
                config = config,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            )

            // ----------------------------------------------------
            // EDITING MODE PARAMETER PANEL
            // ----------------------------------------------------
            OptionsContextPanel(
                viewModel = viewModel,
                state = state,
                config = config,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            )

            // ----------------------------------------------------
            // CORE EDITING NAVIGATION TABS
            // ----------------------------------------------------
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(config.surface)
                    .border(1.dp, config.glassBorder)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                val tabs = listOf(
                    "TRIM" to Icons.Filled.ContentCut,
                    "TEXT" to Icons.Filled.TextFields,
                    "TRANSITIONS" to Icons.Filled.SwapHoriz,
                    "EFFECTS" to Icons.Filled.AutoAwesome,
                    "AUDIO" to Icons.Filled.MusicNote,
                    "COLOR_GRADING" to Icons.Filled.Tune,
                    "CHROMA_KEY" to Icons.Filled.Layers,
                    "STICKERS" to Icons.Filled.EmojiEmotions,
                    "CANVAS" to Icons.Filled.AspectRatio
                )
                items(tabs) { (tab, icon) ->
                    val isSelected = viewModel.activeEditorTab == tab
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.activeEditorTab = tab }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab,
                            tint = if (isSelected) config.primary else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.replace("_", " "),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Export Render dialog
        if (showExportDialog) {
            ExportPresetDialog(
                viewModel = viewModel,
                projectName = state.project.name,
                onDismiss = { showExportDialog = false },
                config = config
            )
        }

        // Subscription Paywall Dialog
        if (com.example.ui.theme.SubscriptionManager.showPaywallDialog) {
            SubscriptionPaywallDialog(
                onDismiss = { com.example.ui.theme.SubscriptionManager.showPaywallDialog = false },
                config = config
            )
        }
    }
}

// --------------------------------------------------------------------------------------------------
// VIDEO SCENE SIMULATION RENDERING WITH CURVED FILTERS & EFFECTS
// --------------------------------------------------------------------------------------------------
@Composable
fun VideoScenePreview(
    viewModel: EditorViewModel,
    state: ProjectState,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    config: com.example.ui.theme.ThemeConfig
) {
    val currentTime = viewModel.currentSeekMs

    // Find if a clip matches current seek position
    val activeClip = state.clips.firstOrNull { clip ->
        val duration = ((clip.trimEndMs - clip.trimStartMs) / clip.speed).toLong()
        currentTime >= clip.startInTimelineMs && currentTime < (clip.startInTimelineMs + duration)
    }

    // Color matrix filter computation based on active ColorGrading
    val grading = state.colorGrading
    val matrix = remember(grading) {
        ColorMatrix().apply {
            setToSaturation(grading.saturation)
            // Simulating Brightness and Contrast
            val m = values
            m[4] = grading.brightness * 255f // Add translation component
            m[0] = grading.contrast
            m[6] = grading.contrast
            m[12] = grading.contrast
        }
    }

    // Find any overlay stickers
    val activeStickers = state.stickers.filter {
        currentTime >= it.startInTimelineMs && currentTime < (it.startInTimelineMs + it.durationMs)
    }

    // Find any overlay texts
    val activeTexts = state.textItems.filter {
        currentTime >= it.startInTimelineMs && currentTime < (it.startInTimelineMs + it.durationMs)
    }

    // Active visual effects (glitch, vhs, cinematic, dream)
    val activeFX = state.effects.firstOrNull {
        currentTime >= it.startInTimelineMs && currentTime < (it.startInTimelineMs + it.durationMs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                if (activeClip != null) {
                    val progress = (currentTime - activeClip.startInTimelineMs).toFloat() / activeClip.durationMs.toFloat()
                    val pulse = Math.abs(Math.sin((progress * Math.PI * 2.0))).toFloat()

                    if (activeClip.chromaKeyEnabled) {
                        val similarity = activeClip.chromaKeySimilarity
                        val smoothness = activeClip.chromaKeySmoothness
                        val spill = activeClip.chromaKeySpill
                        val keyColor = Color(activeClip.chromaKeyColor)

                        // 1. Draw Project Background Canvas (which is what we see behind the keyed subject)
                        when (state.project.canvasType) {
                            CanvasType.SOLID -> {
                                drawRect(color = Color(state.project.canvasColor))
                            }
                            CanvasType.GRADIENT -> {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(state.project.canvasColor), Color(0xFF101114))
                                    )
                                )
                            }
                            CanvasType.BLUR -> {
                                drawRect(color = Color(state.project.canvasColor).copy(alpha = 0.5f))
                            }
                        }

                        // 2. Render a professional, subtle transparency checkerboard grid overlay
                        val cellSize = 30f
                        val cols = (size.width / cellSize).toInt() + 1
                        val rows = (size.height / cellSize).toInt() + 1
                        for (c in 0 until cols) {
                            for (r in 0 until rows) {
                                if ((c + r) % 2 == 0) {
                                    drawRect(
                                        color = Color.White.copy(alpha = 0.04f),
                                        topLeft = Offset(c * cellSize, r * cellSize),
                                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                    )
                                }
                            }
                        }

                        // 3. Realistic Green-Screen Bleed (If similarity/threshold is low, some of the chroma key color is still visible)
                        val thresholdCutoff = 0.75f
                        if (similarity < thresholdCutoff) {
                            val bleedAlpha = ((thresholdCutoff - similarity) / thresholdCutoff).coerceIn(0.0f, 1.0f)
                            drawRect(color = keyColor.copy(alpha = bleedAlpha * 0.9f))
                        }

                        // 4. Chroma-Keyed subject rendering with Edge Softness & Spill reduction
                        val coreColor = Color(0xFF6366F1)
                        val spillAlpha = ((1.0f - similarity) * (1.0f - spill) * 0.6f).coerceIn(0.0f, 1.0f)
                        val spillColor = keyColor.copy(alpha = spillAlpha)

                        val innerRadius = size.height * 0.22f
                        val outerRadius = size.height * (0.22f + 0.02f + smoothness * 0.16f)

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(coreColor, spillColor, Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.5f),
                                radius = outerRadius
                            ),
                            radius = outerRadius,
                            center = Offset(size.width * 0.5f, size.height * 0.5f)
                        )

                        // Inner solid subject body
                        drawCircle(
                            color = Color(0xFF818CF8),
                            radius = innerRadius,
                            center = Offset(size.width * 0.5f, size.height * 0.5f)
                        )
                    } else {
                        // Drawing original solid backplate
                        val screenColor = Color(activeClip.chromaKeyColor)
                        drawRect(color = screenColor)

                        // Moving scanlines representing raw video feed
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f * pulse),
                            start = Offset(0f, size.height * progress),
                            end = Offset(size.width, size.height * progress),
                            strokeWidth = 3f
                        )

                        // Raw green-screen subject before keying
                        drawCircle(
                            color = Color(0xFF6366F1),
                            radius = size.height * 0.22f,
                            center = Offset(size.width * 0.5f, size.height * 0.5f)
                        )
                    }
                } else {
                    drawRect(color = Color(0xFF101114))
                }
            }
    ) {
        // Overlay visual representation of video filters / LUT effects
        if (activeFX != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when (activeFX.type) {
                            EffectType.VHS -> Color.Magenta.copy(alpha = 0.1f * activeFX.intensity)
                            EffectType.GLOW -> Color.Cyan.copy(alpha = 0.15f * activeFX.intensity)
                            EffectType.CINEMATIC -> Color(0xFF00FFCC).copy(alpha = 0.12f * activeFX.intensity)
                            EffectType.OLD_FILM -> Color(0xFFD4A373).copy(alpha = 0.2f * activeFX.intensity)
                            EffectType.RAIN -> Color.Blue.copy(alpha = 0.08f * activeFX.intensity)
                            else -> Color.Transparent
                        }
                    )
            ) {
                // Draw noise/lines for old retro filters
                if (activeFX.type == EffectType.VHS || activeFX.type == EffectType.OLD_FILM) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = if (activeFX.type == EffectType.VHS) "REC  [PLAY]" else "1920 AUTO MUTE",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
                        )
                    }
                }
            }
        }

        // Overlay Text Layers
        activeTexts.forEach { text ->
            Text(
                text = text.text,
                color = Color(text.textColor).copy(alpha = text.opacity),
                fontSize = (20 * text.scale).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }

        // Overlay Stickers
        activeStickers.forEach { sticker ->
            val isSelected = viewModel.selectedStickerId == sticker.id
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(sticker.translationX.toInt(), sticker.translationY.toInt())
                    }
                    .graphicsLayer(
                        rotationZ = sticker.rotation,
                        scaleX = sticker.scale,
                        scaleY = sticker.scale
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.selectedStickerId = sticker.id
                    }
                    .pointerInput(sticker.id) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            viewModel.updateStickerTransform(
                                stickerId = sticker.id,
                                translationX = sticker.translationX + pan.x,
                                translationY = sticker.translationY + pan.y,
                                scale = (sticker.scale * zoom).coerceIn(0.2f, 5.0f),
                                rotation = sticker.rotation + rotation
                            )
                        }
                    }
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = config.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                        } else Modifier
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = sticker.content,
                    fontSize = 44.sp,
                    modifier = Modifier.padding(4.dp)
                )

                if (isSelected) {
                    // Small delete/close button on top-right of selected sticker
                    IconButton(
                        onClick = { viewModel.deleteSticker(sticker.id) },
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Delete Sticker",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (activeClip == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.VideoFile, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Double Tap '+' to append media clips", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
            }
        }
    }
}

// --------------------------------------------------------------------------------------------------
// TIMELINE COMPONENT WITH SCROLLABLE MULTI-LAYERS
// --------------------------------------------------------------------------------------------------
@Composable
fun TimelineLayout(
    viewModel: EditorViewModel,
    state: ProjectState,
    config: com.example.ui.theme.ThemeConfig,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Video/Image file picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            val isImage = fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".png", true)
            val mediaType = if (isImage) MediaType.IMAGE else MediaType.VIDEO
            viewModel.addVideoClip(
                uri = uri.toString(),
                type = mediaType,
                name = fileName,
                durationMs = if (isImage) 5000L else 10000L // 5s for image, 10s default for video
            )
        }
    }

    // Timeline Zoom spacing parameters
    val scaleFactor = (80 * viewModel.timelineZoom).dp

    Column(
        modifier = modifier
            .background(config.background.copy(alpha = 0.9f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f))
    ) {
        // Track action toolbar (Split, Cut, Duplicate, Undo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Split Action
                IconButton(
                    onClick = {
                        viewModel.selectedClipId?.let { id ->
                            viewModel.splitClip(id, viewModel.currentSeekMs)
                        }
                    },
                    enabled = viewModel.selectedClipId != null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(config.surface)
                ) {
                    Icon(Icons.Filled.ContentCut, contentDescription = "Split", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Delete Action
                IconButton(
                    onClick = {
                        viewModel.selectedClipId?.let { viewModel.deleteClip(it) }
                        viewModel.selectedTextId?.let { viewModel.deleteTextItem(it) }
                        viewModel.selectedAudioId?.let { viewModel.deleteAudioItem(it) }
                        viewModel.selectedEffectId?.let { viewModel.deleteEffect(it) }
                    },
                    enabled = viewModel.selectedClipId != null || viewModel.selectedTextId != null || viewModel.selectedAudioId != null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(config.surface)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                }

                // Zoom Timeline Controls
                IconButton(
                    onClick = { viewModel.timelineZoom = (viewModel.timelineZoom * 1.2f).coerceIn(0.5f, 4.0f) },
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(config.surface)
                ) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { viewModel.timelineZoom = (viewModel.timelineZoom / 1.2f).coerceIn(0.5f, 4.0f) },
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(config.surface)
                ) {
                    Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // Media Add Buttons (Real File Import + Quick Demo Clip)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        try {
                            mediaPickerLauncher.launch("*/*")
                        } catch (e: Exception) {
                            // Fallback if system picker is unavailable
                            viewModel.addVideoClip("mock://clip", MediaType.VIDEO, "Demo Clip", 6000)
                        }
                    }
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = config.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import File", color = config.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(
                    onClick = {
                        viewModel.addVideoClip("mock://clip", MediaType.VIDEO, "Demo Clip", 6000)
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = config.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Demo Clip", color = config.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Layers Scroll container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            // Drawn vertical grid lines matching intervals
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(scaleFactor * (viewModel.totalDurationMs / 1000f + 2))
            ) {
                val secCount = (viewModel.totalDurationMs / 1000f).toInt()
                for (i in 0..secCount + 2) {
                    val x = (i * scaleFactor.toPx())
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                }
            }

            // Horizontal Track Rows
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Track 1: MAIN VIDEO
                TimelineRowTrack(
                    label = "Video Track",
                    config = config,
                    scaleFactor = scaleFactor
                ) {
                    // 1. Render all Clips
                    state.clips.forEach { clip ->
                        val duration = ((clip.trimEndMs - clip.trimStartMs) / clip.speed).toLong()
                        val clipWidth = ((duration / 1000f) * scaleFactor.value).dp
                        val clipOffset = ((clip.startInTimelineMs / 1000f) * scaleFactor.value).dp
                        val isSelected = viewModel.selectedClipId == clip.id

                        Box(
                            modifier = Modifier
                                .offset(x = clipOffset)
                                .width(clipWidth)
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) config.accent.copy(alpha = 0.5f) else config.surface.copy(alpha = 0.8f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) config.primary else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.selectedClipId = clip.id
                                    viewModel.selectedTextId = null
                                    viewModel.selectedAudioId = null
                                    viewModel.selectedTransitionSourceId = null
                                    viewModel.selectedTransitionTargetId = null
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (clip.type == MediaType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Photo,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = clip.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 2. Render Transition Badges/Indicators between adjacent clips
                    for (i in 0 until state.clips.size - 1) {
                        val clipA = state.clips[i]
                        val clipB = state.clips[i + 1]

                        val durationA = ((clipA.trimEndMs - clipA.trimStartMs) / clipA.speed).toLong()
                        val boundaryMs = clipA.startInTimelineMs + durationA
                        val badgeOffset = ((boundaryMs / 1000f) * scaleFactor.value).dp

                        val isBoundarySelected = viewModel.selectedTransitionSourceId == clipA.id &&
                                viewModel.selectedTransitionTargetId == clipB.id

                        // Check if a transition is applied at this boundary
                        val appliedTransition = state.transitions.firstOrNull {
                            it.sourceClipId == clipA.id && it.targetClipId == clipB.id
                        }
                        val hasTransition = appliedTransition != null && appliedTransition.type != TransitionType.NONE

                        Box(
                            modifier = Modifier
                                .offset(x = badgeOffset - 14.dp, y = 4.dp) // Offset to center over junction
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isBoundarySelected) config.primary 
                                    else if (hasTransition) config.accent 
                                    else Color.DarkGray.copy(alpha = 0.9f)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isBoundarySelected) Color.White else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    // Select this transition boundary and switch editor tab to TRANSITIONS!
                                    viewModel.selectedTransitionSourceId = clipA.id
                                    viewModel.selectedTransitionTargetId = clipB.id
                                    viewModel.selectedClipId = null
                                    viewModel.selectedTextId = null
                                    viewModel.selectedAudioId = null
                                    viewModel.activeEditorTab = "TRANSITIONS"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasTransition) Icons.Filled.SwapHoriz else Icons.Filled.Add,
                                contentDescription = "Transition",
                                tint = if (isBoundarySelected) Color.White else Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Track 2: TEXT LAYERS
                TimelineRowTrack(
                    label = "Text Layers",
                    config = config,
                    scaleFactor = scaleFactor
                ) {
                    state.textItems.forEach { text ->
                        val textWidth = ((text.durationMs / 1000f) * scaleFactor.value).dp
                        val textOffset = ((text.startInTimelineMs / 1000f) * scaleFactor.value).dp
                        val isSelected = viewModel.selectedTextId == text.id

                        Box(
                            modifier = Modifier
                                .offset(x = textOffset)
                                .width(textWidth)
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFC51162) else Color(0x80880E4F))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.selectedTextId = text.id
                                    viewModel.selectedClipId = null
                                    viewModel.selectedAudioId = null
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = text.text,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Track 3: BACKGROUND MUSIC
                TimelineRowTrack(
                    label = "Audio Mixer",
                    config = config,
                    scaleFactor = scaleFactor
                ) {
                    state.audioItems.forEach { audio ->
                        val audioWidth = ((audio.durationMs / 1000f) * scaleFactor.value).dp
                        val audioOffset = ((audio.startInTimelineMs / 1000f) * scaleFactor.value).dp
                        val isSelected = viewModel.selectedAudioId == audio.id

                        Box(
                            modifier = Modifier
                                .offset(x = audioOffset)
                                .width(audioWidth)
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF00C853) else Color(0x801B5E20))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.selectedAudioId = audio.id
                                    viewModel.selectedClipId = null
                                    viewModel.selectedTextId = null
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = audio.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // RED LINE PLAYHEAD
            val playheadOffset = ((viewModel.currentSeekMs / 1000f) * scaleFactor.value).dp
            Box(
                modifier = Modifier
                    .offset(x = playheadOffset)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.Red)
            ) {
                // Little playhead pointer bubble at the top
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .offset(x = (-4).dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
fun TimelineRowTrack(
    label: String,
    config: com.example.ui.theme.ThemeConfig,
    scaleFactor: androidx.compose.ui.unit.Dp,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.02f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Name Column
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(config.surface)
                .border(0.5.dp, Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Track layers scrollable segment
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            content = content
        )
    }
}

// --------------------------------------------------------------------------------------------------
// OPTIONS PANEL - PARAMETERS AND DIALOG CONTROLS PER TAB
// --------------------------------------------------------------------------------------------------
@Composable
fun OptionsContextPanel(
    viewModel: EditorViewModel,
    state: ProjectState,
    config: com.example.ui.theme.ThemeConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            viewModel.addAudioItem(
                uri = uri.toString(),
                name = fileName,
                durationMs = 15000L, // default 15s for imported music
                startTimelineMs = viewModel.currentSeekMs
            )
        }
    }

    val currentTime = viewModel.currentSeekMs
    val activeClip = state.clips.firstOrNull { clip ->
        currentTime >= clip.startInTimelineMs && currentTime < (clip.startInTimelineMs + clip.durationMs)
    }

    Column(
        modifier = modifier
            .background(config.surface.copy(alpha = 0.5f))
            .border(1.dp, config.glassBorder)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when (viewModel.activeEditorTab) {
            "TRIM" -> {
                Text("Video Trimming & Speeds", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                val clipId = viewModel.selectedClipId
                if (clipId != null) {
                    val clip = state.clips.firstOrNull { it.id == clipId }
                    if (clip != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Flip clip
                            GlassButton(
                                onClick = { viewModel.flipClip(clipId, horizontal = true) },
                                text = "Mirror Horiz",
                                config = config,
                                modifier = Modifier.weight(1f)
                            )
                            // Rotate clip
                            GlassButton(
                                onClick = { viewModel.rotateClip(clipId, 90f) },
                                text = "Rotate 90°",
                                config = config,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom clip speed slider representing speed curves
                        PremiumSlider(
                            value = clip.speed,
                            onValueChange = { viewModel.updateClipSpeed(clipId, it) },
                            valueRange = 0.25f..4.0f,
                            label = "Playback Speed Curve (x)",
                            config = config
                        )
                    }
                } else {
                    Text("Select a video clip in the timeline track first.", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                }
            }

            "TEXT" -> {
                Text("Rich Text Overlays", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(
                        onClick = { viewModel.addTextItem("STUNNING CINEMATIC", viewModel.currentSeekMs) },
                        text = "Add Static Text",
                        config = config,
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        onClick = { viewModel.addTextItem("SUBTITLE TRANSLATION", viewModel.currentSeekMs) },
                        text = "Add Dynamic Sub",
                        config = config,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val textId = viewModel.selectedTextId
                if (textId != null) {
                    val textItem = state.textItems.firstOrNull { it.id == textId }
                    if (textItem != null) {
                        var inputVal by remember(textItem.text) { mutableStateOf(textItem.text) }

                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = {
                                inputVal = it
                                viewModel.updateTextItem(textItem.id, it, textItem.fontName, textItem.textColor, textItem.hasShadow, textItem.scale, textItem.opacity, textItem.enterAnimation)
                            },
                            label = { Text("Edit Overlay Text", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = config.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            "TRANSITIONS" -> {
                val sourceId = viewModel.selectedTransitionSourceId
                val targetId = viewModel.selectedTransitionTargetId

                val clipA = state.clips.firstOrNull { it.id == sourceId }
                val clipB = state.clips.firstOrNull { it.id == targetId }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (clipA != null && clipB != null) {
                        Text(
                            text = "Transition: ${clipA.name} ➔ ${clipB.name}",
                            color = config.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Clear",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.selectedTransitionSourceId = null
                                    viewModel.selectedTransitionTargetId = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Select a transition boundary (⋈) in the timeline video track",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(TransitionType.values()) { type ->
                        val isCurrentType = if (clipA != null && clipB != null) {
                            state.transitions.any { it.sourceClipId == clipA.id && it.targetClipId == clipB.id && it.type == type }
                        } else {
                            if (state.clips.size >= 2) {
                                state.transitions.any { it.sourceClipId == state.clips[0].id && it.targetClipId == state.clips[1].id && it.type == type }
                            } else false
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrentType) config.primary.copy(alpha = 0.2f) else config.surface)
                                .border(
                                    width = if (isCurrentType) 2.dp else 1.dp,
                                    color = if (isCurrentType) config.primary else config.glassBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (clipA != null && clipB != null) {
                                        viewModel.setTransition(clipA.id, clipB.id, type)
                                    } else if (state.clips.size >= 2) {
                                        viewModel.setTransition(state.clips[0].id, state.clips[1].id, type)
                                        viewModel.selectedTransitionSourceId = state.clips[0].id
                                        viewModel.selectedTransitionTargetId = state.clips[1].id
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = type.displayName,
                                color = if (isCurrentType) config.primary else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            "EFFECTS" -> {
                Text("Video Effects & Shaders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(EffectType.values()) { fx ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(config.surface)
                                .border(1.dp, config.glassBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.addEffect(fx, viewModel.currentSeekMs)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(text = fx.displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "AUDIO" -> {
                Text("Background Audio & Recording", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
 
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(
                        onClick = {
                            try {
                                audioPickerLauncher.launch("audio/*")
                            } catch (e: Exception) {
                                viewModel.addAudioItem("mock://beat", "Local Track", 15000, viewModel.currentSeekMs)
                            }
                        },
                        text = "Pick Audio File",
                        config = config,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        onClick = { viewModel.addAudioItem("mock://beat", "Sunset Chill Lofi", 8000, viewModel.currentSeekMs) },
                        text = "Demo Beat",
                        config = config,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        onClick = { viewModel.addAudioItem("mock://recording", "Studio Voice Recording", 5000, viewModel.currentSeekMs, isRecording = true) },
                        text = "Record Overlay",
                        config = config,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            "COLOR_GRADING" -> {
                Text("LUT Shader Adjustments", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                PremiumSlider(
                    value = state.colorGrading.saturation,
                    onValueChange = { viewModel.updateColorGrading(state.colorGrading.copy(saturation = it)) },
                    valueRange = 0f..2f,
                    label = "Color Saturation",
                    config = config
                )

                PremiumSlider(
                    value = state.colorGrading.brightness,
                    onValueChange = { viewModel.updateColorGrading(state.colorGrading.copy(brightness = it)) },
                    valueRange = -0.5f..0.5f,
                    label = "Brightness Level",
                    config = config
                )
            }

            "STICKERS" -> {
                Text("Import Overlay Sticker Packs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                val emojis = listOf("🔥", "⚡", "❤️", "🎯", "🌟", "🎬", "🎨", "🚀", "👑")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(config.surface)
                                .border(1.dp, config.glassBorder, CircleShape)
                                .clickable {
                                    viewModel.addStickerItem(emoji, viewModel.currentSeekMs)
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                val selectedStickerId = viewModel.selectedStickerId
                if (selectedStickerId != null) {
                    val sticker = state.stickers.firstOrNull { it.id == selectedStickerId }
                    if (sticker != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Sticker: ${sticker.content}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            // Clear Selection button
                            GlassButton(
                                onClick = { viewModel.selectedStickerId = null },
                                text = "Deselect",
                                config = config,
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        PremiumSlider(
                            value = sticker.scale,
                            onValueChange = {
                                viewModel.updateStickerTransform(
                                    stickerId = sticker.id,
                                    translationX = sticker.translationX,
                                    translationY = sticker.translationY,
                                    scale = it,
                                    rotation = sticker.rotation
                                )
                            },
                            valueRange = 0.2f..4.0f,
                            label = "Sticker Size Scale",
                            config = config
                        )

                        PremiumSlider(
                            value = sticker.rotation,
                            onValueChange = {
                                viewModel.updateStickerTransform(
                                    stickerId = sticker.id,
                                    translationX = sticker.translationX,
                                    translationY = sticker.translationY,
                                    scale = sticker.scale,
                                    rotation = it
                                )
                            },
                            valueRange = -180f..180f,
                            label = "Sticker Rotation Angle",
                            config = config
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassButton(
                            onClick = { viewModel.deleteSticker(sticker.id) },
                            text = "Delete Selected Sticker",
                            config = config,
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "💡 Tip: Click on a sticker on the canvas to move, rotate, scale, or delete it directly.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            "CANVAS" -> {
                Text("Aspect Ratio Canvas background", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.example.data.model.AspectRatio.entries.forEach { ratio ->
                        val isSelected = state.project.aspectRatio == ratio
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) config.glassBg else config.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) config.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.updateCanvasSettings(ratio, state.project.canvasType, state.project.canvasColor)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = ratio.displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "CHROMA_KEY" -> {
                Text("Modern Chroma Key (Green Screen)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val clipId = viewModel.selectedClipId ?: activeClip?.id
                if (clipId != null) {
                    val clip = state.clips.firstOrNull { it.id == clipId }
                    if (clip != null) {
                        val isPremium = com.example.ui.theme.SubscriptionManager.isPremium
                        if (!isPremium) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(config.surface.copy(alpha = 0.5f))
                                    .border(1.dp, config.glassBorder, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = "Locked",
                                            tint = config.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pro Studio Feature Only",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Green Screen keying, multi-layer overlays, and 4K presets are reserved for premium members.",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    GlassButton(
                                        onClick = { com.example.ui.theme.SubscriptionManager.showPaywallDialog = true },
                                        text = "Unlock Premium PRO",
                                        config = config,
                                        isPrimary = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Layers,
                                        contentDescription = "Chroma Key",
                                        tint = config.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Enable Green Screen Compositer",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = clip.chromaKeyEnabled,
                                    onCheckedChange = { viewModel.updateChromaKeyEnabled(clip.id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = config.primary,
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                        uncheckedTrackColor = Color.Transparent
                                    )
                                )
                            }

                            if (clip.chromaKeyEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))

                                PremiumSlider(
                                    value = clip.chromaKeySimilarity,
                                    onValueChange = { viewModel.updateChromaKeySimilarity(clip.id, it)  },
                                    valueRange = 0.01f..1.0f,
                                    label = "Color Threshold similarity",
                                    config = config
                                )

                                PremiumSlider(
                                    value = clip.chromaKeySmoothness,
                                    onValueChange = { viewModel.updateChromaKeySmoothness(clip.id, it)  },
                                    valueRange = 0.0f..1.0f ,
                                    label = "Edge Softness / Smoothness",
                                    config = config
                                )

                                PremiumSlider(
                                    value = clip.chromaKeySpill,
                                    onValueChange = { viewModel.updateChromaKeySpill(clip.id, it)  },
                                    valueRange = 0.0f..1.0f ,
                                    label = "Color Spill reduction",
                                    config = config
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Select Chroma Key Background Color",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val colors = listOf(
                                    0xFF00FF00 to "Green Screen",
                                    0xFF0000FF to "Blue Screen",
                                    0xFFFF00FF to "Magenta",
                                    0xFF00E5FF to "Teal Key"
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    colors.forEach { (colorValue, name) ->
                                        val isColorSelected = clip.chromaKeyColor == colorValue
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isColorSelected) config.glassBg else config.surface)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isColorSelected) config.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    viewModel.updateChromaKeyColor(clip.id, colorValue)
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(colorValue))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = name.split(" ")[0],
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                
                            }
                        }
                    }
                } else {
                    Text("Select a video clip in the timeline track first.", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------------------
// EXPORT PRESET DIALOG WITH FFMPEG RENDERING PROGRESS
// --------------------------------------------------------------------------------------------------
@Composable
fun ExportPresetDialog(
    viewModel: EditorViewModel,
    projectName: String,
    onDismiss: () -> Unit,
    config: com.example.ui.theme.ThemeConfig
) {
    val presets = listOf(
        ExportPreset("480P", 854, 480, 1500, 24, "H.264"),
        ExportPreset("720P", 1280, 720, 3000, 30, "H.264"),
        ExportPreset("1080P", 1920, 1080, 6000, 30, "H.264"),
        ExportPreset("1440P", 2560, 1440, 12000, 60, "HEVC"),
        ExportPreset("4K", 3840, 2160, 24000, 60, "HEVC")
    )

    var selectedPreset by remember { mutableStateOf(presets[2]) }
    var exportCodec by remember { mutableStateOf("H.264") }
    var exportFps by remember { mutableStateOf(30) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            config = config,
            borderWidth = 1.5f,
            cornerRadius = 20
        ) {
            if (viewModel.exportProgress != null) {
                // Rendering Screen
                Text("NextCut Studio Render", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(72.dp).align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (viewModel.exportProgress ?: 0) / 100f },
                        color = config.primary,
                        strokeWidth = 6.dp,
                    )
                    Text(
                        text = "${viewModel.exportProgress}%",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = viewModel.exportStatusMessage,
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Do not close NextCut or switch applications.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Export Configuration Settings Screen
                Text("Configure Export Video", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                // Presets
                Text("RESOLUTION PRESET", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { preset ->
                        val isSelected = selectedPreset.resolutionName == preset.resolutionName
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) config.glassBg else config.surface)
                                .border(1.dp, if (isSelected) config.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedPreset = preset }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = preset.resolutionName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Size estimates
                val size = (viewModel.totalDurationMs / 1000f) * (selectedPreset.bitrateKbps / 8000f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESTIMATED FILE SIZE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text(String.format("%.1f MB", size), color = config.primary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ESTIMATED BITRATE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text("${selectedPreset.bitrateKbps / 1000} Mbps", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    GlassButton(
                        onClick = {
                            viewModel.triggerExport(selectedPreset, exportFps, exportCodec)
                        },
                        text = "Trigger Render",
                        config = config,
                        isPrimary = true
                    )
                }
            }
        }
    }
}

// Helper: formats milliseconds into 00:00.00 standard format
fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    val centiseconds = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
}

@Composable
fun SubscriptionPaywallDialog(
    onDismiss: () -> Unit,
    config: com.example.ui.theme.ThemeConfig
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            config = config
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Crown / Star Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(config.primary.copy(alpha = 0.15f))
                        .border(1.dp, config.primary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Premium Star",
                        tint = config.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Upgrade to Pro Studio",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Unlock professional tools & elevate your output",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature items
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PaywallFeatureItem(
                        icon = Icons.Filled.Layers,
                        title = "Modern Chroma Keying",
                        subtitle = "Professional green screen background removal",
                        config = config
                    )
                    PaywallFeatureItem(
                        icon = Icons.Filled.Hd,
                        title = "4K Extreme Resolution",
                        subtitle = "Export projects in Ultra HD with high bitrates",
                        config = config
                    )
                    PaywallFeatureItem(
                        icon = Icons.Filled.Speed,
                        title = "Vulkan GPU Rendering",
                        subtitle = "Up to 5x faster export and shader preview speed",
                        config = config
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pricing model Segmented Switch (Mock Free vs Premium selection)
                val isCurrentlyPremium = com.example.ui.theme.SubscriptionManager.isPremium
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(config.surface)
                        .border(1.dp, config.glassBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isCurrentlyPremium) config.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { com.example.ui.theme.SubscriptionManager.isPremium = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Free Model",
                            color = if (!isCurrentlyPremium) config.primary else Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrentlyPremium) config.primary.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                width = if (isCurrentlyPremium) 1.dp else 0.dp,
                                color = if (isCurrentlyPremium) config.primary.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { com.example.ui.theme.SubscriptionManager.isPremium = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Premium PRO",
                            color = if (isCurrentlyPremium) config.primary else Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Button
                GlassButton(
                    onClick = {
                        com.example.ui.theme.SubscriptionManager.isPremium = !isCurrentlyPremium
                        onDismiss()
                    },
                    text = if (isCurrentlyPremium) "Switch back to Free tier" else "Upgrade to Premium ($4.99/mo)",
                    config = config,
                    isPrimary = !isCurrentlyPremium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Cancel anytime. Terms of service apply.",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PaywallFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    config: com.example.ui.theme.ThemeConfig
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = config.accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}
