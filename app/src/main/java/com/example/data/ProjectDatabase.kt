package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val prompt: String,
    val status: String, // "Planning", "Running", "Completed", "Failed"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val senderName: String,
    val senderRole: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val title: String,
    val assignee: String,
    val status: String, // "Pending", "Running", "Completed"
    val orderIndex: Int
)

@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val motto: String,
    val avatarChar: String,
    val baseColorHex: String,
    val personalizedApiKey: String = ""
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): ProjectEntity?

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    // Messages
    @Query("SELECT * FROM messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesForProjectFlow(projectId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE projectId = :projectId")
    suspend fun deleteMessagesForProject(projectId: Int)

    // Tasks
    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getTasksForProjectFlow(projectId: Int): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteTasksForProject(projectId: Int)

    // Config Key-Value
    @Query("SELECT * FROM config WHERE `key` = :key")
    suspend fun getConfigByKey(key: String): ConfigEntity?

    @Query("SELECT * FROM config")
    fun getAllConfigFlow(): Flow<List<ConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity)

    // Team Members
    @Query("SELECT * FROM team_members")
    fun getAllTeamMembersFlow(): Flow<List<TeamMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMember(member: TeamMemberEntity)

    @Query("DELETE FROM team_members WHERE id = :id")
    suspend fun deleteTeamMemberById(id: Int)
}

@Database(
    entities = [
        ProjectEntity::class,
        MessageEntity::class,
        TaskEntity::class,
        ConfigEntity::class,
        TeamMemberEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ProjectDatabase : RoomDatabase() {
    abstract val projectDao: ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: ProjectDatabase? = null

        fun getDatabase(context: android.content.Context): ProjectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProjectDatabase::class.java,
                    "project_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
