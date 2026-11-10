package com.example.mfb.domain.repository

import com.example.mfb.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    suspend fun getProjectById(id: Long): Project?
    suspend fun getProjectByUuid(uuid: String): Project?
    suspend fun insertProject(project: Project): Long
    suspend fun updateProject(project: Project)
    suspend fun updateProjectName(id: Long, name: String)
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun recordProjectOpened(id: Long)
    suspend fun updateProjectMetadata(id: Long, name: String, description: String, tags: List<String>)
    suspend fun updateProjectStats(id: Long, fileCount: Int, totalSizeBytes: Long)
    suspend fun deleteProject(project: Project)
    suspend fun deleteProjectById(id: Long)
    suspend fun getProjectsCount(): Int
}
