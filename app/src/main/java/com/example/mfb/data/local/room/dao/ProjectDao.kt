package com.example.mfb.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mfb.data.local.room.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE projectUuid = :uuid")
    suspend fun getProjectByUuid(uuid: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectName(id: Long, name: String, updatedAt: Long)

    @Query("UPDATE projects SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE projects SET lastOpenedAt = :lastOpenedAt WHERE id = :id")
    suspend fun updateLastOpened(id: Long, lastOpenedAt: Long)

    @Query("UPDATE projects SET name = :name, description = :description, tags = :tags, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectDetails(id: Long, name: String, description: String, tags: String, updatedAt: Long)

    @Query("UPDATE projects SET fileCount = :fileCount, totalSizeBytes = :totalSizeBytes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectStats(id: Long, fileCount: Int, totalSizeBytes: Long, updatedAt: Long)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}
