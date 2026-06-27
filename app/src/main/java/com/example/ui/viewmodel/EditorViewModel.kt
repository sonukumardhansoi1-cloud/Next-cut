package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ProjectRepository
import com.example.data.repository.ProjectState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class EditorViewModel(private val repository: ProjectRepository) : ViewModel() {

    // Current active project state
    private val _projectState = MutableStateFlow<ProjectState?>(null)
    val projectState: StateFlow<ProjectState?> = _projectState.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<ProjectState>()
    private val redoStack = mutableListOf<ProjectState>()

    // Playback State
    var isPlaying by mutableStateOf(false)
    var currentSeekMs by mutableStateOf(0L)
    var totalDurationMs by mutableStateOf(10000L)

    // Selection & UI parameters
    var selectedClipId by mutableStateOf<String?>(null)
    var selectedTextId by mutableStateOf<String?>(null)
    var selectedAudioId by mutableStateOf<String?>(null)
    var selectedStickerId by mutableStateOf<String?>(null)
    var selectedEffectId by mutableStateOf<String?>(null)
    var selectedTransitionSourceId by mutableStateOf<String?>(null)
    var selectedTransitionTargetId by mutableStateOf<String?>(null)

    var timelineZoom by mutableStateOf(1.0f) // zoom factor
    var activeEditorTab by mutableStateOf("TRIM") // TRIM, FILTER, TEXT, AUDIO, STICKERS, CANVAS, COLOR_GRADING

    // Standard lists exposed
    val allProjects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exportHistory = repository.exportHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated background export state
    var exportProgress by mutableStateOf<Int?>(null)
    var exportStatusMessage by mutableStateOf("")

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val state = repository.getProjectState(projectId)
            if (state != null) {
                _projectState.value = state
                undoStack.clear()
                redoStack.clear()
                recalculateDuration()
            }
        }
    }

    fun createNewProject(name: String, ratio: AspectRatio = AspectRatio.RATIO_16_9) {
        viewModelScope.launch {
            val project = Project(name = name, aspectRatio = ratio)
            // Generate default tracks
            val defaultTracks = listOf(
                TimelineTrack(type = TrackType.MAIN_VIDEO, name = "Main Video", index = 0),
                TimelineTrack(type = TrackType.OVERLAY_PIP, name = "PIP Overlays", index = 1),
                TimelineTrack(type = TrackType.TEXT, name = "Text Layers", index = 2),
                TimelineTrack(type = TrackType.MUSIC, name = "Music / Audio", index = 3),
                TimelineTrack(type = TrackType.EFFECTS, name = "Effects & Filters", index = 4)
            )
            val newState = ProjectState(project = project, tracks = defaultTracks)
            _projectState.value = newState
            repository.saveProjectState(newState)
            undoStack.clear()
            redoStack.clear()
            recalculateDuration()
        }
    }

    private fun pushToUndo() {
        _projectState.value?.let { current ->
            // Clone/Snapshot current state
            undoStack.add(current.copy())
            if (undoStack.size > 20) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            _projectState.value?.let { current ->
                redoStack.add(current)
            }
            _projectState.value = previous
            saveActiveStateSilently()
            recalculateDuration()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            _projectState.value?.let { current ->
                undoStack.add(current)
            }
            _projectState.value = next
            saveActiveStateSilently()
            recalculateDuration()
        }
    }

    private fun saveActiveStateSilently() {
        val current = _projectState.value ?: return
        viewModelScope.launch {
            repository.saveProjectState(current)
        }
    }

    private fun recalculateDuration() {
        val state = _projectState.value ?: return
        var maxTime = 10000L // minimum default
        for (clip in state.clips) {
            val end = clip.startInTimelineMs + ((clip.trimEndMs - clip.trimStartMs) / clip.speed).toLong()
            if (end > maxTime) maxTime = end
        }
        for (audio in state.audioItems) {
            val end = audio.startInTimelineMs + audio.durationMs
            if (end > maxTime) maxTime = end
        }
        for (text in state.textItems) {
            val end = text.startInTimelineMs + text.durationMs
            if (end > maxTime) maxTime = end
        }
        for (sticker in state.stickers) {
            val end = sticker.startInTimelineMs + sticker.durationMs
            if (end > maxTime) maxTime = end
        }
        totalDurationMs = maxTime
    }

    // ----------------------------------------------------
    // Video/Clip Actions
    // ----------------------------------------------------
    fun addVideoClip(uri: String, type: MediaType, name: String, durationMs: Long) {
        val current = _projectState.value ?: return
        pushToUndo()

        // Calculate timeline position (append to end of existing clips)
        var nextStartMs = 0L
        for (clip in current.clips) {
            val end = clip.startInTimelineMs + ((clip.trimEndMs - clip.trimStartMs) / clip.speed).toLong()
            if (end > nextStartMs) nextStartMs = end
        }

        val newClip = MediaClip(
            uri = uri,
            type = type,
            name = name,
            durationMs = durationMs,
            startInTimelineMs = nextStartMs
        )

        val updatedClips = current.clips + newClip
        _projectState.value = current.copy(clips = updatedClips)
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun deleteClip(clipId: String) {
        val current = _projectState.value ?: return
        pushToUndo()

        val updatedClips = current.clips.filterNot { it.id == clipId }
        _projectState.value = current.copy(clips = updatedClips)
        if (selectedClipId == clipId) selectedClipId = null

        saveActiveStateSilently()
        recalculateDuration()
    }

    fun duplicateClip(clipId: String) {
        val current = _projectState.value ?: return
        val clipToCopy = current.clips.firstOrNull { it.id == clipId } ?: return
        pushToUndo()

        var nextStartMs = 0L
        for (clip in current.clips) {
            val end = clip.startInTimelineMs + ((clip.trimEndMs - clip.trimStartMs) / clip.speed).toLong()
            if (end > nextStartMs) nextStartMs = end
        }

        val duplicated = clipToCopy.copy(
            id = UUID.randomUUID().toString(),
            startInTimelineMs = nextStartMs
        )

        _projectState.value = current.copy(clips = current.clips + duplicated)
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun trimClip(clipId: String, startMs: Long, endMs: Long) {
        val current = _projectState.value ?: return
        pushToUndo()

        val updatedClips = current.clips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(trimStartMs = startMs, trimEndMs = endMs)
            } else {
                clip
            }
        }
        _projectState.value = current.copy(clips = updatedClips)
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun splitClip(clipId: String, splitAtTimelineMs: Long) {
        val current = _projectState.value ?: return
        val clip = current.clips.firstOrNull { it.id == clipId } ?: return
        pushToUndo()

        val relativeSplitMs = ((splitAtTimelineMs - clip.startInTimelineMs) * clip.speed).toLong() + clip.trimStartMs
        if (relativeSplitMs <= clip.trimStartMs || relativeSplitMs >= clip.trimEndMs) return

        // Create first half
        val clipA = clip.copy(
            id = UUID.randomUUID().toString(),
            trimEndMs = relativeSplitMs
        )

        // Create second half
        val clipB = clip.copy(
            id = UUID.randomUUID().toString(),
            trimStartMs = relativeSplitMs,
            startInTimelineMs = splitAtTimelineMs
        )

        // Filter and insert
        val updatedClips = current.clips.filterNot { it.id == clipId } + listOf(clipA, clipB)
        _projectState.value = current.copy(clips = updatedClips.sortedBy { it.startInTimelineMs })
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun rotateClip(clipId: String, angleDelta: Float) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) clip.copy(rotation = (clip.rotation + angleDelta) % 360f) else clip
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
    }

    fun flipClip(clipId: String, horizontal: Boolean) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) {
                if (horizontal) clip.copy(isFlippedHorizontal = !clip.isFlippedHorizontal)
                else clip.copy(isFlippedVertical = !clip.isFlippedVertical)
            } else {
                clip
            }
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
    }

    fun updateClipSpeed(clipId: String, newSpeed: Float) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) clip.copy(speed = newSpeed.coerceIn(0.2f, 5.0f)) else clip
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
        recalculateDuration()
    }

    // Image slideshow animation duration
    fun updateSlideDuration(clipId: String, newDurationMs: Long) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId && clip.type == MediaType.IMAGE) {
                clip.copy(durationMs = newDurationMs, trimEndMs = newDurationMs)
            } else {
                clip
            }
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun toggleKenBurns(clipId: String, enabled: Boolean) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId && clip.type == MediaType.IMAGE) clip.copy(kenBurnsEnabled = enabled) else clip
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
    }

    fun updateChromaKeyEnabled(clipId: String, enabled: Boolean) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) clip.copy(chromaKeyEnabled = enabled) else clip
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
    }

    fun updateChromaKeyColor(clipId: String, color: Long) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) clip.copy(chromaKeyColor = color) else clip
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
    }

    fun updateChromaKeySimilarity(clipId: String, similarity: Float) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) clip.copy(chromaKeySimilarity = similarity.coerceIn(0.0f, 1.0f)) else clip
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
    }

    fun updateChromaKeySmoothness(clipId: String, smoothness: Float) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) clip.copy(chromaKeySmoothness = smoothness.coerceIn(0.0f, 1.0f)) else clip
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
    }

    fun updateChromaKeySpill(clipId: String, spill: Float) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.clips.map { clip ->
            if (clip.id == clipId) clip.copy(chromaKeySpill = spill.coerceIn(0.0f, 1.0f)) else clip
        }
        _projectState.value = current.copy(clips = updated)
        saveActiveStateSilently()
    }

    // ----------------------------------------------------
    // Text Actions
    // ----------------------------------------------------
    fun addTextItem(text: String, startTimelineMs: Long) {
        val current = _projectState.value ?: return
        pushToUndo()

        val newItem = TextItem(
            text = text,
            startInTimelineMs = startTimelineMs,
            durationMs = 3000
        )
        _projectState.value = current.copy(textItems = current.textItems + newItem)
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun updateTextItem(id: String, text: String, font: String, color: Long, shadow: Boolean, size: Float, opacity: Float, anim: String) {
        val current = _projectState.value ?: return
        pushToUndo()

        val updated = current.textItems.map { item ->
            if (item.id == id) {
                item.copy(
                    text = text,
                    fontName = font,
                    textColor = color,
                    hasShadow = shadow,
                    scale = size,
                    opacity = opacity,
                    enterAnimation = anim
                )
            } else {
                item
            }
        }
        _projectState.value = current.copy(textItems = updated)
        saveActiveStateSilently()
    }

    fun deleteTextItem(id: String) {
        val current = _projectState.value ?: return
        pushToUndo()
        _projectState.value = current.copy(textItems = current.textItems.filterNot { it.id == id })
        if (selectedTextId == id) selectedTextId = null
        saveActiveStateSilently()
        recalculateDuration()
    }

    // ----------------------------------------------------
    // Audio Actions
    // ----------------------------------------------------
    fun addAudioItem(uri: String, name: String, durationMs: Long, startTimelineMs: Long, isRecording: Boolean = false) {
        val current = _projectState.value ?: return
        pushToUndo()

        val newItem = AudioItem(
            uri = uri,
            name = name,
            durationMs = durationMs,
            startInTimelineMs = startTimelineMs,
            isRecording = isRecording
        )
        _projectState.value = current.copy(audioItems = current.audioItems + newItem)
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun updateAudioItem(id: String, volume: Float, fadeInMs: Long, fadeOutMs: Long, noiseReduction: Boolean) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.audioItems.map { audio ->
            if (audio.id == id) {
                audio.copy(
                    volume = volume,
                    fadeInMs = fadeInMs,
                    fadeOutMs = fadeOutMs,
                    noiseReductionEnabled = noiseReduction
                )
            } else {
                audio
            }
        }
        _projectState.value = current.copy(audioItems = updated)
        saveActiveStateSilently()
    }

    fun deleteAudioItem(id: String) {
        val current = _projectState.value ?: return
        pushToUndo()
        _projectState.value = current.copy(audioItems = current.audioItems.filterNot { it.id == id })
        if (selectedAudioId == id) selectedAudioId = null
        saveActiveStateSilently()
        recalculateDuration()
    }

    // ----------------------------------------------------
    // Effect Actions
    // ----------------------------------------------------
    fun addEffect(type: EffectType, startMs: Long, durationMs: Long = 2000) {
        val current = _projectState.value ?: return
        pushToUndo()

        val newEffect = EffectItem(
            type = type,
            startInTimelineMs = startMs,
            durationMs = durationMs,
            trackId = current.tracks.firstOrNull { it.type == TrackType.EFFECTS }?.id ?: ""
        )
        _projectState.value = current.copy(effects = current.effects + newEffect)
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun deleteEffect(effectId: String) {
        val current = _projectState.value ?: return
        pushToUndo()
        _projectState.value = current.copy(effects = current.effects.filterNot { it.id == effectId })
        if (selectedEffectId == effectId) selectedEffectId = null
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun updateEffectIntensity(effectId: String, intensity: Float) {
        val current = _projectState.value ?: return
        pushToUndo()
        val updated = current.effects.map { fx ->
            if (fx.id == effectId) fx.copy(intensity = intensity) else fx
        }
        _projectState.value = current.copy(effects = updated)
        saveActiveStateSilently()
    }

    // ----------------------------------------------------
    // Sticker Actions
    // ----------------------------------------------------
    fun addStickerItem(content: String, startMs: Long, isAnimated: Boolean = false) {
        val current = _projectState.value ?: return
        pushToUndo()

        val newItem = StickerItem(
            content = content,
            isAnimated = isAnimated,
            startInTimelineMs = startMs
        )
        _projectState.value = current.copy(stickers = current.stickers + newItem)
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun deleteSticker(stickerId: String) {
        val current = _projectState.value ?: return
        pushToUndo()
        _projectState.value = current.copy(stickers = current.stickers.filterNot { it.id == stickerId })
        if (selectedStickerId == stickerId) selectedStickerId = null
        saveActiveStateSilently()
        recalculateDuration()
    }

    fun updateStickerTransform(
        stickerId: String,
        translationX: Float,
        translationY: Float,
        scale: Float,
        rotation: Float
    ) {
        val current = _projectState.value ?: return
        val updated = current.stickers.map { sticker ->
            if (sticker.id == stickerId) {
                sticker.copy(
                    translationX = translationX,
                    translationY = translationY,
                    scale = scale.coerceIn(0.2f, 5.0f),
                    rotation = rotation
                )
            } else sticker
        }
        _projectState.value = current.copy(stickers = updated)
        saveActiveStateSilently()
    }

    // ----------------------------------------------------
    // Aspect Ratio & Canvas theme Background
    // ----------------------------------------------------
    fun updateCanvasSettings(ratio: AspectRatio, type: CanvasType, color: Long) {
        val current = _projectState.value ?: return
        pushToUndo()

        val updatedProject = current.project.copy(
            aspectRatio = ratio,
            canvasType = type,
            canvasColor = color
        )
        _projectState.value = current.copy(project = updatedProject)
        saveActiveStateSilently()
    }

    // ----------------------------------------------------
    // Color Grading
    // ----------------------------------------------------
    fun updateColorGrading(grading: ColorGrading) {
        val current = _projectState.value ?: return
        pushToUndo()
        _projectState.value = current.copy(colorGrading = grading)
        saveActiveStateSilently()
    }

    // ----------------------------------------------------
    // Track Management Settings
    // ----------------------------------------------------
    fun toggleTrackLock(trackId: String) {
        val current = _projectState.value ?: return
        val updated = current.tracks.map { track ->
            if (track.id == trackId) track.copy(isLocked = !track.isLocked) else track
        }
        _projectState.value = current.copy(tracks = updated)
        saveActiveStateSilently()
    }

    fun toggleTrackHide(trackId: String) {
        val current = _projectState.value ?: return
        val updated = current.tracks.map { track ->
            if (track.id == trackId) track.copy(isHidden = !track.isHidden) else track
        }
        _projectState.value = current.copy(tracks = updated)
        saveActiveStateSilently()
    }

    fun renameTrack(trackId: String, newName: String) {
        val current = _projectState.value ?: return
        val updated = current.tracks.map { track ->
            if (track.id == trackId) track.copy(name = newName) else track
        }
        _projectState.value = current.copy(tracks = updated)
        saveActiveStateSilently()
    }

    // ----------------------------------------------------
    // Transitions Mapping
    // ----------------------------------------------------
    fun setTransition(srcId: String, destId: String, type: TransitionType) {
        val current = _projectState.value ?: return
        pushToUndo()

        val transition = TransitionItem(
            type = type,
            sourceClipId = srcId,
            targetClipId = destId
        )
        // filter old transition between same clips
        val filtered = current.transitions.filterNot {
            it.sourceClipId == srcId && it.targetClipId == destId
        }
        _projectState.value = current.copy(transitions = filtered + transition)
        saveActiveStateSilently()
    }

    // ----------------------------------------------------
    // Playback Navigation Helpers
    // ----------------------------------------------------
    fun stepFrame(forward: Boolean) {
        val delta = if (forward) 33L else -33L // standard 30fps frames
        currentSeekMs = (currentSeekMs + delta).coerceIn(0, totalDurationMs)
    }

    // ----------------------------------------------------
    // Project & Draft operations on Home Screen
    // ----------------------------------------------------
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            repository.duplicateProject(projectId)
        }
    }

    // ----------------------------------------------------
    // Background Rendering / FFmpeg Simulation Engine
    // ----------------------------------------------------
    fun triggerExport(preset: ExportPreset, fps: Int, codec: String) {
        val current = _projectState.value ?: return
        viewModelScope.launch {
            exportProgress = 0
            exportStatusMessage = "Initializing NextCut GPU Encoder..."
            kotlinx.coroutines.delay(1000)

            exportStatusMessage = "Assembling multi-layer tracks with FFmpeg..."
            exportProgress = 15
            kotlinx.coroutines.delay(1200)

            exportStatusMessage = "Applying filters, text transitions & audio mixer..."
            exportProgress = 45
            kotlinx.coroutines.delay(1500)

            exportStatusMessage = "Executing color grading shaders (3D LUTs)..."
            exportProgress = 70
            kotlinx.coroutines.delay(1200)

            exportStatusMessage = "Multiplexing HEVC audio & video segments..."
            exportProgress = 90
            kotlinx.coroutines.delay(800)

            exportProgress = 100
            exportStatusMessage = "Export Completed Successfully!"
            kotlinx.coroutines.delay(500)

            // Save to database export logs
            val size = (totalDurationMs / 1000f) * (preset.bitrateKbps / 8000f)
            val history = ExportHistoryItem(
                projectName = current.project.name,
                status = "COMPLETED",
                progress = 100,
                presetName = "${preset.resolutionName} (${fps}fps, ${codec})",
                estimatedSizeMb = size,
                outputUri = "file:///storage/emulated/0/DCIM/NextCut/${current.project.name}.mp4"
            )
            repository.saveExportItem(history)
            exportProgress = null
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearExportHistory()
        }
    }

    fun deleteExportItem(id: String) {
        viewModelScope.launch {
            repository.deleteExportItem(id)
        }
    }
}

class EditorViewModelFactory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
