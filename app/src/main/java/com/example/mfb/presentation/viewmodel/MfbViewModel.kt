package com.example.mfb.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mfb.core.di.AppContainer
import com.example.mfb.core.model.ThemeMode
import com.example.mfb.data.engine.HtmlImportEngine
import com.example.mfb.data.engine.HtmlImportResult
import com.example.mfb.data.engine.ProjectStorageManager
import com.example.mfb.data.engine.ZipImportEngine
import com.example.mfb.data.engine.ZipImportResult
import com.example.mfb.data.local.datastore.UserPreferencesRepository
import com.example.mfb.domain.model.BuildRecord
import com.example.mfb.domain.model.Project
import com.example.mfb.domain.repository.BuildRepository
import com.example.mfb.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ImportUiState {
    object Idle : ImportUiState()
    data class Progress(val progress: Float, val statusMessage: String) : ImportUiState()
    data class NeedsEntryPointChoice(
        val tempDirName: String,
        val candidates: List<String>,
        val originalFileName: String,
        val suggestedName: String
    ) : ImportUiState()
    data class Success(val project: Project, val warnings: List<String> = emptyList()) : ImportUiState()
    data class Error(val errorMessage: String) : ImportUiState()
}

class MfbViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val projectRepository: ProjectRepository,
    private val buildRepository: BuildRepository,
    private val appContext: Context? = null
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.DARK
        )

    val appLanguage: StateFlow<String> = preferencesRepository.appLanguageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )

    val projects: StateFlow<List<Project>> = projectRepository.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentBuilds: StateFlow<List<BuildRecord>> = buildRepository.getRecentBuilds(5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _importUiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importUiState: StateFlow<ImportUiState> = _importUiState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setAppLanguage(languageCode: String) {
        viewModelScope.launch {
            preferencesRepository.setAppLanguage(languageCode)
        }
    }

    fun resetImportState() {
        _importUiState.value = ImportUiState.Idle
    }

    fun importZip(context: Context, uri: Uri, customProjectName: String? = null) {
        viewModelScope.launch {
            _importUiState.value = ImportUiState.Progress(0.05f, "Starting archive extraction...")

            val result = ZipImportEngine.importFromUri(
                context = context,
                uri = uri,
                customProjectName = customProjectName,
                onProgress = { progress, message ->
                    _importUiState.value = ImportUiState.Progress(progress, message)
                }
            )

            when (result) {
                is ZipImportResult.Success -> {
                    val id = projectRepository.insertProject(result.project)
                    val saved = result.project.copy(id = id)
                    _importUiState.value = ImportUiState.Success(saved, emptyList())
                }

                is ZipImportResult.NeedsEntryPointChoice -> {
                    _importUiState.value = ImportUiState.NeedsEntryPointChoice(
                        tempDirName = result.tempDirName,
                        candidates = result.candidates,
                        originalFileName = result.originalFileName,
                        suggestedName = result.suggestedName
                    )
                }

                is ZipImportResult.Error -> {
                    _importUiState.value = ImportUiState.Error(result.message)
                }
            }
        }
    }

    fun completeZipWithEntryPoint(
        context: Context,
        tempDirName: String,
        selectedEntryPoint: String,
        projectName: String,
        originalFileName: String
    ) {
        viewModelScope.launch {
            _importUiState.value = ImportUiState.Progress(0.92f, "Finalizing selected entry point: $selectedEntryPoint")

            val result = ZipImportEngine.completeWithSelectedEntryPoint(
                context = context,
                tempDirName = tempDirName,
                selectedEntryPoint = selectedEntryPoint,
                projectName = projectName,
                originalFileName = originalFileName,
                onProgress = { progress, message ->
                    _importUiState.value = ImportUiState.Progress(progress, message)
                }
            )

            when (result) {
                is ZipImportResult.Success -> {
                    val id = projectRepository.insertProject(result.project)
                    val saved = result.project.copy(id = id)
                    _importUiState.value = ImportUiState.Success(saved, emptyList())
                }

                is ZipImportResult.Error -> {
                    _importUiState.value = ImportUiState.Error(result.message)
                }

                else -> {}
            }
        }
    }

    fun importHtml(context: Context, uri: Uri, customProjectName: String? = null) {
        viewModelScope.launch {
            _importUiState.value = ImportUiState.Progress(0.10f, "Starting HTML import...")

            val result = HtmlImportEngine.importFromUri(
                context = context,
                uri = uri,
                customProjectName = customProjectName,
                onProgress = { progress, message ->
                    _importUiState.value = ImportUiState.Progress(progress, message)
                }
            )

            when (result) {
                is HtmlImportResult.Success -> {
                    val id = projectRepository.insertProject(result.project)
                    val saved = result.project.copy(id = id)
                    _importUiState.value = ImportUiState.Success(saved, result.warnings)
                }

                is HtmlImportResult.Error -> {
                    _importUiState.value = ImportUiState.Error(result.message)
                }
            }
        }
    }

    fun renameProject(projectId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            projectRepository.updateProjectName(projectId, trimmed)
        }
    }

    fun deleteProject(context: Context, project: Project) {
        viewModelScope.launch {
            ProjectStorageManager.deleteProject(context, project.projectUuid)
            projectRepository.deleteProject(project)
        }
    }

    suspend fun getProject(id: Long): Project? {
        return projectRepository.getProjectById(id)
    }

    companion object {
        fun provideFactory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MfbViewModel::class.java)) {
                        return MfbViewModel(
                            preferencesRepository = appContainer.userPreferencesRepository,
                            projectRepository = appContainer.projectRepository,
                            buildRepository = appContainer.buildRepository,
                            appContext = appContainer.appContext
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
                }
            }
    }
}
