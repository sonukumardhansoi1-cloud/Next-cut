package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalThemeConfig
import com.example.ui.theme.ThemeEngine
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val config = LocalThemeConfig.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var activeLanguage by remember { mutableStateOf("English") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var cacheSizeMb by remember { mutableStateOf(412.5f) }
    var isCleaningCache by remember { mutableStateOf(false) }

    var selectedExportRes by remember { mutableStateOf("1080P") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(config.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(config.surface.copy(alpha = 0.5f))
                        .border(1.dp, config.glassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Studio Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Subscription / Membership Segment
            Text(
                text = "MEMBERSHIP PLAN STATUS",
                color = config.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            val isPremium = com.example.ui.theme.SubscriptionManager.isPremium
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                config = config
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isPremium) config.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPremium) Icons.Filled.AutoAwesome else Icons.Filled.StarOutline,
                                contentDescription = "Pro Icon",
                                tint = if (isPremium) config.accent else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isPremium) "NextCut Studio Pro Member" else "NextCut Studio Free Tier",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isPremium) "All premium features unlocked" else "Upgrade to unlock Chroma Key & 4K",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isPremium,
                        onCheckedChange = { com.example.ui.theme.SubscriptionManager.isPremium = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = config.primary,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                            uncheckedTrackColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Selection
            Text(
                text = "THEME ENGINE presets",
                color = config.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                config = config
            ) {
                Text(
                    text = "Visual Accent Preset",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreset.values().forEach { preset ->
                        val isSelected = ThemeEngine.currentThemeState == preset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) config.glassBg else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) config.primary.copy(alpha = 0.3f) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { ThemeEngine.currentThemeState = preset }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (preset) {
                                                ThemePreset.DARK_BLACK -> Color.White
                                                ThemePreset.DARK_BLUE -> Color(0xFF00E5FF)
                                                ThemePreset.PURPLE -> Color(0xFFD500F9)
                                                ThemePreset.ORANGE -> Color(0xFFFF6D00)
                                                ThemePreset.EMERALD -> Color(0xFF00E676)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = preset.displayName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = config.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Storage and Cache Cleaner
            Text(
                text = "STORAGE & PERFORMANCE",
                color = config.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                config = config
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NextCut Cache",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Temporary clip proxy and timeline thumbnail files",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = String.format("%.1f MB", cacheSizeMb),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (cacheSizeMb > 0) 0.6f else 0f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(config.primary)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Used Space",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassButton(
                    onClick = {
                        isCleaningCache = true
                        coroutineScope.launch {
                            delay(1800)
                            cacheSizeMb = 0f
                            isCleaningCache = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = if (isCleaningCache) "Purging Cache Files..." else "Run Storage Cache Cleaner",
                    config = config,
                    enabled = cacheSizeMb > 0f && !isCleaningCache
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Export Defaults
            Text(
                text = "EXPORT PARAMETERS",
                color = config.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                config = config
            ) {
                Text(
                    text = "Default Resolution Preset",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(config.surface.copy(alpha = 0.4f))
                        .padding(4.dp)
                ) {
                    listOf("720P", "1080P", "4K").forEach { res ->
                        val isSelected = selectedExportRes == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) config.glassBg else Color.Transparent)
                                .clickable { selectedExportRes = res }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res,
                                color = if (isSelected) config.primary else Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Metadata / About
            Text(
                text = "ABOUT STUDIO",
                color = config.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                config = config
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Studio Version", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text("3.1.2-PRO", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("FFmpeg Engine", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text("v6.1.1 (Neon Hardware)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GPU Acceleration", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text("Enabled (Vulkan 1.3)", color = config.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Privacy & Terms", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text("NextCut Studio Privacy", color = config.primary, fontSize = 13.sp, modifier = Modifier.clickable { })
                }
            }
        }
    }
}
