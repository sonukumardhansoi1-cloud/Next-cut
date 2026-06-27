package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.ThemeConfig
import com.example.ui.theme.glassmorphism

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    config: ThemeConfig,
    borderWidth: Float = 1f,
    cornerRadius: Int = 16,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .glassmorphism(cornerRadius, borderWidth, config)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    config: ThemeConfig,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    val alpha = if (enabled) 1.0f else 0.4f
    val bgBrush = if (isPrimary) {
        Brush.horizontalGradient(
            colors = listOf(config.primary, config.accent)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(config.glassBg, config.surface.copy(alpha = 0.5f))
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (isPrimary) Color.White.copy(alpha = 0.2f) else config.glassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPrimary && config.primary != Color(0xFF6366F1)) config.background else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun PremiumSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    label: String,
    config: ThemeConfig,
    modifier: Modifier = Modifier,
    displayValue: String = String.format("%.2f", value)
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = displayValue,
                color = config.primary,
                fontSize = 13.sp,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                activeTrackColor = config.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                thumbColor = config.accent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PremiumSegmentedControl(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    config: ThemeConfig,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(config.surface.copy(alpha = 0.6f))
            .border(1.dp, config.glassBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val alpha by animateFloatAsState(targetValue = if (isSelected) 1.0f else 0.5f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) config.glassBg else Color.Transparent)
                    .clickable { onItemSelected(item) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    color = Color.White.copy(alpha = alpha),
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}
