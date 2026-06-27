package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dateCreated: Long,
    val dateModified: Long,
    val aspectRatioName: String, // e.g. RATIO_16_9
    val canvasTypeName: String, // e.g. BLUR
    val canvasColor: Long,
    val isEncrypted: Boolean,
    val encryptionPassword: String,
    val thumbnailUri: String?,
    // Serialized timeline layers
    val clipsJson: String,
    val tracksJson: String,
    val transitionsJson: String,
    val effectsJson: String,
    val textItemsJson: String,
    val audioItemsJson: String,
    val stickersJson: String,
    val colorGradingJson: String
)

@Entity(tableName = "exports")
data class ExportHistoryEntity(
    @PrimaryKey val id: String,
    val projectName: String,
    val timestamp: Long,
    val status: String,
    val progress: Int,
    val presetName: String,
    val estimatedSizeMb: Float,
    val outputUri: String?
)

class DatabaseConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun stringToMediaClips(value: String): List<MediaClip> {
        val type = Types.newParameterizedType(List::class.java, MediaClip::class.java)
        val adapter = moshi.adapter<List<MediaClip>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun mediaClipsToString(clips: List<MediaClip>): String {
        val type = Types.newParameterizedType(List::class.java, MediaClip::class.java)
        val adapter = moshi.adapter<List<MediaClip>>(type)
        return adapter.toJson(clips)
    }
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY dateModified DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface ExportDao {
    @Query("SELECT * FROM exports ORDER BY timestamp DESC")
    fun getAllExports(): Flow<List<ExportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: ExportHistoryEntity)

    @Query("DELETE FROM exports WHERE id = :id")
    suspend fun deleteExportById(id: String)

    @Query("DELETE FROM exports")
    suspend fun clearExportHistory()
}

@Database(entities = [ProjectEntity::class, ExportHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun exportDao(): ExportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nextcut_studio_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
