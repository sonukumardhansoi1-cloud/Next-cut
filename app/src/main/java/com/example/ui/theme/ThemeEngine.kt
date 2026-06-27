package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemePreset(val displayName: String) {
    DARK_BLACK("Sleek Interface"),
    DARK_BLUE("Nordic Sapphire"),
    PURPLE("Aura Purple"),
    ORANGE("Sunset Amber"),
    EMERALD("Mint Emerald")
}

data class ThemeConfig(
    val preset: ThemePreset,
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val surfaceVariant: Color = surface.copy(alpha = 0.7f),
    val glassBg: Color = surface.copy(alpha = 0.4f),
    val glassBorder: Color = Color.White.copy(alpha = 0.08f)
)

object ThemeEngine {
    private val oledBlack = ThemeConfig(
        preset = ThemePreset.DARK_BLACK,
        background = Color(0xFF050505),
        surface = Color(0xFF0F0F0F),
        primary = Color(0xFF6366F1), // Indigo 600/500 style
        secondary = Color(0xFF94A3B8), // Slate Gray
        accent = Color(0xFF6366F1) // Indigo Accent
    )

    private val nordicSapphire = ThemeConfig(
        preset = ThemePreset.DARK_BLUE,
        background = Color(0xFF0A0F1D),
        surface = Color(0xFF141B2E),
        primary = Color(0xFF00E5FF), // Electric Cyan
        secondary = Color(0xFF536DFE),
        accent = Color(0xFF2979FF)
    )

    private val auraPurple = ThemeConfig(
        preset = ThemePreset.PURPLE,
        background = Color(0xFF0E071A),
        surface = Color(0xFF18102A),
        primary = Color(0xFFD500F9), // Vivid Neon Purple
        secondary = Color(0xFF90CAFF),
        accent = Color(0xFFE040FB)
    )

    private val sunsetAmber = ThemeConfig(
        preset = ThemePreset.ORANGE,
        background = Color(0xFF0F0B08),
        surface = Color(0xFF1B1410),
        primary = Color(0xFFFF6D00), // Hot Orange
        secondary = Color(0xFFFFD600),
        accent = Color(0xFFFFAB40)
    )

    private val mintEmerald = ThemeConfig(
        preset = ThemePreset.EMERALD,
        background = Color(0xFF07100D),
        surface = Color(0xFF101F1A),
        primary = Color(0xFF00E676), // Vivid Mint
        secondary = Color(0xFFB9F6CA),
        accent = Color(0xFF00B0FF)
    )

    // Observable current state
    var currentThemeState by mutableStateOf(ThemePreset.DARK_BLACK)

    fun getThemeConfig(preset: ThemePreset = currentThemeState): ThemeConfig {
        return when (preset) {
            ThemePreset.DARK_BLACK -> oledBlack
            ThemePreset.DARK_BLUE -> nordicSapphire
            ThemePreset.PURPLE -> auraPurple
            ThemePreset.ORANGE -> sunsetAmber
            ThemePreset.EMERALD -> mintEmerald
        }
    }

    fun makeColorScheme(config: ThemeConfig): ColorScheme {
        return darkColorScheme(
            background = config.background,
            onBackground = Color.White,
            surface = config.surface,
            onSurface = Color.White,
            primary = config.primary,
            onPrimary = config.background,
            secondary = config.secondary,
            onSecondary = Color.White,
            tertiary = config.accent,
            onTertiary = Color.White,
            surfaceVariant = config.surfaceVariant,
            onSurfaceVariant = Color.White.copy(alpha = 0.8f)
        )
    }
}

// Composition local to access the ThemeConfig easily
val LocalThemeConfig = staticCompositionLocalOf { ThemeEngine.getThemeConfig(ThemePreset.DARK_BLACK) }

object SubscriptionManager {
    var isPremium by mutableStateOf(true)
    var showPaywallDialog by mutableStateOf(false)
}

object AuthManager {
    var isLoggedIn by mutableStateOf(false)
    var currentUserEmail by mutableStateOf("")
    var currentUserName by mutableStateOf("")

    // Simulated local user database for Signup and Sign-In validation
    private val registeredUsers = mutableMapOf<String, Pair<String, String>>(
        "editor@nextcut.com" to ("Studio Editor" to "password123")
    )

    fun signUp(name: String, email: String, pass: String): Boolean {
        if (email.isBlank() || pass.isBlank() || name.isBlank()) return false
        if (registeredUsers.containsKey(email)) return false
        registeredUsers[email] = name to pass
        isLoggedIn = true
        currentUserEmail = email
        currentUserName = name
        return true
    }

    fun signIn(email: String, pass: String): Boolean {
        val user = registeredUsers[email]
        if (user != null && user.second == pass) {
            isLoggedIn = true
            currentUserEmail = email
            currentUserName = user.first
            return true
        }
        return false
    }

    fun logOut() {
        isLoggedIn = false
        currentUserEmail = ""
        currentUserName = ""
    }
}

// Premium Glassmorphism Modifier
fun Modifier.glassmorphism(
    cornerRadius: Int = 16,
    borderWidth: Float = 1f,
    config: ThemeConfig
): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    return this
        .clip(shape)
        .background(config.glassBg)
        .border(
            width = borderWidth.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.02f)
                )
            ),
            shape = shape
        )
}
