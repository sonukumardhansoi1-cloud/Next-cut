package com.example.data.model

import java.util.UUID

enum class AspectRatio(val ratio: Float, val displayName: String, val iconName: String) {
    RATIO_16_9(16f / 9f, "16:9", "YouTube"),
    RATIO_9_16(9f / 16f, "9:16", "TikTok / Reel"),
    RATIO_1_1(1f / 1f, "1:1", "Instagram"),
    RATIO_4_5(4f / 5f, "4:5", "Portrait"),
    RATIO_21_9(21f / 9f, "21:9", "Cinematic")
}

enum class CanvasType {
    BLUR, SOLID, GRADIENT
}

enum class MediaType {
    VIDEO, IMAGE
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val aspectRatio: AspectRatio = AspectRatio.RATIO_16_9,
    val canvasType: CanvasType = CanvasType.BLUR,
    val canvasColor: Long = 0xFF121212, // Long representation of color
    val isEncrypted: Boolean = false,
    val encryptionPassword: String = "",
    val thumbnailUri: String? = null
)

data class MediaClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val type: MediaType,
    val name: String,
    val durationMs: Long,
    val originalWidth: Int = 1920,
    val originalHeight: Int = 1080,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = durationMs,
    val speed: Float = 1.0f,
    val rotation: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val volume: Float = 1.0f,
    val startInTimelineMs: Long = 0,
    // Ken Burns Animation parameters for images
    val kenBurnsEnabled: Boolean = false,
    val kenBurnsStartScale: Float = 1.0f,
    val kenBurnsEndScale: Float = 1.2f,
    val kenBurnsStartPanX: Float = 0f,
    val kenBurnsStartPanY: Float = 0f,
    val kenBurnsEndPanX: Float = 5f,
    val kenBurnsEndPanY: Float = 5f,
    val chromaKeyEnabled: Boolean = false,
    val chromaKeyColor: Long = 0xFF00FF00, // Default: Green
    val chromaKeySimilarity: Float = 0.45f,
    val chromaKeySmoothness: Float = 0.15f,
    val chromaKeySpill: Float = 0.25f
)

enum class TrackType {
    MAIN_VIDEO,
    OVERLAY_PIP,
    TEXT,
    MUSIC,
    EFFECTS
}

data class TimelineTrack(
    val id: String = UUID.randomUUID().toString(),
    val type: TrackType,
    val name: String,
    val index: Int,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false
)

enum class TransitionType(val displayName: String) {
    NONE("None"),
    FADE("Fade"),
    MIX("Mix"),
    ZOOM("Zoom"),
    PUSH("Push"),
    SLIDE("Slide"),
    SPIN("Spin"),
    GLITCH("Glitch"),
    FLASH("Flash"),
    WARP("Warp"),
    BLUR("Blur"),
    RIPPLE("Ripple"),
    CIRCLE("Circle"),
    CAMERA("Camera Roll")
}

data class TransitionItem(
    val id: String = UUID.randomUUID().toString(),
    val type: TransitionType,
    val durationMs: Long = 500,
    val sourceClipId: String,
    val targetClipId: String
)

enum class EffectType(val displayName: String, val category: String) {
    CINEMATIC("Teal & Orange", "Color"),
    RETRO("Retro 70s", "Classic"),
    VHS("VHS Glitch", "Glitch"),
    FILM("8mm Projector", "Classic"),
    DREAM("Dreamy Soft", "Atmosphere"),
    GLOW("Neon Glow", "Atmosphere"),
    RGB("RGB Split", "Glitch"),
    NEON("Neon Outline", "Artistic"),
    SHAKE("Camera Shake", "Action"),
    CHROMATIC("Chromatic aberration", "Glitch"),
    GLITCH("Old School Glitch", "Glitch"),
    OLD_FILM("Old Movie 1920", "Classic"),
    MOTION("Motion Blur", "Action"),
    RAIN("Rain overlay", "Particles"),
    SNOW("Snow particles", "Particles"),
    LIGHT_LEAK("Light Leak Flare", "Atmosphere"),
    FIRE("Fire overlay", "Particles"),
    SMOKE("Mystic Smoke", "Particles")
}

data class EffectItem(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType,
    val intensity: Float = 0.8f,
    val startInTimelineMs: Long,
    val durationMs: Long = 2000,
    val trackId: String
)

data class TextItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Double Tap to Edit",
    val fontName: String = "Roboto",
    val textColor: Long = 0xFFFFFFFF,
    val hasGradient: Boolean = false,
    val gradientColorStart: Long = 0xFFEA4335,
    val gradientColorEnd: Long = 0xFFFBBC05,
    val strokeColor: Long = 0xFF000000,
    val strokeWidth: Float = 2f,
    val hasShadow: Boolean = false,
    val shadowColor: Long = 0x80000000,
    val glowColor: Long = 0x00000000,
    val opacity: Float = 1.0f,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.2f,
    val startInTimelineMs: Long,
    val durationMs: Long = 3000,
    val scale: Float = 1.0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val rotation: Float = 0f,
    val enterAnimation: String = "None", // Fade, Typewriter, Slide, Bounce
    val exitAnimation: String = "None"
)

data class AudioItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val name: String,
    val durationMs: Long,
    val startInTimelineMs: Long,
    val volume: Float = 1.0f,
    val fadeInMs: Long = 200,
    val fadeOutMs: Long = 200,
    val isMuted: Boolean = false,
    val noiseReductionEnabled: Boolean = false,
    val isRecording: Boolean = false
)

data class StickerItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String, // Can be emoji or res/drawable identifier
    val isAnimated: Boolean = false,
    val startInTimelineMs: Long,
    val durationMs: Long = 3000,
    val scale: Float = 1.0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val rotation: Float = 0f,
    val packName: String = "Classic Emoji"
)

data class ColorGrading(
    val brightness: Float = 0f, // -1f to 1f
    val contrast: Float = 1f,   // 0.5f to 1.5f
    val exposure: Float = 0f,   // -1f to 1f
    val saturation: Float = 1f, // 0f to 2f
    val temperature: Float = 0f,// -1f to 1f
    val tint: Float = 0f,       // -1f to 1f
    val highlights: Float = 0f,  // -1f to 1f
    val shadows: Float = 0f     // -1f to 1f
)

data class ExportPreset(
    val resolutionName: String, // 480P, 720P, 1080P, 1440P, 4K
    val width: Int,
    val height: Int,
    val bitrateKbps: Int,
    val fps: Int = 30,
    val codec: String = "H.264" // H.264, HEVC
)

data class ExportHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val projectName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // COMPLETED, FAILED, RUNNING
    val progress: Int, // 0 to 100
    val presetName: String,
    val estimatedSizeMb: Float,
    val outputUri: String? = null
)
