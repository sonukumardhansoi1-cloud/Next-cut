package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class ProjectState(
    val project: Project,
    val clips: List<MediaClip> = emptyList(),
    val tracks: List<TimelineTrack> = emptyList(),
    val transitions: List<TransitionItem> = emptyList(),
    val effects: List<EffectItem> = emptyList(),
    val textItems: List<TextItem> = emptyList(),
    val audioItems: List<AudioItem> = emptyList(),
    val stickers: List<StickerItem> = emptyList(),
    val colorGrading: ColorGrading = ColorGrading()
)

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val exportDao: ExportDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // Adapters for serialization
    private val clipsAdapter = moshi.adapter<List<MediaClip>>(Types.newParameterizedType(List::class.java, MediaClip::class.java))
    private val tracksAdapter = moshi.adapter<List<TimelineTrack>>(Types.newParameterizedType(List::class.java, TimelineTrack::class.java))
    private val transitionsAdapter = moshi.adapter<List<TransitionItem>>(Types.newParameterizedType(List::class.java, TransitionItem::class.java))
    private val effectsAdapter = moshi.adapter<List<EffectItem>>(Types.newParameterizedType(List::class.java, EffectItem::class.java))
    private val textsAdapter = moshi.adapter<List<TextItem>>(Types.newParameterizedType(List::class.java, TextItem::class.java))
    private val audiosAdapter = moshi.adapter<List<AudioItem>>(Types.newParameterizedType(List::class.java, AudioItem::class.java))
    private val stickersAdapter = moshi.adapter<List<StickerItem>>(Types.newParameterizedType(List::class.java, StickerItem::class.java))
    private val gradingAdapter = moshi.adapter(ColorGrading::class.java)

    // Flow of all projects
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects().map { entities ->
        entities.map { entity ->
            Project(
                id = entity.id,
                name = entity.name,
                dateCreated = entity.dateCreated,
                dateModified = entity.dateModified,
                aspectRatio = AspectRatio.values().firstOrNull { it.name == entity.aspectRatioName } ?: AspectRatio.RATIO_16_9,
                canvasType = CanvasType.valueOf(entity.canvasTypeName),
                canvasColor = entity.canvasColor,
                isEncrypted = entity.isEncrypted,
                encryptionPassword = entity.encryptionPassword,
                thumbnailUri = entity.thumbnailUri
            )
        }
    }

    // Flow of export history
    val exportHistory: Flow<List<ExportHistoryItem>> = exportDao.getAllExports().map { entities ->
        entities.map { entity ->
            ExportHistoryItem(
                id = entity.id,
                projectName = entity.projectName,
                timestamp = entity.timestamp,
                status = entity.status,
                progress = entity.progress,
                presetName = entity.presetName,
                estimatedSizeMb = entity.estimatedSizeMb,
                outputUri = entity.outputUri
            )
        }
    }

    suspend fun getProjectState(projectId: String): ProjectState? {
        val entity = projectDao.getProjectById(projectId) ?: return null
        val project = Project(
            id = entity.id,
            name = entity.name,
            dateCreated = entity.dateCreated,
            dateModified = entity.dateModified,
            aspectRatio = AspectRatio.values().firstOrNull { it.name == entity.aspectRatioName } ?: AspectRatio.RATIO_16_9,
            canvasType = CanvasType.valueOf(entity.canvasTypeName),
            canvasColor = entity.canvasColor,
            isEncrypted = entity.isEncrypted,
            encryptionPassword = entity.encryptionPassword,
            thumbnailUri = entity.thumbnailUri
        )

        val clips = clipsAdapter.fromJson(entity.clipsJson) ?: emptyList()
        val tracks = tracksAdapter.fromJson(entity.tracksJson) ?: emptyList()
        val transitions = transitionsAdapter.fromJson(entity.transitionsJson) ?: emptyList()
        val effects = effectsAdapter.fromJson(entity.effectsJson) ?: emptyList()
        val texts = textsAdapter.fromJson(entity.textItemsJson) ?: emptyList()
        val audios = audiosAdapter.fromJson(entity.audioItemsJson) ?: emptyList()
        val stickers = stickersAdapter.fromJson(entity.stickersJson) ?: emptyList()
        val colorGrading = gradingAdapter.fromJson(entity.colorGradingJson) ?: ColorGrading()

        return ProjectState(
            project = project,
            clips = clips,
            tracks = tracks,
            transitions = transitions,
            effects = effects,
            textItems = texts,
            audioItems = audios,
            stickers = stickers,
            colorGrading = colorGrading
        )
    }

    suspend fun saveProjectState(state: ProjectState) {
        val entity = ProjectEntity(
            id = state.project.id,
            name = state.project.name,
            dateCreated = state.project.dateCreated,
            dateModified = System.currentTimeMillis(),
            aspectRatioName = state.project.aspectRatio.name,
            canvasTypeName = state.project.canvasType.name,
            canvasColor = state.project.canvasColor,
            isEncrypted = state.project.isEncrypted,
            encryptionPassword = state.project.encryptionPassword,
            thumbnailUri = state.project.thumbnailUri,
            clipsJson = clipsAdapter.toJson(state.clips),
            tracksJson = tracksAdapter.toJson(state.tracks),
            transitionsJson = transitionsAdapter.toJson(state.transitions),
            effectsJson = effectsAdapter.toJson(state.effects),
            textItemsJson = textsAdapter.toJson(state.textItems),
            audioItemsJson = audiosAdapter.toJson(state.audioItems),
            stickersJson = stickersAdapter.toJson(state.stickers),
            colorGradingJson = gradingAdapter.toJson(state.colorGrading)
        )
        projectDao.insertProject(entity)
    }

    suspend fun deleteProject(projectId: String) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun duplicateProject(projectId: String) {
        val existing = getProjectState(projectId) ?: return
        val duplicatedId = UUID.randomUUID().toString()
        val duplicatedProject = existing.project.copy(
            id = duplicatedId,
            name = "${existing.project.name} (Copy)",
            dateCreated = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )
        saveProjectState(existing.copy(project = duplicatedProject))
    }

    suspend fun saveExportItem(item: ExportHistoryItem) {
        val entity = ExportHistoryEntity(
            id = item.id,
            projectName = item.projectName,
            timestamp = item.timestamp,
            status = item.status,
            progress = item.progress,
            presetName = item.presetName,
            estimatedSizeMb = item.estimatedSizeMb,
            outputUri = item.outputUri
        )
        exportDao.insertExport(entity)
    }

    suspend fun deleteExportItem(id: String) {
        exportDao.deleteExportById(id)
    }

    suspend fun clearExportHistory() {
        exportDao.clearExportHistory()
    }
}
