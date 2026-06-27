package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.data.model.AspectRatio
import androidx.compose.foundation.border
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.AuthDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.theme.LocalThemeConfig
import com.example.ui.theme.ThemeEngine
import com.example.ui.theme.ThemeConfig
import com.example.ui.viewmodel.EditorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: EditorViewModel,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val config = LocalThemeConfig.current
    val projects by viewModel.allProjects.collectAsState()
    val exports by viewModel.exportHistory.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf(AspectRatio.RATIO_16_9) }

    var searchQuery by remember { mutableStateOf("") }
    var activeHomeTab by remember { mutableStateOf("PROJECTS") } // PROJECTS, TEMPLATES, EXPORTS
    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Filter projects
    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects else {
            projects.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(config.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NextCut",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        text = "STUDIO ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = config.accent,
                        letterSpacing = 2.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLoggedIn = com.example.ui.theme.AuthManager.isLoggedIn
                    IconButton(
                        onClick = {
                            if (isLoggedIn) {
                                showProfileDialog = true
                            } else {
                                showAuthDialog = true
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(config.surface.copy(alpha = 0.5f))
                            .border(1.dp, config.glassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isLoggedIn) Icons.Filled.AccountCircle else Icons.Filled.Login,
                            contentDescription = if (isLoggedIn) "User Profile" else "Sign In",
                            tint = if (isLoggedIn) config.accent else Color.White
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(config.surface.copy(alpha = 0.5f))
                            .border(1.dp, config.glassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Open Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search projects, drafts, files...", color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon", tint = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, config.glassBorder, RoundedCornerShape(14.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = config.surface.copy(alpha = 0.6f),
                    unfocusedContainerColor = config.surface.copy(alpha = 0.4f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Create Panel (Glassmorphism)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                config = config
            ) {
                Text(
                    text = "Start Editing",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        onClick = {
                            newProjectName = "NextCut Project ${projects.size + 1}"
                            showCreateDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        text = "New Project",
                        config = config,
                        isPrimary = true
                    )

                    // Template quick launcher
                    GlassButton(
                        onClick = {
                            // Instant Project
                            val name = "Aesthetic Template ${projects.size + 1}"
                            viewModel.createNewProject(name)
                            viewModel.addVideoClip("mock://video/ocean_loop", MediaType.VIDEO, "Ocean Ripple Loop", 8000)
                            viewModel.addAudioItem("mock://music/ambient_lofi", "Lofi Sunset Chill", 12000, 0)
                            viewModel.addTextItem("STUNNING CINEMATIC", 1000)
                            viewModel.addEffect(EffectType.CINEMATIC, 0, 4000)
                            // Navigate to active project after creation
                            viewModel.projectState.value?.project?.id?.let { onNavigateToEditor(it) }
                        },
                        modifier = Modifier.weight(1f),
                        text = "Use Template",
                        config = config
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Segment Tabs (Projects, Templates, Exports)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(config.surface.copy(alpha = 0.4f))
                    .border(1.dp, config.glassBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                listOf(
                    "PROJECTS" to "Drafts",
                    "TEMPLATES" to "Templates",
                    "EXPORTS" to "Exports"
                ).forEach { (tab, label) ->
                    val isSelected = activeHomeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) config.glassBg else Color.Transparent)
                            .clickable { activeHomeTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) config.primary else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                when (activeHomeTab) {
                    "PROJECTS" -> {
                        if (filteredProjects.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.VideoFile,
                                title = "No Projects Found",
                                description = "Tap 'New Project' above to craft your first visual masterpiece.",
                                config = config
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredProjects) { project ->
                                    ProjectRowItem(
                                        project = project,
                                        config = config,
                                        onOpen = { onNavigateToEditor(project.id) },
                                        onDelete = { viewModel.deleteProject(project.id) },
                                        onDuplicate = { viewModel.duplicateProject(project.id) }
                                    )
                                }
                            }
                        }
                    }

                    "TEMPLATES" -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val templatesList = listOf(
                                Triple("Cyberpunk Neon Loop", "Glitch transitions + VHS aesthetic", EffectType.VHS),
                                Triple("Sunset Silhouette", "Ken Burns image motion + soft dream", EffectType.DREAM),
                                Triple("Hyperlapse Cityscape", "4x curve speed + color grading", EffectType.CINEMATIC),
                                Triple("Old Retro Projector", "Black & white grain + old film", EffectType.OLD_FILM),
                                Triple("Vaporwave Glow", "RGB split transitions + heavy saturation", EffectType.RGB),
                                Triple("Mint Rain Loop", "Rain overlays + soft glow shader", EffectType.RAIN)
                            )
                            items(templatesList) { (title, subtitle, fx) ->
                                TemplateCard(
                                    title = title,
                                    subtitle = subtitle,
                                    config = config,
                                    onClick = {
                                        viewModel.createNewProject("Template - $title")
                                        viewModel.addVideoClip("mock://video/template_clip", MediaType.VIDEO, "Stock Fragment", 10000)
                                        viewModel.addEffect(fx, 1000, 3000)
                                        viewModel.addTextItem(title.uppercase(), 1000)
                                        viewModel.addAudioItem("mock://audio/beat", "SynthWave Waveform", 15000, 0)
                                        viewModel.projectState.value?.project?.id?.let { onNavigateToEditor(it) }
                                    }
                                )
                            }
                        }
                    }

                    "EXPORTS" -> {
                        if (exports.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.CheckCircle,
                                title = "No Recent Exports",
                                description = "Configure and render projects in the editor to view them here.",
                                config = config
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Text("Clear Logs", color = config.accent, fontSize = 12.sp)
                                    }
                                }
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(exports) { item ->
                                        ExportRowItem(item = item, config = config, onDelete = { viewModel.deleteExportItem(item.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Project Creator Dialog
        if (showCreateDialog) {
            Dialog(onDismissRequest = { showCreateDialog = false }) {
                GlassCard(
                    config = config,
                    borderWidth = 1.5f,
                    cornerRadius = 20
                ) {
                    Text(
                        text = "Create New Studio Project",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project Name", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = config.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = config.primary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Aspect Ratio Format",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AspectRatioSelectorGrid(
                        selected = selectedRatio,
                        onSelect = { selectedRatio = it },
                        config = config
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        GlassButton(
                            onClick = {
                                showCreateDialog = false
                                viewModel.createNewProject(newProjectName, selectedRatio)
                                // Standard initial timeline trigger delay to let DB write complete
                            },
                            text = "Launch Editor",
                            config = config,
                            isPrimary = true
                        )
                    }
                }
            }
        }

        // Listener to redirect upon active project initialization
        val activeProjState by viewModel.projectState.collectAsState()
        LaunchedEffect(activeProjState) {
            val projId = activeProjState?.project?.id
            if (projId != null && activeHomeTab == "PROJECTS") {
                onNavigateToEditor(projId)
            }
        }

        if (showAuthDialog) {
            AuthDialog(
                onDismiss = { showAuthDialog = false },
                config = config
            )
        }

        if (showProfileDialog) {
            ProfileDialog(
                onDismiss = { showProfileDialog = false },
                config = config
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    config: ThemeConfig
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(config.surface.copy(alpha = 0.5f))
                .border(1.dp, config.glassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = config.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 250.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectRowItem(
    project: Project,
    config: ThemeConfig,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("MMM d, yyyy · hh:mm a", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(config.surface.copy(alpha = 0.4f))
            .border(1.dp, config.glassBorder, RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { showMenu = true }
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aspect ratio display card
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (project.aspectRatio) {
                        AspectRatio.RATIO_16_9 -> Icons.Filled.Tv
                        AspectRatio.RATIO_9_16 -> Icons.Filled.Smartphone
                        AspectRatio.RATIO_1_1 -> Icons.Filled.CropSquare
                        AspectRatio.RATIO_4_5 -> Icons.Filled.Portrait
                        AspectRatio.RATIO_21_9 -> Icons.Filled.Panorama
                    },
                    contentDescription = null,
                    tint = config.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.aspectRatio.displayName,
                        color = config.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatter.format(Date(project.dateModified)),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu Options", tint = Color.White.copy(alpha = 0.5f))
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(config.surface)
        ) {
            DropdownMenuItem(
                text = { Text("Open in Timeline") },
                onClick = { showMenu = false; onOpen() },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = config.primary) }
            )
            DropdownMenuItem(
                text = { Text("Duplicate Draft") },
                onClick = { showMenu = false; onDuplicate() },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.LightGray) }
            )
            DropdownMenuItem(
                text = { Text("Delete Project") },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red) }
            )
        }
    }
}

@Composable
fun TemplateCard(
    title: String,
    subtitle: String,
    config: ThemeConfig,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(config.surface, config.background)
                )
            )
            .border(1.dp, config.glassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(config.accent.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(text = "CREATIVE", color = config.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ExportRowItem(
    item: ExportHistoryItem,
    config: ThemeConfig,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(config.surface.copy(alpha = 0.3f))
            .border(1.dp, config.glassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(config.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.VideoSettings, contentDescription = null, tint = config.accent, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = item.projectName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(text = item.presetName, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "•", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = String.format("%.1f MB", item.estimatedSizeMb), color = config.primary, fontSize = 11.sp)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatter.format(Date(item.timestamp)),
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete export", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AspectRatioSelectorGrid(
    selected: AspectRatio,
    onSelect: (AspectRatio) -> Unit,
    config: ThemeConfig
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AspectRatio.values().forEach { ratio ->
            val isSelected = selected == ratio
            val borderBrush = if (isSelected) {
                Brush.horizontalGradient(listOf(config.primary, config.accent))
            } else {
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f)))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) config.glassBg else config.surface.copy(alpha = 0.3f))
                    .border(1.dp, borderBrush, RoundedCornerShape(10.dp))
                    .clickable { onSelect(ratio) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when (ratio) {
                        AspectRatio.RATIO_16_9 -> Icons.Filled.Tv
                        AspectRatio.RATIO_9_16 -> Icons.Filled.Smartphone
                        AspectRatio.RATIO_1_1 -> Icons.Filled.CropSquare
                        AspectRatio.RATIO_4_5 -> Icons.Filled.Portrait
                        AspectRatio.RATIO_21_9 -> Icons.Filled.Panorama
                    },
                    contentDescription = null,
                    tint = if (isSelected) config.primary else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ratio.displayName,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ratio.iconName,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}
